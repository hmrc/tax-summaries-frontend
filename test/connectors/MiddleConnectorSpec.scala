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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, urlEqualTo}
import com.github.tomakehurst.wiremock.http.Fault
import config.ApplicationConfig
import models.{AtsData, AtsErrorResponse, AtsListData, AtsNotFoundResponse, AtsSuccessResponseWithPayload}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Injecting
import uk.gov.hmrc.domain.{SaUtr, Uar}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestConstants.{testNino, testUar, testUtr}
import utils.{JsonUtil, WireMockHelper}

import scala.concurrent.ExecutionContext
import scala.io.Source

class MiddleConnectorSpec
    extends UnitSpec with GuiceOneAppPerSuite with ScalaFutures with WireMockHelper with IntegrationPatience
    with JsonUtil with Injecting {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.tax-summaries.port"       -> server.port(),
        "microservice.services.tax-summaries-agent.port" -> server.port()
      )
      .build()

  implicit val hc = HeaderCarrier()
  implicit lazy val appConfig = inject[ApplicationConfig]
  implicit lazy val ec = inject[ExecutionContext]
  private val currentYear = 2018
  private val currentYearMinus1 = currentYear - 1

  def sut = new MiddleConnector(inject[HttpHandler])

  val utr = SaUtr(testUtr)

  val uar = Uar(testUar)

  val loadSAJson = loadAndParseJsonWithDummyData("/summary_json_test.json")
  val saResponse: String = loadAndReplace("/summary_json_test.json", Map("$utr" -> utr.utr))
  val expectedSAResponse = Json.fromJson[AtsData](loadSAJson).get

  val loadAtsListData = Source.fromURL(getClass.getResource("/test_list_utr.json")).mkString
  val atsListData = Json.fromJson[AtsListData](Json.parse(loadAtsListData)).get

  "connectToPayeTaxSummary" should {

    "return successful response" in {

      val expectedResponse: String = loadAndReplace("/paye_ats.json", Map("$nino" -> testNino.nino))
      val url = s"/taxs/" + testNino + "/" + currentYear + "/paye-ats-data"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(expectedResponse))
      )

      val result = sut.connectToPayeATS(testNino, currentYear).futureValue

      result.json shouldBe Json.parse(expectedResponse)
    }

    "return BadRequest response" in {

      val url = s"/taxs/" + testNino + "/" + currentYear + "/paye-ats-data"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(BAD_REQUEST)
            .withBody("Bad Request"))
      )

      a[BadRequestException] should be thrownBy await(sut.connectToPayeATS(testNino, currentYear))

    }

    "return NotFound response" in {

      val url = s"/taxs/" + testNino + "/" + currentYear + "/paye-ats-data"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(404)
            .withBody("Not Found"))
      )
      a[NotFoundException] should be thrownBy await(sut.connectToPayeATS(testNino, currentYear))

    }

    "return InternalServerError response" in {

      val url = s"/taxs/" + testNino + "/" + currentYear + "/paye-ats-data"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(500)
            .withBody("Internal Server Error"))
      )
      a[Upstream5xxResponse] should be thrownBy await(sut.connectToPayeATS(testNino, currentYear))

    }
  }

  "connectToAts" should {

    "return successful response" in {

      val url = s"/taxs/" + utr + "/" + currentYear + "/ats-data"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(saResponse))
      )

      val result = sut.connectToAts(utr, currentYear).futureValue

      result shouldBe AtsSuccessResponseWithPayload[AtsData](expectedSAResponse)
    }

    "return 4xx response" in {

      val url = s"/taxs/" + utr + "/" + currentYear + "/ats-data"
      val body = "No ATS List found"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(NOT_FOUND)
            .withBody(body))
      )

      val result = sut.connectToAts(utr, currentYear).futureValue

      result shouldBe AtsNotFoundResponse(NOT_FOUND.toString)
    }

    "return 5xx response" in {

      val url = s"/taxs/" + utr + "/" + currentYear + "/ats-data"
      val body = "Something went wrong"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
            .withBody(body))
      )

      val result = sut.connectToAts(utr, currentYear).futureValue

      result shouldBe a[AtsErrorResponse]
    }

    "return BadRequest response" in {

      val url = s"/taxs/" + utr + "/" + currentYear + "/ats-data"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(BAD_REQUEST)
            .withBody("Bad Request"))
      )

      sut.connectToAts(utr, currentYear).futureValue shouldBe a[AtsErrorResponse]
    }
  }

  "connectToAtsOnBehalfOf" should {

    "return successful response" in {

      val url = s"/taxs/" + utr + "/" + currentYear + "/ats-data"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(saResponse))
      )

      val result = sut.connectToAtsOnBehalfOf(uar, utr, currentYear).futureValue

      result shouldBe AtsSuccessResponseWithPayload[AtsData](expectedSAResponse)
    }

    "return 4xx response" in {

      val url = s"/taxs/" + utr + "/" + currentYear + "/ats-data"
      val body = "No ATS List found"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(NOT_FOUND)
            .withBody(body))
      )

      val result = sut.connectToAtsOnBehalfOf(uar, utr, currentYear).futureValue

      result shouldBe AtsNotFoundResponse(NOT_FOUND.toString)
    }

    "return 5xx response" in {

      val url = s"/taxs/" + utr + "/" + currentYear + "/ats-data"
      val body = "Something went wrong"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
            .withBody(body))
      )

      val result = sut.connectToAtsOnBehalfOf(uar, utr, currentYear).futureValue

      result shouldBe a[AtsErrorResponse]
    }
  }

  "connectToAtsList" should {

    "return successful response" in {

      val url = s"/taxs/" + utr + "/" + "ats-list"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(loadAtsListData))
      )

      val result = sut.connectToAtsList(utr).futureValue

      result shouldBe AtsSuccessResponseWithPayload[AtsListData](atsListData)
    }

    "return 4xx response" in {

      val url = s"/taxs/" + utr + "/" + "ats-list"
      val body = "No ATS List found"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(NOT_FOUND)
            .withBody(body))
      )

      val result = sut.connectToAtsList(utr).futureValue

      result shouldBe AtsNotFoundResponse(NOT_FOUND.toString)
    }

    "return 5xx response" in {

      val url = s"/taxs/" + utr + "/" + "ats-list"
      val body = "Something went wrong"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
            .withBody(body))
      )

      val result = sut.connectToAtsList(utr).futureValue

      result shouldBe a[AtsErrorResponse]
    }

    "return BadRequest response" in {

      val url = s"/taxs/" + utr + "/" + "ats-list"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(BAD_REQUEST)
            .withBody("Bad Request"))
      )

      sut.connectToAtsList(utr).futureValue shouldBe a[AtsErrorResponse]
    }
  }

  "connectToAtsListOnBehalfOf" should {

    "return successful response" in {

      val url = s"/taxs/" + utr + "/" + "ats-list"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(loadAtsListData))
      )

      val result = sut.connectToAtsListOnBehalfOf(uar, utr).futureValue

      result shouldBe AtsSuccessResponseWithPayload[AtsListData](atsListData)
    }

    "return 4xx response" in {

      val url = s"/taxs/" + utr + "/" + "ats-list"
      val body = "No ATS List found"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(NOT_FOUND)
            .withBody(body))
      )

      val result = sut.connectToAtsListOnBehalfOf(uar, utr).futureValue

      result shouldBe AtsNotFoundResponse(NOT_FOUND.toString)
    }

    "return 5xx response" in {

      val url = s"/taxs/" + utr + "/" + "ats-list"
      val body = "Something went wrong"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
            .withBody(body))
      )

      val result = sut.connectToAtsListOnBehalfOf(uar, utr).futureValue

      result shouldBe a[AtsErrorResponse]
    }
  }

  "connectToPayeATSMultipleYears" should {

    val url = s"/taxs/$testNino/$currentYearMinus1/$currentYear/paye-ats-data"

    "return successful response" in {

      val expectedResponse: String = loadAndReplace("/paye_ats_multiple_years.json", Map("$nino" -> testNino.nino))

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(expectedResponse))
      )

      val result = sut.connectToPayeATSMultipleYears(testNino, currentYearMinus1, currentYear).futureValue

      result.json shouldBe Json.parse(expectedResponse)
    }

    "return BadRequest response" in {

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(BAD_REQUEST)
            .withBody("Bad Request"))
      )

      a[BadRequestException] should be thrownBy await(
        sut.connectToPayeATSMultipleYears(testNino, currentYearMinus1, currentYear))

    }

    "return NotFound response" in {

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(NOT_FOUND)
            .withBody("Not found")
        )
      )

      intercept[NotFoundException] {
        await(sut.connectToPayeATSMultipleYears(testNino, currentYearMinus1, currentYear))
      }
    }

    "return a InternalServerError response" in {

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
            .withBody("An error occurred"))
      )
      a[Upstream5xxResponse] should be thrownBy await(
        sut.connectToPayeATSMultipleYears(testNino, currentYearMinus1, currentYear))

    }

    "return an exception when a fault with the request occurs" in {

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withFault(Fault.MALFORMED_RESPONSE_CHUNK)
        )
      )

      intercept[Exception] {
        await(sut.connectToPayeATSMultipleYears(testNino, currentYearMinus1, currentYear))
      }
    }
  }

  "connectToGovernmentSpend" should {

    val url = s"/taxs/government-spend/$currentYear/$testNino"

    "return a successful response" in {

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(OK)
            .withBody("""{"Environment" : 5.5}""")
        )
      )

      val result = sut.connectToGovernmentSpend(currentYear, testNino).futureValue
      result.status shouldBe OK
      result.json.as[Map[String, Double]] shouldBe Map("Environment" -> 5.5)
    }

    "return a BadRequest response" in {

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(BAD_REQUEST)
            .withBody("Oops")
        )
      )

      intercept[BadRequestException] {
        await(sut.connectToGovernmentSpend(currentYear, testNino))
      }
    }

    "return a InternalServerError response" in {

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
            .withBody("Oops")
        )
      )

      intercept[Upstream5xxResponse] {
        await(sut.connectToGovernmentSpend(currentYear, testNino))
      }
    }

    "return an exception when a fault with the request occurs" in {

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withFault(Fault.MALFORMED_RESPONSE_CHUNK)
        )
      )

      intercept[Exception] {
        await(sut.connectToGovernmentSpend(currentYear, testNino))
      }
    }
  }
}
