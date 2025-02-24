/*
 * Copyright 2025 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, notFound, ok, post, serverError, urlEqualTo}
import models.{ErrorView, PertaxApiResponse}
import org.scalatest.EitherValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.*
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{FakeRequest, Injecting}
import play.twirl.api.Html
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.partials.HtmlPartial
import utils.{JsonUtil, WireMockHelper}

import scala.concurrent.ExecutionContext.Implicits.global

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

  private implicit val hc: HeaderCarrier                                = HeaderCarrier()
  private implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure("microservice.services.pertax.port" -> server.port())
      .build()

  lazy val pertaxConnector: PertaxConnector = inject[PertaxConnector]

  def authoriseUrl() = s"/pertax/authorise"
  val partialUrl     = "/pertax/partials"

  "PertaxConnector.pertaxPostAuthorise" must {
    "return a PertaxApiResponse with ACCESS_GRANTED code" in {
      server.stubFor(
        post(urlEqualTo(authoriseUrl()))
          .willReturn(ok("{\"code\": \"ACCESS_GRANTED\", \"message\": \"Access granted\"}"))
      )

      val result = pertaxConnector
        .pertaxPostAuthorise()
        .value
        .futureValue
        .getOrElse(PertaxApiResponse("INCORRECT RESPONSE", "INCORRECT", None, None))
      result mustBe PertaxApiResponse("ACCESS_GRANTED", "Access granted", None, None)
    }

    "return a PertaxApiResponse with NO_HMRC_PT_ENROLMENT code with a redirect link" in {
      server.stubFor(
        post(urlEqualTo(authoriseUrl()))
          .willReturn(
            ok(
              "{\"code\": \"NO_HMRC_PT_ENROLMENT\", \"message\": \"There is no valid HMRC PT enrolment\", \"redirect\": \"/tax-enrolment-assignment-frontend/account\"}"
            )
          )
      )

      val result = pertaxConnector
        .pertaxPostAuthorise()
        .value
        .futureValue
        .getOrElse(PertaxApiResponse("INCORRECT RESPONSE", "INCORRECT", None, None))
      result mustBe PertaxApiResponse(
        "NO_HMRC_PT_ENROLMENT",
        "There is no valid HMRC PT enrolment",
        None,
        Some("/tax-enrolment-assignment-frontend/account")
      )
    }

    "return a PertaxApiResponse with INVALID_AFFINITY code and an errorView" in {
      server.stubFor(
        post(urlEqualTo(authoriseUrl()))
          .willReturn(
            ok(
              "{\"code\": \"INVALID_AFFINITY\", \"message\": \"The user is neither an individual or an organisation\", \"errorView\": {\"url\": \"/path/for/partial\", \"statusCode\": 401}}"
            )
          )
      )

      val result = pertaxConnector
        .pertaxPostAuthorise()
        .value
        .futureValue
        .getOrElse(PertaxApiResponse("INCORRECT RESPONSE", "INCORRECT", None, None))
      result mustBe PertaxApiResponse(
        "INVALID_AFFINITY",
        "The user is neither an individual or an organisation",
        Some(ErrorView("/path/for/partial", UNAUTHORIZED)),
        None
      )
    }

    "return a PertaxApiResponse with MCI_RECORD code and an errorView" in {
      server.stubFor(
        post(urlEqualTo(authoriseUrl()))
          .willReturn(
            ok(
              "{\"code\": \"MCI_RECORD\", \"message\": \"Manual correspondence indicator is set\", \"errorView\": {\"url\": \"/path/for/partial\", \"statusCode\": 423}}"
            )
          )
      )

      val result = pertaxConnector
        .pertaxPostAuthorise()
        .value
        .futureValue
        .getOrElse(PertaxApiResponse("INCORRECT RESPONSE", "INCORRECT", None, None))
      result mustBe PertaxApiResponse(
        "MCI_RECORD",
        "Manual correspondence indicator is set",
        Some(ErrorView("/path/for/partial", 423)),
        None
      )
    }

    "return a UpstreamErrorResponse with the correct error code" when {

      List(
        BAD_REQUEST,
        NOT_FOUND,
        FORBIDDEN,
        INTERNAL_SERVER_ERROR
      ).foreach { error =>
        s"an $error is returned from the backend" in {

          server.stubFor(
            post(urlEqualTo(authoriseUrl())).willReturn(
              aResponse()
                .withStatus(error)
            )
          )

          val result = pertaxConnector
            .pertaxPostAuthorise()
            .value
            .futureValue
            .swap
            .getOrElse(UpstreamErrorResponse("INCORRECT RESPONSE", IM_A_TEAPOT))
          result.statusCode mustBe error
        }
      }
    }
  }

  "PertaxConnector.loadPartial" must {

    "return a successful partial" in {
      val returnPartial: HtmlPartial = HtmlPartial.Success.apply(None, Html("test content"))
      server.stubFor(
        get(urlEqualTo(partialUrl)).willReturn(ok("test content"))
      )

      val result = pertaxConnector.loadPartial(partialUrl).futureValue
      result mustBe returnPartial
    }

    "return failed partial when a malformed URL is provided" in {

      val malformedUrl               = "/this%20is%20a%20malformed%20url"
      val returnPartial: HtmlPartial = HtmlPartial.Failure(Some(404), "Not Found")

      server.stubFor(
        get(urlEqualTo(malformedUrl)).willReturn(notFound.withBody("Not Found"))
      )

      val result = pertaxConnector.loadPartial(malformedUrl).futureValue

      result mustBe returnPartial
    }

    "return a failed partial when the request fails with 404" in {
      val returnPartial: HtmlPartial = HtmlPartial.Failure(Some(404), "Not Found")
      server.stubFor(
        get(urlEqualTo(partialUrl)).willReturn(notFound.withBody("Not Found"))
      )

      val result = pertaxConnector.loadPartial(partialUrl).futureValue
      result mustBe returnPartial
    }

    "return failed partial when the call to the service fails" in {

      val returnPartial: HtmlPartial = HtmlPartial.Failure(Some(500), "Error")
      server.stubFor(
        get(urlEqualTo(partialUrl)).willReturn(serverError.withBody("Error"))
      )

      val result = pertaxConnector.loadPartial(partialUrl).futureValue
      result mustBe returnPartial
    }
  }
}
