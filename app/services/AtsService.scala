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

import cats.data.EitherT
import com.google.inject.Inject
import config.ApplicationConfig
import connectors.MiddleConnector
import controllers.auth.requests.AuthenticatedRequest
import models.*
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND}
import repository.TaxsAgentTokenSessionCacheRepository
import uk.gov.hmrc.domain.{SaUtr, TaxIdentifier, Uar}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.DataKey
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utils.*
import view_models.{ATSUnavailableViewModel, NoATSViewModel}

import java.util.Date
import scala.annotation.nowarn
import scala.concurrent.{ExecutionContext, Future}

class AtsService @Inject() (
  middleConnector: MiddleConnector,
  taxsAgentTokenSessionCacheRepository: TaxsAgentTokenSessionCacheRepository,
  appConfig: ApplicationConfig,
  val auditService: AuditService,
  val authUtils: AuthorityUtils
)(implicit ex: ExecutionContext) {
  val accountUtils: AccountUtils = AccountUtils

  def createModel(taxYear: Int, converter: AtsData => GenericViewModel)(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]
  ): Future[GenericViewModel] =
    getAts(taxYear) map {
      checkCreateModel(taxYear, _, converter)
    }

  def createFutureModel(taxYear: Int, converter: AtsData => Future[GenericViewModel])(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]
  ): Future[GenericViewModel] =
    getAts(taxYear) flatMap {
      checkCreateFutureModel(taxYear, _, converter)
    }

  private def checkCreateModel(
    taxYear: Int,
    output: Either[Int, AtsData],
    converter: AtsData => GenericViewModel
  ): GenericViewModel =
    output match {
      case Right(atsList) if atsList.taxYear > appConfig.taxYearSA =>
        new ATSUnavailableViewModel
      case Right(atsList)                                          => converter(atsList)
      case Left(NOT_FOUND)                                         => NoATSViewModel(taxYear)
      case Left(_)                                                 => new ATSUnavailableViewModel
    }

  private def checkCreateFutureModel(
    taxYear: Int,
    output: Either[Int, AtsData],
    converter: AtsData => Future[GenericViewModel]
  ): Future[GenericViewModel] = {
    val finalOutput = output match {
      case Right(atsData) if atsData.taxYear > appConfig.taxYearSA => Future.successful(new ATSUnavailableViewModel)
      case Right(atsData)                                          => converter(atsData)
      case Left(NOT_FOUND)                                         => Future.successful(NoATSViewModel(taxYear))
      case Left(_)                                                 => Future.successful(new ATSUnavailableViewModel)
    }
    finalOutput
  }

  private def getAts(
    taxYear: Int
  )(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[Either[Int, AtsData]] =
    if (accountUtils.isAgent(request)) {
      taxsAgentTokenSessionCacheRepository.getFromSession[AgentToken](DataKey(Globals.TAXS_AGENT_TOKEN_KEY)).flatMap {
        token =>
          getAtsAndStore(taxYear, token).value
      }
    } else {
      getAtsAndStore(taxYear).value
    }

  @nowarn("msg=match may not be exhaustive")
  private def getAtsAndStore(taxYear: Int, agentToken: Option[AgentToken] = None)(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]
  ): EitherT[Future, Int, AtsData] = {
    val account      = utils.AccountUtils.getAccount(request)
    val requestedUTR = authUtils.getRequestedUtr(account, agentToken)

    // This warning is unchecked because we know that AuthorisedFor will only give us those accounts
    val gotData = (account: @unchecked) match {
      case _: Uar               => middleConnector.connectToAtsOnBehalfOf(requestedUTR, taxYear)
      case individualUtr: SaUtr => middleConnector.connectToAts(individualUtr, taxYear)
    }

    EitherT {
      gotData flatMap {
        case AtsSuccessResponseWithPayload(data: AtsData) if data.taxLiability.forall(_.isZeroOrLess) =>
          Future.successful(Left(NOT_FOUND))
        case AtsSuccessResponseWithPayload(data: AtsData) if data.errors.nonEmpty                     =>
          Future.successful(Left(INTERNAL_SERVER_ERROR))
        case AtsSuccessResponseWithPayload(data: AtsData)                                             =>
          sendAuditEvent(account, data)
          Future.successful(Right(data))
        case AtsNotFoundResponse(_)                                                                   =>
          Future.successful(Left(NOT_FOUND))
        case AtsErrorResponse(_)                                                                      =>
          Future.successful(Left(INTERNAL_SERVER_ERROR))
      }
    }
  }

  private def sendAuditEvent(account: TaxIdentifier, data: AtsData)(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]
  ): Future[AuditResult] =
    (account: @unchecked) match {
      case _: Uar   =>
        auditService.sendEvent(
          AuditTypes.Tx_SUCCEEDED,
          Map(
            "agentId"   -> AccountUtils.getAccountId(request),
            "clientUtr" -> data.utr.get,
            "taxYear"   -> data.taxYear.toString,
            "time"      -> new Date().toString
          )
        )
      case _: SaUtr =>
        val userType = if (AccountUtils.isPortalUser(request)) "non-transitioned" else "transitioned"
        auditService.sendEvent(
          AuditTypes.Tx_SUCCEEDED,
          Map(
            "userId"   -> request.userId,
            "userUtr"  -> request.saUtr.fold("")(_.utr),
            "userType" -> userType,
            "taxYear"  -> data.taxYear.toString,
            "time"     -> new Date().toString
          )
        )
    }
}
