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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, anyUrl, get}
import com.github.tomakehurst.wiremock.http.Fault
import models.{AtsErrorResponse, AtsNotFoundResponse, AtsSuccessResponseWithPayload}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK, UNAUTHORIZED}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{Json, Reads}
import play.api.test.Injecting
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import utils.WireMockHelper

import scala.concurrent.ExecutionContext

class HttpHandlerSpec
    extends WordSpec with Matchers with GuiceOneAppPerSuite with ScalaFutures with WireMockHelper
    with IntegrationPatience with Injecting {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.tax-summaries.port"       -> server.port(),
        "microservice.services.tax-summaries-agent.port" -> server.port()
      )
      .build()

  implicit val hc = HeaderCarrier()

  implicit lazy val ec = inject[ExecutionContext]

  def sut: HttpHandler = new HttpHandler(inject[DefaultHttpClient])

  case class TestClass(str: String)

  object TestClass {
    implicit val reads: Reads[TestClass] = Json.reads[TestClass]
  }

  def url = s"http://localhost:${server.port()}"
  def partialUrl = s"/foo/bar"

  "HttpHandler" when {

    "get is called" should {

      "return a AtsSuccessResponseWithPayload" when {

        "the http call returns 200" in {

          val expectedBody = "success"

          server.stubFor(
            get(anyUrl()).willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(s"""{"str": "$expectedBody"}"""))
          )

          val result = sut.get[TestClass](url).futureValue

          result shouldBe AtsSuccessResponseWithPayload(TestClass(expectedBody))
        }
      }

      "return a AtsNotFoundResponse" when {

        "the connector returns 404" in {

          val expectedBody = "not found"

          server.stubFor(
            get(anyUrl()).willReturn(
              aResponse()
                .withStatus(NOT_FOUND)
                .withBody(expectedBody))
          )

          val result = sut.get[TestClass](url).futureValue

          result shouldBe AtsNotFoundResponse(NOT_FOUND.toString)
        }
      }

      "return a AtsErrorResponse" when {

        "the connector does not return 200 or 404" in {

          server.stubFor(
            get(anyUrl()).willReturn(
              aResponse()
                .withStatus(UNAUTHORIZED)
                .withBody("Unauthorised"))
          )

          val result = sut.get[TestClass](url).futureValue

          result shouldBe an[AtsErrorResponse]
        }

        "the connector returns a 5xx response" in {

          server.stubFor(
            get(anyUrl()).willReturn(
              aResponse()
                .withStatus(INTERNAL_SERVER_ERROR)
                .withBody("Error"))
          )

          val result = sut.get[TestClass](url).futureValue

          result shouldBe an[AtsErrorResponse]
        }

        "the connector throws an exception" in {

          server.stubFor(
            get(anyUrl()).willReturn(
              aResponse()
                .withFault(Fault.EMPTY_RESPONSE)
            ))

          val result = sut.get[TestClass](url).futureValue

          result shouldBe an[AtsErrorResponse]
        }
      }
    }
  }
}
