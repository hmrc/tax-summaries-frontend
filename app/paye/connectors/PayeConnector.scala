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

package paye.connectors

import com.google.inject.Inject
import common.config.ApplicationConfig
import play.api.Logging
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.{ExecutionContext, Future}

class PayeConnector @Inject() (http: HttpClientV2)(implicit
  appConfig: ApplicationConfig,
  ec: ExecutionContext
) extends Logging {

  val serviceUrl: String = appConfig.serviceUrl

  private def url(path: String) = s"$serviceUrl$path"

  def connectToPayeATS(nino: Nino, taxYear: Int)(implicit
    hc: HeaderCarrier
  ): Future[Either[UpstreamErrorResponse, HttpResponse]] = {
    val fullUrl = url("/taxs/" + nino + "/" + taxYear + "/paye-ats-data")
    http.get(url"$fullUrl").execute[Either[UpstreamErrorResponse, HttpResponse]] recover handleHttpExceptions
  }

  def connectToPayeATSMultipleYears(nino: Nino, yearFrom: Int, yearTo: Int)(implicit
    hc: HeaderCarrier
  ): Future[Either[UpstreamErrorResponse, HttpResponse]] = {
    val fullUrl = url(s"/taxs/$nino/$yearFrom/$yearTo/paye-ats-data")
    http.get(url"$fullUrl").execute[Either[UpstreamErrorResponse, HttpResponse]] recover handleHttpExceptions
  }

  private val handleHttpExceptions: PartialFunction[Throwable, Either[UpstreamErrorResponse, HttpResponse]] = {
    case e: HttpException =>
      logger.error(e.message)
      Left(UpstreamErrorResponse(e.message, e.responseCode))
  }

}
