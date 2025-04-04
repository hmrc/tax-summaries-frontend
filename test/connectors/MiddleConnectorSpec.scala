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

import com.github.tomakehurst.wiremock.client.WireMock._
import config.ApplicationConfig
import models._
import org.scalatest.EitherValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.Injecting
import uk.gov.hmrc.domain.{SaUtr, Uar}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import utils.TestConstants.{testNino, testUar, testUtr}
import utils.{JsonUtil, WireMockHelper}

import scala.concurrent.ExecutionContext
import scala.io.{BufferedSource, Source}

class MiddleConnectorSpec
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
        "microservice.services.tax-summaries.port" -> server.port(),
        "play.ws.timeout.request"                  -> "1000ms",
        "play.ws.timeout.connection"               -> "500ms"
      )
      .build()

  implicit val hc: HeaderCarrier                 = HeaderCarrier()
  implicit lazy val appConfig: ApplicationConfig = inject[ApplicationConfig]
  implicit lazy val ec: ExecutionContext         = inject[ExecutionContext]
  private val currentYear                        = 2022
  private val currentYearMinus1                  = currentYear - 1

  val listOfErrors: List[Int] = List(400, 401, 403, 404, 409, 412, 500, 501, 502, 503, 504)

  def sut: MiddleConnector = new MiddleConnector(inject[HttpClientV2], inject[HttpHandler])

  val utr: SaUtr = SaUtr(testUtr)

  val uar: Uar = Uar(testUar)

  val loadSAJson: JsValue         = loadAndParseJsonWithDummyData("/summary_json_test_2021.json")
  val saResponse: String          = loadAndReplace("/summary_json_test_2021.json", Map("testUtr" -> utr.utr))
  val expectedSAResponse: AtsData = Json.fromJson[AtsData](loadSAJson).get

  val loadAtsListDataSource: BufferedSource = Source.fromURL(getClass.getResource("/test_list_utr.json"))
  val loadAtsListData: String               = loadAtsListDataSource.mkString
  loadAtsListDataSource.close()
  val atsListData: AtsListData              =
    Json.fromJson[AtsListData](Json.parse(loadAtsListData)).get

  "connectToPayeATS" must {

    "return successful response" in {

      val expectedResponse: String = loadAndReplace("/paye_ats_2020.json", Map("$nino" -> testNino.nino))
      val url                      = s"/taxs/" + testNino + "/" + currentYear + "/paye-ats-data"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(expectedResponse)
        )
      )

      val result = sut.connectToPayeATS(testNino, currentYear).futureValue.value

      result.json mustBe Json.parse(expectedResponse)
    }

    "return an UpstreamErrorResponse" when {
      listOfErrors.foreach { status =>
        s"a response with status $status is received" in {
          val url = s"/taxs/" + testNino + "/" + currentYear + "/paye-ats-data"

          server.stubFor(
            get(urlEqualTo(url))
              .willReturn(
                aResponse()
                  .withStatus(status)
              )
          )

          val result = sut.connectToPayeATS(testNino, currentYear).futureValue.left.value

          result.statusCode mustBe status
        }
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

      val result = sut.connectToPayeATS(testNino, currentYear).futureValue.left.value
      result.statusCode mustBe GATEWAY_TIMEOUT
    }
  }

  "connectToAts" must {

    "return successful response" in {

      val url = s"/taxs/" + utr + "/" + currentYear + "/ats-data"

      server.stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(saResponse)
          )
      )

      val result = sut.connectToAts(utr, currentYear).futureValue

      result mustBe AtsSuccessResponseWithPayload[AtsData](expectedSAResponse)
    }

    "return 4xx response" in {

      val url  = s"/taxs/" + utr + "/" + currentYear + "/ats-data"
      val body = "No ATS List found"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(NOT_FOUND)
            .withBody(body)
        )
      )

      val result = sut.connectToAts(utr, currentYear).futureValue

      result mustBe a[AtsNotFoundResponse]
    }

    "return 5xx response" in {

      val url  = s"/taxs/" + utr + "/" + currentYear + "/ats-data"
      val body = "Something went wrong"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
            .withBody(body)
        )
      )

      val result = sut.connectToAts(utr, currentYear).futureValue

      result mustBe a[AtsErrorResponse]
    }

    "return BadRequest response" in {

      val url = s"/taxs/" + utr + "/" + currentYear + "/ats-data"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(BAD_REQUEST)
            .withBody("Bad Request")
        )
      )

      sut.connectToAts(utr, currentYear).futureValue mustBe a[AtsErrorResponse]
    }
  }

  "connectToAtsOnBehalfOf" must {

    "return successful response" in {

      val url = s"/taxs/" + utr + "/" + currentYear + "/ats-data"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(saResponse)
        )
      )

      val result = sut.connectToAtsOnBehalfOf(utr, currentYear).futureValue

      result mustBe AtsSuccessResponseWithPayload[AtsData](expectedSAResponse)
    }

    "return 4xx response" in {

      val url  = s"/taxs/" + utr + "/" + currentYear + "/ats-data"
      val body = "No ATS List found"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(NOT_FOUND)
            .withBody(body)
        )
      )

      val result = sut.connectToAtsOnBehalfOf(utr, currentYear).futureValue

      result mustBe a[AtsNotFoundResponse]
    }

    "return 5xx response" in {

      val url  = s"/taxs/" + utr + "/" + currentYear + "/ats-data"
      val body = "Something went wrong"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
            .withBody(body)
        )
      )

      val result = sut.connectToAtsOnBehalfOf(utr, currentYear).futureValue

      result mustBe a[AtsErrorResponse]
    }
  }

  "connectToAtsList" must {

    "return successful response" in {

      val url = s"/taxs/" + utr + "/2022/5" + "/" + "ats-list"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(loadAtsListData)
        )
      )

      val result = sut.connectToAtsList(utr, 2022, 5).futureValue

      result mustBe AtsSuccessResponseWithPayload[AtsListData](atsListData)
    }

    "return 4xx response" in {

      val url  = s"/taxs/" + utr + "/2022/5" + "/" + "ats-list"
      val body = "No ATS List found"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(NOT_FOUND)
            .withBody(body)
        )
      )

      val result = sut.connectToAtsList(utr, 2022, 5).futureValue

      result mustBe a[AtsNotFoundResponse]
    }

    "return 5xx response" in {

      val url  = s"/taxs/" + utr + "/2022/5" + "/" + "ats-list"
      val body = "Something went wrong"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
            .withBody(body)
        )
      )

      val result = sut.connectToAtsList(utr, 2022, 5).futureValue

      result mustBe a[AtsErrorResponse]
    }

    "return BadRequest response" in {

      val url = s"/taxs/" + utr + "/2022/5" + "/" + "ats-list"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(BAD_REQUEST)
            .withBody("Bad Request")
        )
      )

      sut.connectToAtsList(utr, 2022, 5).futureValue mustBe a[AtsErrorResponse]
    }
  }

  "connectToAtsListOnBehalfOf" must {

    "return successful response" in {

      val url = s"/taxs/" + utr + "/2022/5" + "/" + "ats-list"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(loadAtsListData)
        )
      )

      val result = sut.connectToAtsListOnBehalfOf(utr, 2022, 5).futureValue

      result mustBe AtsSuccessResponseWithPayload[AtsListData](atsListData)
    }

    "return 4xx response" in {

      val url  = s"/taxs/" + utr + "/2022/5" + "/" + "ats-list"
      val body = "No ATS List found"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(NOT_FOUND)
            .withBody(body)
        )
      )

      val result = sut.connectToAtsListOnBehalfOf(utr, 2022, 5).futureValue

      result mustBe a[AtsNotFoundResponse]
    }

    "return 5xx response" in {

      val url  = s"/taxs/" + utr + "/2022/5" + "/" + "ats-list"
      val body = "Something went wrong"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
            .withBody(body)
        )
      )

      val result = sut.connectToAtsListOnBehalfOf(utr, 2022, 5).futureValue

      result mustBe a[AtsErrorResponse]
    }
  }

  "connectToPayeATSMultipleYears" must {

    val url = s"/taxs/$testNino/$currentYearMinus1/$currentYear/paye-ats-data"

    "return successful response" in {

      val expectedResponse: String = loadAndReplace("/paye_ats_multiple_years.json", Map("$nino" -> testNino.nino))

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(expectedResponse)
        )
      )

      val result = sut.connectToPayeATSMultipleYears(testNino, currentYearMinus1, currentYear).futureValue.value

      result.json mustBe Json.parse(expectedResponse)
    }

    "return an UpstreamErrorResponse" when {
      listOfErrors.foreach { status =>
        s"a response with status $status is received" in {
          server.stubFor(
            get(urlEqualTo(url))
              .willReturn(
                aResponse()
                  .withStatus(status)
              )
          )

          val result = sut.connectToPayeATSMultipleYears(testNino, currentYearMinus1, currentYear).futureValue
          result.left.value.statusCode mustBe status
        }
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

      val result = sut.connectToPayeATSMultipleYears(testNino, currentYearMinus1, currentYear).futureValue.left.value
      result.statusCode mustBe GATEWAY_TIMEOUT
    }
  }

  "connectToGovernmentSpend" must {

    val url = s"/taxs/government-spend/$currentYear"

    "return a successful response" in {

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(OK)
            .withBody("""{"Environment" : 5.5}""")
        )
      )

      val result = sut.connectToGovernmentSpend(currentYear).futureValue.value

      result.status mustBe OK
      result.json.as[Map[String, Double]] mustBe Map("Environment" -> 5.5)
    }

    "return an UpstreamErrorResponse" when {
      listOfErrors.foreach { status =>
        s"a response with status $status is received" in {
          server.stubFor(
            get(urlEqualTo(url))
              .willReturn(
                aResponse()
                  .withStatus(status)
              )
          )

          val result = sut.connectToGovernmentSpend(currentYear).futureValue

          result.left.value.statusCode mustBe status
        }
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

      val result = sut.connectToGovernmentSpend(currentYear).futureValue.left.value
      result.statusCode mustBe GATEWAY_TIMEOUT
    }
  }
}
