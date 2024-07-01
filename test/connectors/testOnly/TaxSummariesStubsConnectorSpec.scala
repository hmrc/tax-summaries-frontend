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

package connectors.testOnly

import com.github.tomakehurst.wiremock.client.WireMock._
import models.testOnly.{OdsValue, SAODSModel}
import org.scalatest.EitherValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Injecting
import uk.gov.hmrc.http.HeaderCarrier
import utils.{JsonUtil, WireMockHelper}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}

class TaxSummariesStubsConnectorSpec
    extends AnyWordSpec
    with Matchers
    with GuiceOneAppPerSuite
    with ScalaFutures
    with WireMockHelper
    with IntegrationPatience
    with JsonUtil
    with Injecting
    with EitherValues {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.tax-summaries-stubs.port" -> server.port()
      )
      .build()

  private implicit val hc: HeaderCarrier                 = HeaderCarrier()
  private implicit lazy val ec: ExecutionContext         = inject[ExecutionContext]
  private lazy val connector: TaxSummariesStubsConnector = inject[TaxSummariesStubsConnector]
  private lazy val url                                   = s"/ods-sa-data/$utr/$taxYear"

  private val taxYear = 2023
  private val utr     = "0000000010"

  private val saODSModel = SAODSModel(utr, taxYear, "0001", List(OdsValue("abc", BigDecimal(44.44))))

  "connectToCid" must {
    "return an OK response when the connector returns OK" in {
      server.stubFor(
        get(url)
          .willReturn(ok(Json.toJson(saODSModel).toString()))
      )

      val result = Await.result(connector.get(taxYear, utr), 5.seconds)
      result mustBe saODSModel
    }

    "return an OK response with country England and empty list when the connector returns NOT FOUND" in {
      server.stubFor(
        get(url)
          .willReturn(notFound())
      )

      val result = Await.result(connector.get(taxYear, utr), 5.seconds)
      result mustBe SAODSModel(utr, taxYear, "0001", Nil)
    }

    "return an error response when the connector returns neither OK nor NOT FOUND" in {
      server.stubFor(
        get(url)
          .willReturn(serverError())
      )

      a[RuntimeException] mustBe thrownBy {
        Await.result(connector.get(taxYear, utr), 5.seconds)
      }
    }
  }
}
