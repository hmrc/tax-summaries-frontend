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
import models.{ AtsListData, IncomingAtsError}
import play.api.mvc.Request
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Account, SaAccount, TaxSummariesAgentAccount}
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import utils.{AccountUtils, AuthorityUtils, AtsError, GenericViewModel}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

object AtsListService extends AtsListService {
  override lazy val middleConnector = MiddleConnector
  override lazy val dataCache = DataCacheConnector
  override lazy val cryptoService = CryptoService
  override lazy val authUtils = AuthorityUtils
  override lazy val auditService: AuditService = AuditService
  override lazy val accountUtils:AccountUtils = AccountUtils
}

trait AtsListService {

  def middleConnector: MiddleConnector
  def auditService: AuditService
  def dataCache: DataCacheConnector
  def cryptoService: CryptoService
  def authUtils: AuthorityUtils
  def accountUtils : AccountUtils

  def createModel(converter: (AtsListData => GenericViewModel))(implicit user: User, hc: HeaderCarrier, request: Request[AnyRef]): Future[GenericViewModel] = {
    getAtsYearList map {
      checkCreateModel(_, converter)
    }
  }

  private def checkCreateModel(output: AtsListData, converter: (AtsListData => GenericViewModel)): GenericViewModel = {
    output match {
      case error if error.errors.nonEmpty => error.errors.get match {
        case IncomingAtsError(_) => throw new AtsError(error.errors.toString)
      }
      case atsList => converter(atsList)
    }
  }

  def getAtsYearList(implicit user: User, hc: HeaderCarrier, request: Request[AnyRef]): Future[AtsListData] = {
    for {
      data <- dataCache.fetchAndGetAtsListForSession
    } yield {
      data match {
        case Some(data) => {
          accountUtils.isAgent(user) match {
            case true =>
              fetchAgentInfo(data)
            case false =>
              getAtsListAndStore()

          }
        }
        case _ =>
          if (accountUtils.isAgent(user)) {
            dataCache.getAgentToken.flatMap {
              token => getAtsListAndStore(token)
            }
          } else {
            getAtsListAndStore()

          }
      }
    }
  } flatMap { identity }


  private def fetchAgentInfo (data :AtsListData)(implicit user: User, hc: HeaderCarrier, request: Request[AnyRef]) : Future[AtsListData] = {
    for {
      token <- dataCache.getAgentToken
    } yield {
      if (authUtils.checkUtr(data.utr, token)) {
        Future.successful(data)
      } else {
         getAtsListAndStore(token)
      }
    }
  } flatMap(identity)

  private def getAtsListAndStore(agentToken: Option[AgentToken]=None)(implicit user: User, hc: HeaderCarrier, request: Request[AnyRef]): Future[AtsListData] = {
    val account = utils.AccountUtils.getAccount(user)
    val requestedUTR = authUtils.getRequestedUtr(account, agentToken)

    val gotData = (account: @unchecked) match {
      case agent: TaxSummariesAgentAccount => middleConnector.connectToAtsListOnBehalfOf(agent.uar, requestedUTR)
      case individual: SaAccount => middleConnector.connectToAtsList(individual.utr)
    }

    // TODO: Audit Events
    for (data <- gotData) yield {
      data.errors match {
        case None =>
          sendAuditEvent(account, data)
          storeAtsListData(data)
        case Some(IncomingAtsError("NoAtsError")) => storeAtsListData(data)
        case Some(_) => Future(data)
      }
    }
  } flatMap { identity }

  private def storeAtsListData(atsList: AtsListData)(implicit user: User, hc: HeaderCarrier): Future[AtsListData] = {
    dataCache.storeAtsListForSession(atsList) map {
      data => data.get
    }
  }

  def storeSelectedTaxYear(taxYear: Int)(implicit user: User, hc: HeaderCarrier): Future[Int] = {
    dataCache.storeAtsTaxYearForSession(taxYear: Int) map {
      data => data.get
    }
  }

  def fetchSelectedTaxYear(implicit user: User, hc: HeaderCarrier): Future[Int] = {
    dataCache.fetchAndGetAtsTaxYearForSession map {
      data => data.get
    }
  }

  private def sendAuditEvent(account: Account, data: AtsListData)(implicit user: User, hc: HeaderCarrier, request: Request[AnyRef]) = {
    (account: @unchecked) match {
      case _: TaxSummariesAgentAccount =>
        auditService.sendEvent(AuditTypes.Tx_SUCCEEDED, Map(
          "agentId" -> AccountUtils.getAccountId(user),
          "clientUtr" -> data.utr,
          "time" -> new Date().toString
        ))
      case _: SaAccount =>
        val userType = if (AccountUtils.isPortalUser(request)) "non-transitioned" else "transitioned"
        auditService.sendEvent(AuditTypes.Tx_SUCCEEDED, Map(
          "userId" -> user.user.userId,
          "userUtr" -> data.utr,
          "userType" -> userType,
          "time" -> new Date().toString
        ))
    }
  }
}
