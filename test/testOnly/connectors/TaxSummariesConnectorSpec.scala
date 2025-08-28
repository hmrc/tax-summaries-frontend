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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, urlEqualTo}
import config.ApplicationConfig
import models.*
import org.scalatest.EitherValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.*
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.Injecting
import testOnly.models.AtsSaFields
import uk.gov.hmrc.domain.{SaUtr, Uar}
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2
import utils.TestConstants.{testUar, testUtr}
import utils.{JsonUtil, TaxYearForTesting, WireMockHelper}

import scala.concurrent.ExecutionContext

class TaxSummariesConnectorSpec
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

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.tax-summaries.port" -> server.port(),
        "play.ws.timeout.request"                  -> "1000ms",
        "play.ws.timeout.connection"               -> "500ms"
      )
      .build()

  implicit val hc: HeaderCarrier                 = HeaderCarrier()
  implicit lazy val appConfig: ApplicationConfig = inject[ApplicationConfig]
  implicit lazy val ec: ExecutionContext         = inject[ExecutionContext]
  private val currentYear                        = 2022

  val listOfErrors: List[Int] = List(400, 401, 403, 404, 409, 412, 500, 501, 502, 503, 504)

  def sut: TaxSummariesConnector = new TaxSummariesConnector(inject[HttpClientV2])

  val utr: SaUtr = SaUtr(testUtr)

  val uar: Uar = Uar(testUar)

  val loadSAJson: JsValue         = Json.parse(
    loadAndReplace(
      "/json/sa-get-ats-data-previous-tax-year.json",
      Map("testUtr" -> testUtr, "<TAXYEAR>" -> currentTaxYearForTesting.toString)
    )
  )
  val saResponse: String          = loadAndReplace(
    "/json/sa-get-ats-data-previous-tax-year.json",
    Map("testUtr" -> utr.utr, "<TAXYEAR>" -> currentTaxYearForTesting.toString)
  )
  val expectedSAResponse: AtsData = Json.fromJson[AtsData](loadSAJson).get

  val atsListData: AtsListData = getSaAtsList("$utr")
  val loadAtsListData: String  = Json.stringify(Json.toJson(atsListData))

  "connectToAtsSaFields" must {
    "return successful response" in {
      val expectedResponse: String = Json.toJson(AtsSaFields(Seq("abc", "def"))).toString()
      val url                      = "/test-only/taxs/" + currentYear + "/ats-sa-fields"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(expectedResponse)
        )
      )

      val result = sut.connectToAtsSaFields(currentYear).futureValue.value

      result mustBe Seq("abc", "def")
    }

    val url = "/test-only/taxs/" + currentYear + "/ats-sa-fields"
    listOfErrors.foreach { status =>
      s"a response with status $status is received" in {
        server.stubFor(
          get(urlEqualTo(url))
            .willReturn(
              aResponse()
                .withStatus(status)
            )
        )

        val result = sut.connectToAtsSaFields(currentYear).futureValue

        result.left.value.statusCode mustBe status
      }
    }
  }

  "connectToAtsSaDataWithoutAuth" must {
    "return successful response" in {
      val expectedResponse: String = Json.toJson(Json.obj("abc" -> "def")).toString()
      val url                      = "/test-only/taxs/" + utr.utr + "/" + currentYear + "/ats-sa-data"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(expectedResponse)
        )
      )

      val result = sut.connectToAtsSaDataWithoutAuth(currentYear, utr.utr).futureValue.value

      result mustBe Json.obj("abc" -> "def")
    }

    val url = "/test-only/taxs/" + utr.utr + "/" + currentYear + "/ats-sa-data"
    listOfErrors.foreach { status =>
      s"a response with status $status is received" in {
        server.stubFor(
          get(urlEqualTo(url))
            .willReturn(
              aResponse()
                .withStatus(status)
            )
        )

        val result = sut.connectToAtsSaDataWithoutAuth(currentYear, utr.utr).futureValue

        result.left.value.statusCode mustBe status
      }
    }
  }
}
