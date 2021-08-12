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

import com.github.tomakehurst.wiremock.client.WireMock._
import config.ApplicationConfig
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.test.Injecting
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, NotFoundException, Upstream4xxResponse, Upstream5xxResponse}
import utils.{JsonUtil, WireMockHelper}

import scala.concurrent.ExecutionContext

class CitizenDetailsConnectorSpec
    extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with ScalaFutures with WireMockHelper
    with IntegrationPatience with JsonUtil with Injecting {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.citizen-details.port" -> server.port()
      )
      .build()

  implicit val hc = HeaderCarrier()
  implicit lazy val appConfig = inject[ApplicationConfig]
  implicit lazy val ec = inject[ExecutionContext]
  lazy val connector = inject[CitizenDetailsConnector]
  lazy val nino = new Generator().nextNino
  lazy val url = s"/citizen-details/nino/$nino"

  "connectToCid" must {
    "return an OK response when the CID API returns OK" in {
      server.stubFor(get(url).willReturn(ok("my cid response")))

      val result = connector.connectToCid(nino.toString()).futureValue

      result.status mustBe OK
      result.body mustBe "my cid response"
    }

    "throws a BadRequestException when the CID API returns BAD_REQUEST" in {
      server.stubFor(get(url).willReturn(badRequest()))

      val result = connector.connectToCid(nino.toString()).failed.futureValue

      result mustBe a[BadRequestException]
    }

    "throws a NotFoundException when the CID API returns NOT_FOUND" in {
      server.stubFor(get(url).willReturn(notFound()))

      val result = connector.connectToCid(nino.toString()).failed.futureValue

      result mustBe a[NotFoundException]
    }

    "throws a Upstream4xxResponse when the CID API returns LOCKED" in {
      server.stubFor(get(url).willReturn(aResponse().withStatus(LOCKED)))

      val result = connector.connectToCid(nino.toString()).failed.futureValue

      result mustBe a[Upstream4xxResponse]
    }

    "throws a Upstream5xxResponse when the CID API returns INTERNAL_SERVER_ERROR" in {
      server.stubFor(get(url).willReturn(serverError()))

      val result = connector.connectToCid(nino.toString()).failed.futureValue

      result mustBe a[Upstream5xxResponse]
    }
  }
}
