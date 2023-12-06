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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, ok, urlEqualTo}
import org.scalatest.EitherValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.{IM_A_TEAPOT, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{FakeRequest, Injecting}
import play.api.test.Helpers.{BAD_GATEWAY, BAD_REQUEST, GET, INTERNAL_SERVER_ERROR, NOT_FOUND, SERVICE_UNAVAILABLE}
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import utils.{JsonUtil, WireMockHelper}

import scala.concurrent.ExecutionContext
import scala.util.Random

class MessageFrontendConnectorSpec
    extends AnyWordSpec
    with Matchers
    with GuiceOneAppPerSuite
    with ScalaFutures
    with WireMockHelper
    with IntegrationPatience
    with JsonUtil
    with Injecting
    with EitherValues {

  override implicit lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.message-frontend.port" -> server.port()
      )
      .build()

  private lazy val testMessageFrontendConnector = inject[MessageFrontendConnector]
  implicit lazy val ec: ExecutionContext        = inject[ExecutionContext]

  "MessageFrontendConnector" must {
    "return a right HttpResponse" in {

      val messageCount = Random.between(1, 100)

      server.stubFor(
        get(urlEqualTo("/messages/count?read=No")).willReturn(ok(s"""{"count": $messageCount}"""))
      )

      val result = testMessageFrontendConnector.getUnreadMessageCount()(FakeRequest(GET, ""), ec).value.futureValue

      result mustBe a[Right[_, HttpResponse]]

      result.getOrElse(HttpResponse(IM_A_TEAPOT, "")).status mustBe OK
    }

    List(
      BAD_REQUEST,
      NOT_FOUND,
      INTERNAL_SERVER_ERROR,
      SERVICE_UNAVAILABLE,
      BAD_GATEWAY
    ).foreach { httpStatus =>
      s"return a left UpstreamErrorResponse with error code $httpStatus" in {

        server.stubFor(
          get(urlEqualTo("/messages/count?read=No")).willReturn(aResponse().withStatus(httpStatus))
        )

        val result = testMessageFrontendConnector.getUnreadMessageCount()(FakeRequest(GET, ""), ec).value.futureValue

        result mustBe a[Left[UpstreamErrorResponse, _]]

        result.swap.getOrElse(UpstreamErrorResponse.apply("ERROR", IM_A_TEAPOT)).statusCode mustBe httpStatus
      }
    }
  }

}
