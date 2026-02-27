/*
 * Copyright 2026 HM Revenue & Customs
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

package sa.connectors

import com.google.inject.Inject
import common.config.ApplicationConfig
import common.models.*
import play.api.Logging
import play.api.http.Status.*
import play.api.libs.json.{JsValue, Reads}
import sa.models.{AtsData, AtsListData}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpResponse, StringContextOps, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}

class SaConnector @Inject() (http: HttpClientV2)(implicit
  appConfig: ApplicationConfig,
  ec: ExecutionContext
) extends Logging {

  private def url(path: String) = s"${appConfig.serviceUrl}$path"

  def getDetail(utr: SaUtr, taxYear: Int)(implicit hc: HeaderCarrier): Future[AtsResponse] = {
    val fullUrl = url(s"/taxs/$utr/$taxYear/ats-data")

    http
      .get(url"$fullUrl")
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .recover(handleHttpExceptions)
      .map(handleAtsResponse[AtsData])
  }

  def getList(utr: SaUtr, endYear: Int, numberOfYears: Int)(implicit hc: HeaderCarrier): Future[AtsResponse] = {
    val fullUrl = url(s"/taxs/$utr/$endYear/$numberOfYears/ats-list")

    http
      .get(url"$fullUrl")
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .recover(handleHttpExceptions)
      .map(handleAtsResponse[AtsListData])
  }

  private val handleHttpExceptions: PartialFunction[Throwable, Either[UpstreamErrorResponse, HttpResponse]] = {
    case e: HttpException =>
      logger.error(e.message)
      Left(UpstreamErrorResponse(e.message, e.responseCode))
  }

  private def handleAtsResponse[A](implicit
    reads: Reads[A]
  ): PartialFunction[
    Either[UpstreamErrorResponse, HttpResponse],
    AtsResponse
  ] = {
    case Left(upstreamErrorResponse) =>
      upstreamErrorResponse.statusCode match {
        case NOT_FOUND =>
          logger.warn(upstreamErrorResponse.message)
          AtsNotFoundResponse(upstreamErrorResponse.message)

        case _ if upstreamErrorResponse.statusCode >= 500 =>
          logger.error(upstreamErrorResponse.message)
          AtsErrorResponse(upstreamErrorResponse.message)

        case _ =>
          logger.error(upstreamErrorResponse.message, upstreamErrorResponse)
          AtsErrorResponse(upstreamErrorResponse.message)
      }

    case Right(response) =>
      extractJson[A](response.json)
  }

  private def extractJson[A](value: JsValue)(implicit reads: Reads[A]): AtsResponse =
    value.asOpt[A] match {
      case Some(a) => AtsSuccessResponseWithPayload[A](a)
      case None    => AtsErrorResponse("Could not parse Json")
    }
}
