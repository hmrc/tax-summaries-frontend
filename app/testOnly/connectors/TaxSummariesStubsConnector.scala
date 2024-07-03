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

package testOnly.connectors

import config.ApplicationConfig
import models.testOnly.{CountryAndODSValues, SAODSModel}
import play.api.Logging
import play.api.http.Status.{CREATED, NOT_FOUND, OK}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaxSummariesStubsConnector @Inject() (
  http: HttpClientV2,
  applicationConfig: ApplicationConfig
) extends Logging {

  private val baseUrl = applicationConfig.taxSummariesStubsHost

  def save(taxYear: Int, utr: String, countryAndODSValues: CountryAndODSValues)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Unit] =
    http
      .post(url"$baseUrl/ods-sa-data/$utr/$taxYear")
      .withBody(countryAndODSValues)
      .execute[HttpResponse]
      .map { r =>
        r.status match {
          case CREATED => (): Unit
          case _       => throw new RuntimeException(s"Unexpected response: ${r.status}")
        }
      }
      .map(_ => (): Unit)

  def get(taxYear: Int, utr: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[SAODSModel] =
    http
      .get(url"$baseUrl/ods-sa-data/$utr/$taxYear")
      .execute[HttpResponse]
      .map { r =>
        r.status match {
          case OK        => r.json.as[SAODSModel]
          case NOT_FOUND => SAODSModel(utr, taxYear, "0001", Nil)
          case _         => throw new RuntimeException(s"Unexpected response: ${r.status}")
        }
      }
}
