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

package sa.connectors

import com.google.inject.Inject
import common.config.ApplicationConfig
import common.connectors.HttpHandler
import common.models.*
import play.api.Logging
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.*

import scala.concurrent.Future

class SaConnector @Inject() (httpHandler: HttpHandler)(implicit
  appConfig: ApplicationConfig
) extends Logging {

  val serviceUrl: String = appConfig.serviceUrl

  private def url(path: String) = s"$serviceUrl$path"

  def connectToAts(UTR: SaUtr, taxYear: Int)(implicit hc: HeaderCarrier): Future[AtsResponse] =
    httpHandler.get[AtsData](url("/taxs/" + UTR + "/" + taxYear + "/ats-data"))

  def connectToAtsOnBehalfOf(requestedUTR: SaUtr, taxYear: Int)(implicit
    hc: HeaderCarrier
  ): Future[AtsResponse] =
    connectToAts(requestedUTR, taxYear)

  def connectToAtsList(
    UTR: SaUtr,
    endYear: Int,
    numberOfYears: Int
  )(implicit hc: HeaderCarrier): Future[AtsResponse] =
    httpHandler.get[AtsListData](url("/taxs/" + UTR + "/" + endYear + "/" + numberOfYears + "/ats-list"))

  def connectToAtsListOnBehalfOf(requestedUTR: SaUtr, endYear: Int, numberOfYears: Int)(implicit
    hc: HeaderCarrier
  ): Future[AtsResponse] =
    connectToAtsList(requestedUTR, endYear, numberOfYears)

  val handleHttpExceptions: PartialFunction[Throwable, Either[UpstreamErrorResponse, HttpResponse]] = {
    case e: HttpException =>
      logger.error(e.message)
      Left(UpstreamErrorResponse(e.message, e.responseCode))
  }
}
