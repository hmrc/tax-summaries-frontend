/*
 * Copyright 2021 HM Revenue & Customs
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
import models.PayeAtsData
import play.api.Logger
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.Reads
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utils.AuditTypes

import scala.concurrent.{ExecutionContext, Future}

class PayeAtsService @Inject()(middleConnector: MiddleConnector, auditService: AuditService)(
  implicit ec: ExecutionContext) {

  def getPayeATSData(nino: Nino, taxYear: Int)(
    implicit hc: HeaderCarrier,
    request: PayeAuthenticatedRequest[_]): Future[Either[HttpResponse, PayeAtsData]] =
    middleConnector.connectToPayeATS(nino, taxYear) map { response =>
      handleConnectorResponse[PayeAtsData](response, nino, taxYear)
    } recover {
      case e: BadRequestException => Left(HttpResponse(BAD_REQUEST, e.getMessage))
      case e: NotFoundException   => Left(HttpResponse(NOT_FOUND, e.getMessage))
      case e: Exception =>
        Logger.error(s"Exception in PayeAtsService: $e", e)
        Left(HttpResponse(INTERNAL_SERVER_ERROR, e.getMessage))
    }

  def getPayeATSMultipleYearData(nino: Nino, yearFrom: Int, yearTo: Int)(
    implicit hc: HeaderCarrier,
    request: PayeAuthenticatedRequest[_]): Future[Either[HttpResponse, List[PayeAtsData]]] =
    middleConnector.connectToPayeATSMultipleYears(nino, yearFrom, yearTo) map { response =>
      handleConnectorResponse[List[PayeAtsData]](response, nino, yearFrom)
    } recover {
      case e: BadRequestException => Left(HttpResponse(BAD_REQUEST, e.getMessage))
      case e: NotFoundException   => Left(HttpResponse(NOT_FOUND, e.getMessage))
      case e: Exception =>
        Logger.error(s"Exception in PayeAtsService: $e", e)
        Left(HttpResponse(INTERNAL_SERVER_ERROR, e.getMessage))
    }

  def getPayeTaxYearData(nino: Nino, yearFrom: Int, yearTo: Int)(
    implicit hc: HeaderCarrier,
    request: AuthenticatedRequest[_]): Future[Either[HttpResponse, List[Int]]] =
    middleConnector.connectToPayeATSMultipleYears(nino, yearFrom, yearTo) map { response =>
      handlePayeTaxYearDataResponse[List[PayeAtsData]](response, nino, yearFrom)
    } recover {
      case e: BadRequestException => Left(HttpResponse(BAD_REQUEST, e.getMessage))
      case e: NotFoundException   => Right(List.empty)
      case e: Exception =>
        Logger.error(s"Exception in PayeAtsService: $e", e)
        Left(HttpResponse(INTERNAL_SERVER_ERROR, e.getMessage))
    }

  private def handlePayeTaxYearDataResponse[A](response: HttpResponse, nino: Nino, taxYear: Int)(
    implicit reads: Reads[List[PayeAtsData]],
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]): Either[HttpResponse, List[Int]] =
    response.status match {
      case OK =>
        sendAuditEventForPayeTaxYearData(nino, taxYear)
        val res = response.json.as[List[PayeAtsData]]
        Right(res.map(_.taxYear).reverse)
      case NOT_FOUND => Right(List.empty)
      case _ =>
        Left(response)
    }

  private def sendAuditEventForPayeTaxYearData(nino: Nino, taxYear: Int)(
    implicit hc: HeaderCarrier,
    request: AuthenticatedRequest[_]): Future[AuditResult] =
    auditService.sendEvent(
      auditType = AuditTypes.Tx_SUCCEEDED,
      details = Map(
        "userNino" -> nino.nino,
        "taxYear"  -> taxYear.toString
      )
    )

  private def handleConnectorResponse[A](response: HttpResponse, nino: Nino, taxYear: Int)(
    implicit reads: Reads[A],
    hc: HeaderCarrier,
    request: PayeAuthenticatedRequest[_]): Either[HttpResponse, A] =
    response.status match {
      case OK =>
        sendAuditEvent(nino, taxYear)
        Right(response.json.as[A])
      case _ =>
        Left(response)
    }

  private def sendAuditEvent(nino: Nino, taxYear: Int)(
    implicit hc: HeaderCarrier,
    request: PayeAuthenticatedRequest[_]): Future[AuditResult] =
    auditService.sendEvent(
      auditType = AuditTypes.Tx_SUCCEEDED,
      details = Map(
        "userNino" -> nino.nino,
        "taxYear"  -> taxYear.toString
      )
    )
}
