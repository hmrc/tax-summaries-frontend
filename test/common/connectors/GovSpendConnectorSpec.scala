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

package common.connectors

import com.github.tomakehurst.wiremock.client.WireMock.*
import common.config.ApplicationConfig
import common.models.*
import common.utils.TestConstants.{testUar, testUtr}
import common.utils.{JsonUtil, TaxYearForTesting, WireMockHelper}
import org.scalatest.EitherValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.*
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Injecting
import uk.gov.hmrc.domain.{SaUtr, Uar}
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.ExecutionContext

class GovSpendConnectorSpec
    extends AnyWordSpec
    with Matchers
    with GuiceOneAppPerSuite
    with ScalaFutures
    with WireMockHelper
    with IntegrationPatience
    with JsonUtil
    with Injecting
    with EitherValues
    with TaxYearForTesting {

  override implicit lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.tax-summaries.port" -> server.port(),
        "play.ws.timeout.request"                  -> "1000ms",
        "play.ws.timeout.connection"               -> "500ms"
      )
      .build()

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val listOfErrors: List[Int] = List(400, 401, 403, 404, 409, 412, 500, 501, 502, 503, 504)

  def sut: GovSpendConnector = new GovSpendConnector(inject[HttpClientV2])

  val utr: SaUtr = SaUtr(testUtr)

  val uar: Uar                                   = Uar(testUar)
  implicit lazy val appConfig: ApplicationConfig = inject[ApplicationConfig]
  implicit lazy val ec: ExecutionContext         = inject[ExecutionContext]

  val saResponse: String = atsData(currentTaxYearSA)

  val expectedSAResponse: AtsData = Json.fromJson[AtsData](Json.parse(saResponse)).get

  val atsListData: AtsListData = atsList("$utr")
  val loadAtsListData: String  = Json.stringify(Json.toJson(atsListData))

  "connectToGovernmentSpend" must {

    val url = s"/taxs/government-spend/$currentTaxYearSA"

    "return a successful response" in {

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(OK)
            .withBody("""{"Environment" : 5.5}""")
        )
      )

      val result = sut.connectToGovernmentSpend(currentTaxYearSA).futureValue.value

      result.status mustBe OK
      result.json.as[Map[String, Double]] mustBe Map("Environment" -> 5.5)
    }

    "return an UpstreamErrorResponse" when
      listOfErrors.foreach { status =>
        s"a response with status $status is received" in {
          server.stubFor(
            get(urlEqualTo(url))
              .willReturn(
                aResponse()
                  .withStatus(status)
              )
          )

          val result = sut.connectToGovernmentSpend(currentTaxYearSA).futureValue

          result.left.value.statusCode mustBe status
        }
      }

    "the connector times out" in {
      server.stubFor(
        get(anyUrl()).willReturn(
          aResponse()
            .withStatus(OK)
            .withBody("""{"Environment" : 5.5}""")
            .withFixedDelay(2000)
        )
      )

      val result = sut.connectToGovernmentSpend(currentTaxYearSA).futureValue.left.value
      result.statusCode mustBe GATEWAY_TIMEOUT
    }
  }
}
