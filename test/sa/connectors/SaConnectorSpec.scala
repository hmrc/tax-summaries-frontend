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

package sa.connectors

import com.github.tomakehurst.wiremock.client.WireMock.*
import common.config.ApplicationConfig
import common.connectors.HttpHandler
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

import scala.concurrent.ExecutionContext

class SaConnectorSpec
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

  def sut: SaConnector = new SaConnector(inject[HttpHandler])

  val utr: SaUtr = SaUtr(testUtr)

  val uar: Uar                                   = Uar(testUar)
  implicit lazy val appConfig: ApplicationConfig = inject[ApplicationConfig]
  implicit lazy val ec: ExecutionContext         = inject[ExecutionContext]

  val saResponse: String = atsData(currentTaxYearSA)

  val expectedSAResponse: AtsData = Json.fromJson[AtsData](Json.parse(saResponse)).get

  val atsListData: AtsListData = atsList("$utr")
  val loadAtsListData: String  = Json.stringify(Json.toJson(atsListData))

  "connectToAts" must {

    "return successful response" in {

      val url = s"/taxs/" + utr + "/" + currentTaxYearSA + "/ats-data"

      server.stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(saResponse)
          )
      )

      val result = sut.connectToAts(utr, currentTaxYearSA).futureValue

      result mustBe AtsSuccessResponseWithPayload[AtsData](expectedSAResponse)
    }

    "return 4xx response" in {

      val url  = s"/taxs/" + utr + "/" + currentTaxYearSA + "/ats-data"
      val body = "No ATS List found"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(NOT_FOUND)
            .withBody(body)
        )
      )

      val result = sut.connectToAts(utr, currentTaxYearSA).futureValue

      result mustBe a[AtsNotFoundResponse]
    }

    "return 5xx response" in {

      val url  = s"/taxs/" + utr + "/" + currentTaxYearSA + "/ats-data"
      val body = "Something went wrong"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
            .withBody(body)
        )
      )

      val result = sut.connectToAts(utr, currentTaxYearSA).futureValue

      result mustBe a[AtsErrorResponse]
    }

    "return BadRequest response" in {

      val url = s"/taxs/" + utr + "/" + currentTaxYearSA + "/ats-data"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(BAD_REQUEST)
            .withBody("Bad Request")
        )
      )

      sut.connectToAts(utr, currentTaxYearSA).futureValue mustBe a[AtsErrorResponse]
    }
  }

  "connectToAtsOnBehalfOf" must {

    "return successful response" in {

      val url = s"/taxs/" + utr + "/" + currentTaxYearSA + "/ats-data"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(saResponse)
        )
      )

      val result = sut.connectToAtsOnBehalfOf(utr, currentTaxYearSA).futureValue

      result mustBe AtsSuccessResponseWithPayload[AtsData](expectedSAResponse)
    }

    "return 4xx response" in {

      val url  = s"/taxs/" + utr + "/" + currentTaxYearSA + "/ats-data"
      val body = "No ATS List found"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(NOT_FOUND)
            .withBody(body)
        )
      )

      val result = sut.connectToAtsOnBehalfOf(utr, currentTaxYearSA).futureValue

      result mustBe a[AtsNotFoundResponse]
    }

    "return 5xx response" in {

      val url  = s"/taxs/" + utr + "/" + currentTaxYearSA + "/ats-data"
      val body = "Something went wrong"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
            .withBody(body)
        )
      )

      val result = sut.connectToAtsOnBehalfOf(utr, currentTaxYearSA).futureValue

      result mustBe a[AtsErrorResponse]
    }
  }

  "connectToAtsList" must {

    "return successful response" in {

      val url = s"/taxs/" + utr + s"/$currentTaxYearSA/5" + "/" + "ats-list"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(loadAtsListData)
        )
      )

      val result = sut.connectToAtsList(utr, currentTaxYearSA, 5).futureValue

      result mustBe AtsSuccessResponseWithPayload[AtsListData](atsListData)
    }

    "return 4xx response" in {

      val url  = s"/taxs/" + utr + s"/$currentTaxYearSA/5" + "/" + "ats-list"
      val body = "No ATS List found"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(NOT_FOUND)
            .withBody(body)
        )
      )

      val result = sut.connectToAtsList(utr, currentTaxYearSA, 5).futureValue

      result mustBe a[AtsNotFoundResponse]
    }

    "return 5xx response" in {

      val url  = s"/taxs/" + utr + s"/$currentTaxYearSA/5" + "/" + "ats-list"
      val body = "Something went wrong"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
            .withBody(body)
        )
      )

      val result = sut.connectToAtsList(utr, currentTaxYearSA, 5).futureValue

      result mustBe a[AtsErrorResponse]
    }

    "return BadRequest response" in {

      val url = s"/taxs/" + utr + s"/$currentTaxYearSA/5" + "/" + "ats-list"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(BAD_REQUEST)
            .withBody("Bad Request")
        )
      )

      sut.connectToAtsList(utr, currentTaxYearSA, 5).futureValue mustBe a[AtsErrorResponse]
    }
  }

  "connectToAtsListOnBehalfOf" must {

    "return successful response" in {

      val url = s"/taxs/" + utr + s"/$currentTaxYearSA/5" + "/" + "ats-list"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(loadAtsListData)
        )
      )

      val result = sut.connectToAtsListOnBehalfOf(utr, currentTaxYearSA, 5).futureValue

      result mustBe AtsSuccessResponseWithPayload[AtsListData](atsListData)
    }

    "return 4xx response" in {

      val url  = s"/taxs/" + utr + s"/$currentTaxYearSA/5" + "/" + "ats-list"
      val body = "No ATS List found"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(NOT_FOUND)
            .withBody(body)
        )
      )

      val result = sut.connectToAtsListOnBehalfOf(utr, currentTaxYearSA, 5).futureValue

      result mustBe a[AtsNotFoundResponse]
    }

    "return 5xx response" in {

      val url  = s"/taxs/" + utr + s"/$currentTaxYearSA/5" + "/" + "ats-list"
      val body = "Something went wrong"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
            .withBody(body)
        )
      )

      val result = sut.connectToAtsListOnBehalfOf(utr, currentTaxYearSA, 5).futureValue

      result mustBe a[AtsErrorResponse]
    }
  }

}
