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

package controllers

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, ok, urlEqualTo}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.{FileHelper, IntegrationSpec}

class IncomeBeforeTaxPayeItSpec extends IntegrationSpec {

  override def fakeApplication() = GuiceApplicationBuilder().configure(
    "microservice.services.auth.port" -> server.port(),
    "microservice.services.tax-summaries.port" -> server.port()
  ).build()

  "/paye/income-before-tax" must {

    lazy val url = "/annual-tax-summary/paye/income-before-tax/2021"

    lazy val backendUrl = s"/taxs/$generatedNino/2021/paye-ats-data"

    "return an OK response" in {

      server.stubFor(
        get(urlEqualTo(backendUrl))
          .willReturn(ok(FileHelper.loadFile("./it/resources/atsData.json")))
      )

      val request = FakeRequest(GET, url)

      val result = route(fakeApplication(), request)

      result.map(status) mustBe Some(OK)
    }

    "return an SEE_OTHER when the call to backend returns a NOT_FOUND response" in {

      server.stubFor(
        get(urlEqualTo(backendUrl))
          .willReturn(aResponse().withStatus(NOT_FOUND))
      )

      val request = FakeRequest(GET, url)

      val result = route(fakeApplication(), request)

      result.map(status) mustBe Some(SEE_OTHER)
    }

    List(
      BAD_REQUEST,
      IM_A_TEAPOT,
      INTERNAL_SERVER_ERROR,
      SERVICE_UNAVAILABLE
    ).foreach { httpResponse =>
      s"return an SEE_OTHER when the call to backend to retrieve paye-ats-data throws a $httpResponse" in {

        server.stubFor(
          get(urlEqualTo(backendUrl))
            .willReturn(aResponse().withStatus(httpResponse))
        )

        val request = FakeRequest(GET, url)

        val result = route(fakeApplication(), request)

        result.map(status) mustBe Some(INTERNAL_SERVER_ERROR)
      }
    }
  }
}
