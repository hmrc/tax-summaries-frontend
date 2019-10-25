/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services

import java.util.Date

import connectors.{DataCacheConnector, MiddleConnector}
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Request
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Account, SaAccount, TaxSummariesAgentAccount}
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import utils.{AccountUtils, AtsError, AuthorityUtils, GenericViewModel}
import view_models.NoATSViewModel

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

object AtsService extends AtsService {
  override val middleConnector = MiddleConnector
  override val dataCache = DataCacheConnector
  override val auditService = AuditService
  override val authUtils = AuthorityUtils
  override val accountUtils = AccountUtils
}

trait AtsService {
  def middleConnector: MiddleConnector
  def dataCache: DataCacheConnector
  def auditService: AuditService
  val authUtils: AuthorityUtils
  val accountUtils: AccountUtils

  def createModel(taxYear: Int, converter: AtsData => GenericViewModel)(
    implicit user: User,
    hc: HeaderCarrier,
    request: Request[AnyRef]): Future[GenericViewModel] =
    getAts(taxYear) map {
      checkCreateModel(_, converter)
    }

  def checkCreateModel(output: AtsData, converter: AtsData => GenericViewModel): GenericViewModel =
    output match {
      case errors if errors.errors.nonEmpty =>
        errors.errors.get match {
          case IncomingAtsError("NoAtsError") => new NoATSViewModel
          case IncomingAtsError(_)            => throw new AtsError(errors.errors.get.toString)
        }
      case wrapper => converter(wrapper)
    }

  def getAts(taxYear: Int)(implicit user: User, hc: HeaderCarrier, request: Request[AnyRef]): Future[AtsData] = {
    for {
      data <- dataCache.fetchAndGetAtsForSession(taxYear)
    } yield {
      data match {
        case Some(data) => {
          accountUtils.isAgent(user) match {
            case true =>
              fetchAgentInfo(data, taxYear)
            case false =>
              getAtsAndStore(taxYear)
          }
        }
        case _ =>
          if (accountUtils.isAgent(user)) {
            dataCache.getAgentToken.flatMap { token =>
              getAtsAndStore(taxYear, token)
            }
          } else {
            getAtsAndStore(taxYear)
          }
      }
    }
  } flatMap { identity }

  private def fetchAgentInfo(
    data: AtsData,
    taxYear: Int)(implicit user: User, hc: HeaderCarrier, request: Request[AnyRef]): Future[AtsData] = {
    for {
      token <- dataCache.getAgentToken
    } yield {
      if (authUtils.checkUtr(data.utr, token)) {
        Future.successful(data)
      } else {
        getAtsAndStore(taxYear, token)
      }
    }
  } flatMap (identity)

  private def getAtsAndStore(taxYear: Int, agentToken: Option[AgentToken] = None)(
    implicit user: User,
    hc: HeaderCarrier,
    request: Request[AnyRef]): Future[AtsData] = {
    val account = utils.AccountUtils.getAccount(user)
    val requestedUTR = authUtils.getRequestedUtr(account, agentToken)

    //This warning is unchecked because we know that AuthorisedFor will only give us those accounts
    val gotData = (account: @unchecked) match {
      case agent: TaxSummariesAgentAccount => middleConnector.connectToAtsOnBehalfOf(agent.uar, requestedUTR, taxYear)
      case individual: SaAccount           => middleConnector.connectToAts(individual.utr, taxYear)
    }

    for (data <- gotData) yield {
      data.errors match {
        case None =>
          sendAuditEvent(account, data)
          storeAtsData(data)
        case Some(IncomingAtsError("NoAtsError")) =>
          storeAtsData(data)
        case Some(_) =>
          Future.successful(data)
      }
    }
  } flatMap { identity }

  private def storeAtsData(dataWithUser: AtsData)(implicit user: User, hc: HeaderCarrier) =
    dataCache.storeAtsForSession(dataWithUser) map { data =>
      data.get
    }

  private def sendAuditEvent(
    account: Account,
    data: AtsData)(implicit user: User, hc: HeaderCarrier, request: Request[AnyRef]) =
    (account: @unchecked) match {
      case _: TaxSummariesAgentAccount =>
        auditService.sendEvent(
          AuditTypes.Tx_SUCCEEDED,
          Map(
            "agentId"   -> AccountUtils.getAccountId(user),
            "clientUtr" -> data.utr.get,
            "taxYear"   -> data.taxYear.toString,
            "time"      -> new Date().toString
          )
        )
      case _: SaAccount =>
        val userType = if (AccountUtils.isPortalUser(request)) "non-transitioned" else "transitioned"
        auditService.sendEvent(
          AuditTypes.Tx_SUCCEEDED,
          Map(
            "userId"   -> user.user.userId,
            "userUtr"  -> data.utr.get,
            "userType" -> userType,
            "taxYear"  -> data.taxYear.toString,
            "time"     -> new Date().toString
          )
        )
    }
}
