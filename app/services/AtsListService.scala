/*
 * Copyright 2020 HM Revenue & Customs
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

import com.google.inject.Inject
import connectors.{DataCacheConnector, MiddleConnector}
import controllers.auth.AuthenticatedRequest
import models._
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND}
import uk.gov.hmrc.domain.{SaUtr, TaxIdentifier, Uar}
import uk.gov.hmrc.http.HeaderCarrier
import utils._
import view_models.{ATSUnavailableViewModel, NoATSViewModel}

import scala.concurrent.{ExecutionContext, Future}

class AtsListService @Inject()(
  auditService: AuditService,
  middleConnector: MiddleConnector,
  dataCache: DataCacheConnector,
  authUtils: AuthorityUtils)(implicit ec: ExecutionContext) {

  def accountUtils: AccountUtils = AccountUtils

  def createModel(converter: (AtsListData => GenericViewModel))(
    implicit hc: HeaderCarrier,
    request: AuthenticatedRequest[_]): Future[GenericViewModel] =
    getAtsYearList map {
      checkCreateModel(_, converter)
    }

  private def checkCreateModel(
    output: Either[Int, AtsListData],
    converter: AtsListData => GenericViewModel): GenericViewModel =
    output match {
      case Right(atsList)  => converter(atsList)
      case Left(NOT_FOUND) => new NoATSViewModel
      case Left(_)         => new ATSUnavailableViewModel
    }

  def getAtsYearList(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[Either[Int, AtsListData]] = {
    for {
      data <- dataCache.fetchAndGetAtsListForSession
    } yield {
      data match {
        case Some(data) =>
          if (accountUtils.isAgent(request)) {
            fetchAgentInfo(data)
          } else {
            getAtsListAndStore()
          }
        case _ =>
          if (accountUtils.isAgent(request)) {
            dataCache.getAgentToken.flatMap { token =>
              getAtsListAndStore(token)
            }
          } else {
            getAtsListAndStore()
          }
      }
    }
  } flatMap { identity }

  private def fetchAgentInfo(data: AtsListData)(
    implicit hc: HeaderCarrier,
    request: AuthenticatedRequest[_]): Future[Either[Int, AtsListData]] = {
    for {
      token <- dataCache.getAgentToken
    } yield {
      if (authUtils.checkUtr(data.utr, token)) {
        Future.successful(Right(data))
      } else {
        getAtsListAndStore(token)
      }
    }
  } flatMap (identity)

  private def getAtsListAndStore(agentToken: Option[AgentToken] = None)(
    implicit hc: HeaderCarrier,
    request: AuthenticatedRequest[_]): Future[Either[Int, AtsListData]] = {
    val account = utils.AccountUtils.getAccount(request)
    val requestedUTR = authUtils.getRequestedUtr(account, agentToken)

    val gotData = (account: @unchecked) match {
      case agent: Uar        => middleConnector.connectToAtsListOnBehalfOf(agent, requestedUTR)
      case individual: SaUtr => middleConnector.connectToAtsList(individual)
    }

    // TODO: Audit Events
    gotData flatMap {
      case AtsSuccessResponseWithPayload(payload: AtsListData) =>
        for {
          _    <- sendAuditEvent(account, payload)
          data <- storeAtsListData(payload)
        } yield {
          Right(data)
        }
      case AtsNotFoundResponse(_) => Future.successful(Left(NOT_FOUND))
      case AtsErrorResponse(_)    => Future.successful(Left(INTERNAL_SERVER_ERROR))
    }
  }

  private def storeAtsListData(atsList: AtsListData)(implicit hc: HeaderCarrier): Future[AtsListData] =
    dataCache.storeAtsListForSession(atsList) map { data =>
      data.get
    }

  def storeSelectedTaxYear(taxYear: Int)(implicit hc: HeaderCarrier): Future[Int] =
    dataCache.storeAtsTaxYearForSession(taxYear: Int) map { data =>
      data.get
    }

  def fetchSelectedTaxYear(implicit hc: HeaderCarrier): Future[Int] =
    dataCache.fetchAndGetAtsTaxYearForSession map { data =>
      data.get
    }

  private def sendAuditEvent(account: TaxIdentifier, data: AtsListData)(
    implicit hc: HeaderCarrier,
    request: AuthenticatedRequest[_]) =
    (account: @unchecked) match {
      case _: Uar =>
        auditService.sendEvent(
          AuditTypes.Tx_SUCCEEDED,
          Map(
            "agentId"   -> AccountUtils.getAccountId(request),
            "clientUtr" -> data.utr,
            "time"      -> new Date().toString
          ))
      case _: SaUtr =>
        val userType = if (AccountUtils.isPortalUser(request)) "non-transitioned" else "transitioned"
        auditService.sendEvent(
          AuditTypes.Tx_SUCCEEDED,
          Map(
            "userId"   -> request.userId,
            "userUtr"  -> data.utr,
            "userType" -> userType,
            "time"     -> new Date().toString
          ))
    }
}
