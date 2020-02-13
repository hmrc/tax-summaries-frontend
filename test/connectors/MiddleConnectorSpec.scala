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
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, ok, urlEqualTo}
import config.WSHttp
import models._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.test.UnitSpec
import utils.{TestConstants, WireMockHelper}

class MiddleConnectorSpec extends UnitSpec with GuiceOneAppPerSuite with WireMockHelper with MockitoSugar {


  lazy val nino = TestConstants.testNino
  lazy val connector = new MiddleConnector {
    override def http: HttpGet = WSHttp
    override def serviceUrl: String = s"http://localhost:${server.port}"
  }


  implicit val hc = HeaderCarrier()


  val taxYear = 2019

  val expectedAts = AtsData(
    taxYear = 2019,
    utr = None,
    nino = Some("AW843651A"),
    income_tax = None,
    summary_data = None,
    income_data = None,
    allowance_data = None,
    capital_gains_data = None,
    gov_spending = None,
    taxPayerData = None,
    errors = None
  )

  lazy val url =  s"/taxs/$nino/$taxYear/paye-ats-data"

  "retrieve PAYE ATS" should {
    "return ATS Response" in {

      server.stubFor(
        get(urlEqualTo(url))
         .willReturn(ok(Json.toJson[AtsData](expectedAts).toString()))
      )

        val result = await(connector.connectToPayeAts(Nino(nino), taxYear))

      result shouldBe expectedAts
    }
    "Throw NotFoundException on receiving 404 Not Found" in {
      server.stubFor(
        get(urlEqualTo(url))
          .willReturn(aResponse().withStatus(404)))
      assertThrows[NotFoundException] {
         val result = await(connector.connectToPayeAts(Nino(nino), taxYear))
       }
    }
    "Throw Upstream5xxResponse on receiving 500 Internal Server Error" in {
      server.stubFor(
        get(urlEqualTo(url))
          .willReturn(aResponse().withStatus(500)))
      assertThrows[Upstream5xxResponse] {
        val result = await(connector.connectToPayeAts(Nino(nino), taxYear))
      }
    }
  }
}
