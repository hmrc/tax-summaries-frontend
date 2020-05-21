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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, urlEqualTo}
import config.WSHttp
import connectors.deskpro.HmrcDeskproConnector
import controllers.auth.AuthenticatedRequest
import models.TicketId
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Application, Configuration}
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import utils.WireMockHelper

class HmrcDeskproConnectorSpec extends UnitSpec with GuiceOneAppPerSuite with WireMockHelper with ScalaFutures with IntegrationPatience {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.hmrc-deskpro.port" -> server.port()
      )
      .build()

  val request = AuthenticatedRequest("1", None, None, None, None, None, None, FakeRequest())
  implicit val hc = HeaderCarrier()

  def sut = new HmrcDeskproConnector(app.injector.instanceOf[WSHttp], app.injector.instanceOf[Configuration])

  "HmrcDeskproConnector" when {

    "createTicket is called" should {

      val url = "/deskpro/ticket"

      "return a ticket ID if a ticket is successfully created" in {

        val expectedResponse = "123"

        server.stubFor(
          post(urlEqualTo(url)).willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(s"""{"ticket_id": $expectedResponse}"""))
        )

        val result = sut.createTicket("John", "Doe", "a subject", "test", "me", false, request)

        result.futureValue shouldBe Some(TicketId(expectedResponse.toInt))
      }

      "thrown an Exception" when {

        "hmrc-deskpro returns a non-200 code" in {

          server.stubFor(
            post(urlEqualTo(url)).willReturn(
              aResponse()
                .withStatus(BAD_REQUEST)
            ))

          val result = sut.createTicket("John", "Doe", "a subject", "test", "me", false, request)

          a [Exception] shouldBe thrownBy {
            result.futureValue
          }
        }
      }
    }

    "createFeedback is called" should {

      val url = "/deskpro/feedback"

      "return a ticket ID if feedback is successfully created" in {

        val expectedResponse = "123"

        server.stubFor(
          post(urlEqualTo(url)).willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(s"""{"ticket_id": $expectedResponse}"""))
        )

        val result = sut.createFeedback("John", "email@email.com", "5", "a subject", "test", "me", false, request)

        result.futureValue shouldBe Some(TicketId(expectedResponse.toInt))
      }

      "thrown an Exception" when {

        "hmrc-deskpro returns a non-200 code" in {

          server.stubFor(
            post(urlEqualTo(url)).willReturn(
              aResponse()
                .withStatus(INTERNAL_SERVER_ERROR)
            ))

          val result = sut.createFeedback("John", "email@email.com", "5", "a subject", "test", "me", false, request)

          a [Exception] shouldBe thrownBy {
            result.futureValue
          }
        }
      }
    }
  }
}
