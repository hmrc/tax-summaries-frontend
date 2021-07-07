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

package connectors

import config.ApplicationConfig
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CitizenDetailsConnector @Inject()(httpClient: HttpClient, applicationConfig: ApplicationConfig)(
  implicit ec: ExecutionContext) {

  private val baseUrl = applicationConfig.cidHost

  def connectToCid(nino: Nino)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    httpClient.GET[HttpResponse](s"$baseUrl/citizen-details/${nino.withoutSuffix}/designatory-details")
}
