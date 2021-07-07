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

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.ok
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Generator

class CitizenDetailsConnectorSpec extends ConnectorSpec {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.citizen-details.port" -> server.port()
      )
      .build()

  lazy val connector = inject[CitizenDetailsConnector]
  val nino = new Generator().nextNino
  val url = s"/citizen-details/${nino.withoutSuffix}/designatory-details"

  "connectToCid" should {
    "return an OK response when the CID API returns OK" in {
      server.stubFor(WireMock.get(url).willReturn(ok("my cid response")))

      val result = connector.connectToCid(nino).futureValue

      result.status shouldBe OK
      result.body shouldBe "my cid response"
    }
  }
}
