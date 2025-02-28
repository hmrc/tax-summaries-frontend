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

package testOnly.connectors

import com.google.inject.Inject
import config.ApplicationConfig
import play.api.Logging
import play.api.http.Status.OK
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2

import scala.annotation.nowarn
import scala.concurrent.{ExecutionContext, Future}

class TaxSummariesConnector @Inject() (http: HttpClientV2)(implicit
  appConfig: ApplicationConfig,
  ec: ExecutionContext
) extends Logging {

  val serviceUrl: String = appConfig.serviceUrl

  private def url(path: String) = s"$serviceUrl$path"

  @nowarn("msg=match may not be exhaustive")
  def connectToAtsSaFields(
    taxYear: Int
  )(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, Seq[String]]] = {
    val fullUrl = url("/test-only/taxs/" + taxYear + "/ats-sa-fields")
    (http.get(url"$fullUrl").execute[Either[UpstreamErrorResponse, HttpResponse]] recover handleHttpExceptions).map {
      case Right(response) if response.status == OK => Right((response.json \ "items").as[Seq[String]])
      case Left(response)                           => Left(response)
    }
  }

  @nowarn("msg=match may not be exhaustive")
  def connectToAtsSaDataWithoutAuth(taxYear: Int, utr: String)(implicit
    hc: HeaderCarrier
  ): Future[Either[UpstreamErrorResponse, JsValue]] = {
    val fullUrl = url("/test-only/taxs/" + utr + "/" + taxYear + "/ats-sa-data")
    (http.get(url"$fullUrl").execute[Either[UpstreamErrorResponse, HttpResponse]] recover handleHttpExceptions).map {
      case Right(response) if response.status == OK => Right(response.json)
      case Left(response)                           => Left(response)
    }
  }

  private val handleHttpExceptions: PartialFunction[Throwable, Either[UpstreamErrorResponse, HttpResponse]] = {
    case e: HttpException =>
      logger.error(e.message)
      Left(UpstreamErrorResponse(e.message, e.responseCode))
  }

}
