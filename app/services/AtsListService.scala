/*
 * Copyright 2023 HM Revenue & Customs
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
import models._
import repository.TaxsAgentTokenSessionCacheRepository
import uk.gov.hmrc.domain.{SaUtr, TaxIdentifier, Uar}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.DataKey
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utils._
import view_models.AtsList

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
            atsList.taxPayer.get.taxpayer_name.get("forename"),
            atsList.taxPayer.get.taxpayer_name.get("surname"),
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
    val account      = getAccount(request)
    val requestedUTR = authUtils.getRequestedUtr(account, agentToken)

    val response = (account: @unchecked) match {
      case _: Uar            =>
        middleConnector.connectToAtsListOnBehalfOf(
          requestedUTR,
          appConfig.taxYear,
          appConfig.maxTaxYearsTobeDisplayed
        )
      case individual: SaUtr =>
        middleConnector.connectToAtsList(individual, appConfig.taxYear, appConfig.maxTaxYearsTobeDisplayed)
    }

    val result = response flatMap {
      case AtsSuccessResponseWithPayload(payload: AtsListData) =>
        val atsListData = if (appConfig.taxYear < 2020 && payload.atsYearList.isDefined) {
          AtsListData(payload.utr, payload.taxPayer, Some(payload.atsYearList.get.filter(_ < 2020)))
        } else {
          payload
        }
        Future.successful(Right(atsListData))
      case r                                                   => Future.successful(Left(r))
    }

    result map { res =>
      sendAuditEvent(account, res)
      res
    }
  }

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
