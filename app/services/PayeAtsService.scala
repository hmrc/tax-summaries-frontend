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
import connectors.MiddleConnector
import controllers.auth.{AuthenticatedRequest, PayeAuthenticatedRequest}
import models.{PayeAtsData, _}
import play.api.Logging
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utils.AuditTypes

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class PayeAtsService @Inject() (middleConnector: MiddleConnector, auditService: AuditService)(implicit
  ec: ExecutionContext
) extends Logging {

  def getPayeATSData(nino: Nino, taxYear: Int)(implicit
    hc: HeaderCarrier,
    request: PayeAuthenticatedRequest[_]
  ): Future[Either[AtsResponse, PayeAtsData]] =
    for {
      response <- middleConnector.connectToPayeATS(nino, taxYear)
    } yield response match {
      case Right(atsData)              =>
        Try(atsData.json.as[PayeAtsData]) match {
          case Success(result) =>
            sendAuditEvent(nino, taxYear, true)
            Right(result)
          case Failure(e)      =>
            sendAuditEvent(nino, taxYear, false)
            throw e
        }
      case Left(upstreamErrorResponse) =>
        val errorMessage = upstreamErrorResponse.message
        upstreamErrorResponse.statusCode match {
          case BAD_REQUEST => Left(AtsBadRequestResponse(errorMessage))
          case NOT_FOUND   => Left(AtsNotFoundResponse(errorMessage))
          case _           =>
            logger.error(s"Exception in PayeAtsService: $errorMessage")
            Left(AtsErrorResponse(errorMessage))
        }
    }

  def getPayeTaxYearData(nino: Nino, yearFrom: Int, yearTo: Int)(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]
  ): Future[Either[AtsResponse, List[Int]]] =
    for {
      response <- middleConnector.connectToPayeATSMultipleYears(nino, yearFrom, yearTo)
    } yield response match {
      case Right(atsData)              =>
        val res = atsData.json.as[List[PayeAtsData]]
        Right(res.map(_.taxYear).reverse)
      case Left(upstreamErrorResponse) =>
        val errorMessage = upstreamErrorResponse.message
        upstreamErrorResponse.statusCode match {
          case NOT_FOUND   => Right(List.empty)
          case BAD_REQUEST => Left(AtsBadRequestResponse(errorMessage))
          case _           =>
            logger.error(s"Exception in PayeAtsService: $errorMessage")
            Left(AtsErrorResponse(errorMessage))
        }
    }

  private def sendAuditEvent(nino: Nino, taxYear: Int, isSuccess: Boolean)(implicit
    hc: HeaderCarrier,
    request: PayeAuthenticatedRequest[_]
  ): Future[AuditResult] =
    auditService.sendEvent(
      auditType = if (isSuccess) AuditTypes.Tx_SUCCEEDED else AuditTypes.Tx_FAILED,
      details = Map(
        "userNino" -> nino.nino,
        "taxYear"  -> taxYear.toString
      )
    )
}
