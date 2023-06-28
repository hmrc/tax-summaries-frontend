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

package connectors

import com.google.inject.Inject
import config.ApplicationConfig
import models.{AtsData, AtsListData, AtsResponse}
import play.api.Logging
import uk.gov.hmrc.domain.{Nino, SaUtr, Uar}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

class MiddleConnector @Inject() (http: HttpClient, httpHandler: HttpHandler)(implicit
  appConfig: ApplicationConfig,
  ec: ExecutionContext
) extends Logging {

  val serviceUrl: String = appConfig.serviceUrl

  private def url(path: String) = s"$serviceUrl$path"

  def connectToAts(UTR: SaUtr, taxYear: Int)(implicit hc: HeaderCarrier): Future[AtsResponse] =
    httpHandler.get[AtsData](url("/taxs/" + UTR + "/" + taxYear + "/ats-data"))

  def connectToAtsOnBehalfOf(uar: Uar, requestedUTR: SaUtr, taxYear: Int)(implicit
    hc: HeaderCarrier
  ): Future[AtsResponse] =
    connectToAts(requestedUTR, taxYear)

  def connectToAtsList(UTR: SaUtr, endYear: Int, numberOfYears: Int)(implicit hc: HeaderCarrier): Future[AtsResponse] =
    httpHandler.get[AtsListData](url("/taxs/" + UTR + "/" + endYear + "/" + numberOfYears + "/ats-list"))

  def connectToAtsListOnBehalfOf(uar: Uar, requestedUTR: SaUtr, endYear: Int, numberOfYears: Int)(implicit
    hc: HeaderCarrier
  ): Future[AtsResponse] =
    connectToAtsList(requestedUTR, endYear, numberOfYears)

  def connectToPayeATS(nino: Nino, taxYear: Int)(implicit
    hc: HeaderCarrier
  ): Future[Either[UpstreamErrorResponse, HttpResponse]] =
    http.GET[Either[UpstreamErrorResponse, HttpResponse]](
      url("/taxs/" + nino + "/" + taxYear + "/paye-ats-data")
    ) recover handleHttpExceptions

  def connectToPayeATSMultipleYears(nino: Nino, yearFrom: Int, yearTo: Int)(implicit
    hc: HeaderCarrier
  ): Future[Either[UpstreamErrorResponse, HttpResponse]] =
    http.GET[Either[UpstreamErrorResponse, HttpResponse]](
      url(s"/taxs/$nino/$yearFrom/$yearTo/paye-ats-data")
    ) recover handleHttpExceptions

  def connectToGovernmentSpend(
    taxYear: Int
  )(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, HttpResponse]] =
    http.GET[Either[UpstreamErrorResponse, HttpResponse]](
      url(s"/taxs/government-spend/$taxYear")
    ) recover handleHttpExceptions

  val handleHttpExceptions: PartialFunction[Throwable, Either[UpstreamErrorResponse, HttpResponse]] = {
    case e: HttpException =>
      logger.error(e.message)
      Left(UpstreamErrorResponse(e.message, e.responseCode))
  }
}
