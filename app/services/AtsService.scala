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

import cats.data.EitherT
import com.google.inject.Inject
import config.ApplicationConfig
import connectors.{DataCacheConnector, MiddleConnector}
import controllers.auth.AuthenticatedRequest
import models._
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND}
import uk.gov.hmrc.domain.{SaUtr, TaxIdentifier, Uar}
import uk.gov.hmrc.http.HeaderCarrier
import utils._
import view_models.{ATSUnavailableViewModel, NoATSViewModel}

import java.util.Date
import scala.concurrent.{ExecutionContext, Future}

class AtsService @Inject() (
  middleConnector: MiddleConnector,
  dataCacheConnector: DataCacheConnector,
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
      checkCreateModel(_, converter)
    }

  def checkCreateModel(output: Either[Int, AtsData], converter: AtsData => GenericViewModel): GenericViewModel =
    output match {
      case Right(atsList) if atsList.taxYear > appConfig.taxYear =>
        new ATSUnavailableViewModel
      case Right(atsList)                                        => converter(atsList)
      case Left(NOT_FOUND)                                       => new NoATSViewModel
      case Left(_)                                               => new ATSUnavailableViewModel
    }

  def getAts(taxYear: Int)(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[Either[Int, AtsData]] =
    dataCacheConnector.fetchAndGetAtsForSession(taxYear) flatMap {
      case Some(data) =>
        if (accountUtils.isAgent(request)) {
          fetchAgentInfo(data, taxYear).value
        } else {
          getAtsAndStore(taxYear).value
        }
      case None       =>
        if (accountUtils.isAgent(request)) {
          dataCacheConnector.getAgentToken.flatMap { token =>
            getAtsAndStore(taxYear, token).value
          }
        } else {
          getAtsAndStore(taxYear).value
        }
    }

  private def fetchAgentInfo(data: AtsData, taxYear: Int)(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]
  ): EitherT[Future, Int, AtsData] =
    EitherT {
      dataCacheConnector.getAgentToken.flatMap { token =>
        if (authUtils.checkUtr(data.utr, token)) {
          Future.successful(Right(data))
        } else {
          getAtsAndStore(taxYear, token).value
        }
      }
    }

  private def getAtsAndStore(taxYear: Int, agentToken: Option[AgentToken] = None)(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]
  ): EitherT[Future, Int, AtsData] = {
    val account      = utils.AccountUtils.getAccount(request)
    val requestedUTR = authUtils.getRequestedUtr(account, agentToken)

    //This warning is unchecked because we know that AuthorisedFor will only give us those accounts
    val gotData = (account: @unchecked) match {
      case agentUar: Uar        => middleConnector.connectToAtsOnBehalfOf(agentUar, requestedUTR, taxYear)
      case individualUtr: SaUtr => middleConnector.connectToAts(individualUtr, taxYear)
    }

    EitherT {
      gotData flatMap {
        case AtsSuccessResponseWithPayload(data: AtsData) if hasNoAts(data)       => Future.successful(Left(NOT_FOUND))
        case AtsSuccessResponseWithPayload(data: AtsData) if data.errors.nonEmpty =>
          Future.successful(Left(INTERNAL_SERVER_ERROR))
        case AtsSuccessResponseWithPayload(data: AtsData)                         =>
          for {
            result <- storeAtsData(data) map (Right(_))
            _      <- sendAuditEvent(account, data)
          } yield result
        case AtsNotFoundResponse(_)                                               => Future.successful(Left(NOT_FOUND))
        case AtsErrorResponse(_)                                                  => Future.successful(Left(INTERNAL_SERVER_ERROR))
      }
    }
  }

  private def hasNoAts(data: AtsData): Boolean = data.errors.fold(false) { errors =>
    errors.error == "NoAtsError"
  }

  private def storeAtsData(dataWithUser: AtsData)(implicit hc: HeaderCarrier) =
    dataCacheConnector.storeAtsForSession(dataWithUser) map { data =>
      data.get
    }

  private def sendAuditEvent(account: TaxIdentifier, data: AtsData)(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]
  ) =
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
