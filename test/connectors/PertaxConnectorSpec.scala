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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get}
import config.ApplicationConfig
import models.PertaxApiResponse
import org.scalatest.EitherValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Injecting
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import utils.{JsonUtil, WireMockHelper}

import scala.concurrent.ExecutionContext

class PertaxConnectorSpec
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
        "microservice.services.pertax.port"    -> server.port(),
        "microservice.services.pertax.version" -> "1.0"
      )
      .build()

  implicit val hc: HeaderCarrier                 = HeaderCarrier()
  implicit lazy val appConfig: ApplicationConfig = inject[ApplicationConfig]
  implicit lazy val ec: ExecutionContext         = inject[ExecutionContext]
  lazy val connector: PertaxConnector            = inject[PertaxConnector]
  lazy val nino: Nino                            = new Generator().nextNino
  lazy val url                                   = s"/pertax/$nino/check-single-account"

  "pertaxAuth" must {
    "return a PertaxApiResponse with an ACCESS_GRANTED code" in {
      server.stubFor(
        get(url).willReturn(
          aResponse().withStatus(OK).withBody("{\"code\": \"ACCESS_GRANTED\", \"message\": \"Access granted\"}")
        )
      )

      val result = connector.pertaxAuth(nino.toString()).value.futureValue.getOrElse(PertaxApiResponse("", ""))

      result mustBe PertaxApiResponse("ACCESS_GRANTED", "Access granted")
    }
    "return a PertaxApiResponse with a NO_HMRC_PT_ENROLMENT code" in {
      server.stubFor(
        get(url).willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(
              "{\"code\": \"NO_HMRC_PT_ENROLMENT\", \"message\": \"There is no valid HMRC PT enrolment\", \"redirect\": \"/tax-enrolment-assignment-frontend/account\"}"
            )
        )
      )

      val result = connector.pertaxAuth(nino.toString()).value.futureValue.getOrElse(PertaxApiResponse("", ""))

      result mustBe PertaxApiResponse(
        "NO_HMRC_PT_ENROLMENT",
        "There is no valid HMRC PT enrolment",
        None,
        Some("/tax-enrolment-assignment-frontend/account")
      )
    }

    "return a PertaxApiResponse with no code" in {
      server.stubFor(get(url).willReturn(aResponse().withStatus(OK).withBody("{\"code\": \"\", \"message\": \"\"}")))

      val result = connector
        .pertaxAuth(nino.toString())
        .value
        .futureValue
        .getOrElse(PertaxApiResponse("NO_HMRC_PT_ENROLMENT", "There is no valid HMRC PT enrolment"))

      result mustBe PertaxApiResponse("", "", None)
    }

    List(
      BAD_REQUEST,
      NOT_FOUND,
      IM_A_TEAPOT,
      REQUEST_TIMEOUT,
      UNPROCESSABLE_ENTITY,
      INTERNAL_SERVER_ERROR,
      BAD_GATEWAY,
      SERVICE_UNAVAILABLE
    ).foreach { errorResponse =>
      s"return an UpstreamErrorResponse on Left with $errorResponse response" in {
        server.stubFor(get(url).willReturn(aResponse().withStatus(errorResponse)))

        val result = connector
          .pertaxAuth(nino.toString())
          .value
          .futureValue
          .swap
          .getOrElse(UpstreamErrorResponse("", OK))
          .statusCode

        result mustBe errorResponse
      }
    }
  }
}
