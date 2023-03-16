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
import config.ApplicationConfig
import models.PertaxApiResponse
import play.api.Logging
import play.api.http.HeaderNames
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PertaxConnector @Inject() (
  httpClient: HttpClient,
  httpClientResponse: HttpClientResponse,
  applicationConfig: ApplicationConfig
)(implicit
  ec: ExecutionContext
) extends Logging {

  private val baseUrl = applicationConfig.pertaxHost

  def pertaxAuth(nino: String)(implicit hc: HeaderCarrier): EitherT[Future, UpstreamErrorResponse, PertaxApiResponse] =
    httpClientResponse
      .read(
        httpClient.GET[Either[UpstreamErrorResponse, HttpResponse]](
          s"$baseUrl/pertax/$nino/check-single-account",
          headers = Seq((HeaderNames.ACCEPT, s"application/vnd.hmrc.${applicationConfig.pertaxApiVersion}+json"))
        )
      )
      .map(response => response.json.as[PertaxApiResponse])
}
