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

import cats.data.EitherT
import com.google.inject.Inject
import config.ApplicationConfig
import play.api.Logging
import play.api.mvc.Request
import uk.gov.hmrc.http.HttpReadsInstances._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.partials.HeaderCarrierForPartialsConverter

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class MessageFrontendConnector @Inject() (
  httpClient: HttpClientV2,
  headerCarrierForPartialsConverter: HeaderCarrierForPartialsConverter,
  httpClientResponse: HttpClientResponse,
  appConfig: ApplicationConfig
) extends Logging {

  private lazy val messageFrontendUrl: String = appConfig.messagesFrontendUrl

  def getUnreadMessageCount()(implicit
    request: Request[_],
    ec: ExecutionContext
  ): EitherT[Future, UpstreamErrorResponse, HttpResponse] = {
    val url = messageFrontendUrl + "/messages/count?read=No"

    implicit val hc = headerCarrierForPartialsConverter.fromRequestWithEncryptedCookie(request)

    httpClientResponse
      .read(
        httpClient
          .get(url"$url")
          .transform(_.withRequestTimeout(appConfig.messagesFrontendTimeoutInSec.seconds))
          .execute[Either[UpstreamErrorResponse, HttpResponse]]
      )
  }

}
