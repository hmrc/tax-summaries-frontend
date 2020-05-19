/*
 * Copyright 2020 HM Revenue & Customs
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
import config.WSHttp
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.OK
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestConstants.testNino
import utils.{JsonUtil, WireMockHelper}

class MiddleConnectorSpec extends UnitSpec with GuiceOneAppPerSuite with ScalaFutures with WireMockHelper with IntegrationPatience with JsonUtil {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.tax-summaries.port" -> server.port()
      )
      .build()

  implicit val hc = HeaderCarrier()
  private val currentYear = 2018

  lazy val sut = new MiddleConnector

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
            .withStatus(400)
            .withBody("Bad Request"))
      )

      a [BadRequestException] should be thrownBy await(sut.connectToPayeATS(testNino, currentYear))

    }

    "return NotFound response" in {

      val url = s"/taxs/" + testNino + "/" + currentYear + "/paye-ats-data"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(404)
            .withBody("Not Found"))
      )
      a [NotFoundException] should be thrownBy await(sut.connectToPayeATS(testNino, currentYear))

    }

    "return InternalServerError response" in {

      val url = s"/taxs/" + testNino + "/" + currentYear + "/paye-ats-data"

      server.stubFor(
        get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(500)
            .withBody("Internal Server Error"))
      )
      a [Upstream5xxResponse] should be thrownBy await(sut.connectToPayeATS(testNino, currentYear))

    }
  }
}
