/*
 * Copyright 2024 HM Revenue & Customs
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

package views

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{get, ok, urlEqualTo, urlMatching}
import play.api
import play.api.Application
import play.api.http.Status.OK
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsString, defaultAwaitTimeout, route, writeableOf_AnyContentAsEmpty, status => getStatus}
import repository.TaxsAgentTokenSessionCacheRepository
import testUtils.{FileHelper, IntegrationSpec}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.sca.models.{MenuItemConfig, PtaMinMenuConfig, WrapperDataResponse}
import uk.gov.hmrc.scalatestaccessibilitylinter.AccessibilityMatchers
import uk.gov.hmrc.scalatestaccessibilitylinter.domain.OutputFormat

import java.util.UUID
import scala.concurrent.Future
import scala.util.Random

class a11yTestSpec extends IntegrationSpec with AccessibilityMatchers {

  lazy val backendUrl   = s"/taxs/$generatedSaUtr/$fakeTaxYear/ats-data"
  lazy val backendUrlSa = s"/taxs/$generatedSaUtr/ats-list"

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .configure(
      "microservice.services.auth.port"                               -> server.port(),
      "microservice.services.tax-summaries.port"                      -> server.port(),
      "microservice.services.cachable.session-cache.port"             -> server.port(),
      "microservice.services.pertax.port"                             -> server.port(),
      "sca-wrapper.services.single-customer-account-wrapper-data.url" -> s"http://localhost:${server.port()}"
    )
    .overrides(
      api.inject.bind[TaxsAgentTokenSessionCacheRepository].toInstance(mockTaxsAgentTokenSessionCacheRepository),
      api.inject.bind[FeatureFlagService].toInstance(mockFeatureFlagService)
    )
    .build()

  val messageCount: Int = Random.between(1, 100)

  val wrapperDataResponse: String = Json
    .toJson(
      WrapperDataResponse(
        Seq(
          MenuItemConfig("id", "NewLayout Item", "link", leftAligned = false, 0, None, None)
        ),
        PtaMinMenuConfig("MenuName", "BackName"),
        List.empty
      )
    )
    .toString

  override def beforeEach(): Unit = {
    super.beforeEach()
    server.stubFor(
      WireMock
        .get(urlMatching("/single-customer-account-wrapper-data/wrapper-data.*"))
        .willReturn(ok(wrapperDataResponse))
    )

    server.stubFor(
      WireMock
        .get(urlMatching("/single-customer-account-wrapper-data/message-data.*"))
        .willReturn(ok(s"$messageCount"))
    )
  }

  def request(url: String): FakeRequest[AnyContentAsEmpty.type] = {
    val uuid = UUID.randomUUID().toString
    FakeRequest(GET, url).withSession(SessionKeys.sessionId -> uuid, SessionKeys.authToken -> "Bearer 1")

  }

  "annual-tax-summary" must {
    List(
      "/annual-tax-summary/",
      "/annual-tax-summary/paye/main"
    ).foreach { url =>
      s"pass accessibility validation at url $url" in {
        server.stubFor(
          get(urlEqualTo(backendUrlSa))
            .willReturn(ok(FileHelper.loadFile("./it/resources/atsList.json")))
        )
        server.stubFor(
          get(urlEqualTo(s"/pertax/$generatedNino/authorise"))
            .willReturn(ok("{\"code\": \"ACCESS_GRANTED\", \"message\": \"Access granted\"}"))
        )

        val result: Future[Result] = route(app, request(url)).get
        getStatus(result) mustBe OK
        contentAsString(result) must passAccessibilityChecks(OutputFormat.Verbose)
      }
    }
  }

  "annual-tax-summary data pages" must {
    List(
      s"/annual-tax-summary/main?taxYear=$fakeTaxYear",
      s"/annual-tax-summary/summary?taxYear=$fakeTaxYear",
      s"/annual-tax-summary/income-tax-national-insurance-contributions?taxYear=$fakeTaxYear",
      s"/annual-tax-summary/treasury-spending?taxYear=$fakeTaxYear",
      s"/annual-tax-summary/income-before-tax?taxYear=$fakeTaxYear",
      s"/annual-tax-summary/tax-free-amount?taxYear=$fakeTaxYear",
      s"/annual-tax-summary/capital-gains-tax?taxYear=$fakeTaxYear"
    ).foreach { url =>
      s"pass accessibility validation at url $url" in {
        server.stubFor(
          get(urlEqualTo(backendUrlSa))
            .willReturn(ok(FileHelper.loadFile("./it/resources/atsList.json")))
        )

        server.stubFor(
          get(urlEqualTo(backendUrl))
            .willReturn(ok(FileHelper.loadFile(s"./it/resources/atsData_$fakeTaxYear.json")))
        )

        server.stubFor(
          get(urlEqualTo(s"/pertax/$generatedNino/authorise"))
            .willReturn(ok("{\"code\": \"ACCESS_GRANTED\", \"message\": \"Access granted\"}"))
        )

        val result: Future[Result] = route(app, request(url)).get
        getStatus(result) mustBe OK
        contentAsString(result) must passAccessibilityChecks(OutputFormat.Verbose)
      }
    }
  }
}
