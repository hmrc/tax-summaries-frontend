/*
 * Copyright 2025 HM Revenue & Customs
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

import com.google.inject.Inject
import config.ApplicationConfig
import connectors.MiddleConnector
import controllers.auth.requests.AuthenticatedRequest
import models.*
import repository.TaxsAgentTokenSessionCacheRepository
import uk.gov.hmrc.domain.{SaUtr, TaxIdentifier, Uar}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.DataKey
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utils.*
import view_models.AtsList

import scala.annotation.nowarn
import scala.concurrent.{ExecutionContext, Future}

class AtsListService @Inject() (
  auditService: AuditService,
  middleConnector: MiddleConnector,
  taxsAgentTokenSessionCacheRepository: TaxsAgentTokenSessionCacheRepository,
  authUtils: AuthorityUtils,
  appConfig: ApplicationConfig
)(implicit ec: ExecutionContext)
    extends AccountUtils {

  def createModel()(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]
  ): Future[Either[AtsResponse, AtsList]] =
    getAtsYearList map {
      case Right(atsList)               =>
        Right(
          AtsList(
            atsList.utr,
            atsList.taxPayer.fold("")(_.getOrElse("forename", "")),
            atsList.taxPayer.fold("")(_.getOrElse("surname", "")),
            atsList.atsYearList.get
          )
        )
      case Left(_: AtsNotFoundResponse) => Right(AtsList.empty)
      case Left(status)                 => Left(status)
    }

  def getAtsYearList(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]
  ): Future[Either[AtsResponse, AtsListData]] = {
    if (isAgent(request)) {
      taxsAgentTokenSessionCacheRepository.getFromSession[AgentToken](DataKey(Globals.TAXS_AGENT_TOKEN_KEY)).flatMap {
        token =>
          getAtsList(token)
      }
    } else {
      getAtsList()
    }
  } map identity

  private def getAtsList(
    agentToken: Option[AgentToken] = None
  )(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[Either[AtsResponse, AtsListData]] = {
    val account  = getAccount(request)
    val response = Future {
      authUtils.getRequestedUtr(account, agentToken)
    } flatMap { requestedUTR =>
      (account: @unchecked) match {
        case _: Uar            =>
          middleConnector.connectToAtsListOnBehalfOf(
            requestedUTR,
            appConfig.taxYearSA,
            appConfig.maxTaxYearsTobeDisplayed
          )
        case individual: SaUtr =>
          middleConnector.connectToAtsList(individual, appConfig.taxYearSA, appConfig.maxTaxYearsTobeDisplayed)
      }
    }

    val result = response flatMap {
      case AtsSuccessResponseWithPayload(payload: AtsListData) => Future.successful(Right(payload))
      case r                                                   => Future.successful(Left(r))
    }

    result map { res =>
      sendAuditEvent(account, res)
      res
    }
  }

  @nowarn("msg=match may not be exhaustive")
  private def sendAuditEvent(account: TaxIdentifier, dataOpt: Either[AtsResponse, AtsListData])(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]
  ): Future[AuditResult] =
    (dataOpt, account: @unchecked) match {
      case (Right(data), _: Uar)   =>
        auditService.sendEvent(
          AuditTypes.Tx_SUCCEEDED,
          Map(
            "agentId"   -> AccountUtils.getAccountId(request),
            "clientUtr" -> data.utr
          )
        )
      case (Right(data), _: SaUtr) =>
        val userType = if (AccountUtils.isPortalUser(request)) "non-transitioned" else "transitioned"
        auditService.sendEvent(
          AuditTypes.Tx_SUCCEEDED,
          Map(
            "userId"   -> request.userId,
            "userUtr"  -> data.utr,
            "userType" -> userType
          )
        )
      case (Left(_), identifier)   =>
        auditService.sendEvent(
          AuditTypes.Tx_FAILED,
          Map(
            "userId"         -> request.userId,
            "userIdentifier" -> identifier.value
          )
        )
    }
}
