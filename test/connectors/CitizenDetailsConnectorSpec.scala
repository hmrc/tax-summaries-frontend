/*
 * Copyright 2022 HM Revenue & Customs
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
import org.scalatest.EitherValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.test.Injecting
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.HeaderCarrier
import utils.{JsonUtil, WireMockHelper}

import scala.concurrent.ExecutionContext

class CitizenDetailsConnectorSpec
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
        "microservice.services.citizen-details.port" -> server.port()
      )
      .build()

  implicit val hc             = HeaderCarrier()
  implicit lazy val appConfig = inject[ApplicationConfig]
  implicit lazy val ec        = inject[ExecutionContext]
  lazy val connector          = inject[CitizenDetailsConnector]
  lazy val nino               = new Generator().nextNino
  lazy val url                = s"/citizen-details/nino/$nino"

  "connectToCid" must {
    "return an OK response when the CID API returns OK" in {
      server.stubFor(get(url).willReturn(ok("my cid response")))

      val result = connector.connectToCid(nino.toString()).futureValue.right.get

      result.status mustBe OK
      result.body mustBe "my cid response"
    }

    "return an UpstreamErrorResponse" when {
      List(400, 401, 403, 404, 409, 412, 500, 501, 502, 503, 504).foreach { status =>
        s"a response with status $status is received" in {
          server.stubFor(
            get(urlEqualTo(url))
              .willReturn(
                aResponse()
                  .withStatus(status)
              )
          )

          val result = connector.connectToCid(nino.toString()).futureValue.left.value
          result.statusCode mustBe status
        }
      }
    }
  }
}
