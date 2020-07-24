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

import com.google.inject.Inject
import connectors.MiddleConnector
import controllers.auth.PayeAuthenticatedRequest
import models.PayeAtsData
import play.api.Logger
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utils.AuditTypes

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PayeAtsService @Inject()(middleConnector: MiddleConnector, auditService : AuditService) {

  def getPayeATSData(nino: Nino, taxYear: Int)(implicit hc: HeaderCarrier, request: PayeAuthenticatedRequest[_]):Future[Either[HttpResponse,PayeAtsData]] = {
     middleConnector.connectToPayeATS(nino,taxYear) map { response =>
       response status match {
         case OK =>
           sendAuditEvent(nino, taxYear)
           Right(response.json.as[PayeAtsData])
         case _ => Left(response)
       }
     } recover {
       case _: BadRequestException => Left(HttpResponse(BAD_REQUEST))
       case _: NotFoundException   => Left(HttpResponse(NOT_FOUND))
       case e: Exception =>
         Logger.error(s"Exception in PayeAtsService: $e", e)
         Left(HttpResponse(INTERNAL_SERVER_ERROR))
     }
  }

  private def sendAuditEvent(nino: Nino, taxYear: Int)(implicit hc: HeaderCarrier, request: PayeAuthenticatedRequest[_]): Future[AuditResult] = {
    auditService.sendEvent(
      auditType = AuditTypes.Tx_SUCCEEDED,
      details = Map(
        "userNino" -> nino.nino,
        "taxYear" -> taxYear.toString
      )
    )
  }
}
