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

package controllers

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, ok, urlEqualTo}
import models.admin.{PAYEServiceToggle, SelfAssessmentServiceToggle}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import play.api
import play.api.Application
import play.api.cache.AsyncCacheApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.PertaxAuthService
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlag
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import utils.{FileHelper, IntegrationSpec, TaxYearForTesting}

import scala.concurrent.Future

class IncomeBeforeTaxItSpec extends IntegrationSpec with TaxYearForTesting {
  private val mockPertaxAuthService           = mock[PertaxAuthService]
  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      "microservice.services.auth.port"                   -> server.port(),
      "microservice.services.tax-summaries.port"          -> server.port(),
      "microservice.services.cachable.session-cache.port" -> server.port()
    )
    .overrides(
      api.inject.bind[AsyncCacheApi].toInstance(mock[AsyncCacheApi]),
      api.inject.bind[FeatureFlagService].toInstance(mockFeatureFlagService),
      api.inject.bind[PertaxAuthService].toInstance(mockPertaxAuthService)
    )
    .build()

  override lazy val keystoreData: Map[String, JsValue] = Map(
    s"TAXS_ATS_$taxYear" -> Json.parse(
      FileHelper.loadFile(
        s"./it/resources/atsData_2022.json",
        Map("testUtr" -> generatedNino.nino, "<TAXYEAR>" -> currentTaxYearForTesting.toString)
      )
    )
  )

  override def beforeEach(): Unit = {
    server.resetAll()
    super.beforeEach()
    reset(mockPertaxAuthService, mockFeatureFlagService)
    when(mockPertaxAuthService.authorise(ArgumentMatchers.any())).thenReturn(Future.successful(None))
    when(mockFeatureFlagService.get(ArgumentMatchers.eq(PAYEServiceToggle)))
      .thenReturn(Future.successful(FeatureFlag(PAYEServiceToggle, isEnabled = true)))
    when(mockFeatureFlagService.get(ArgumentMatchers.eq(SelfAssessmentServiceToggle)))
      .thenReturn(Future.successful(FeatureFlag(SelfAssessmentServiceToggle, isEnabled = true)))
  }

  // TODO DDCNL-9288 : Remove the override below when PAYE uprating done for tax year 2024
  override lazy val taxYear: Int = 2023

  "/income-before-tax" must {

    lazy val url = s"/annual-tax-summary/income-before-tax?taxYear=$taxYear"

    lazy val backendUrl = s"/taxs/$generatedSaUtr/$taxYear/ats-data"

    "return an OK response" in {

      server.stubFor(
        get(urlEqualTo(backendUrl))
          .willReturn(
            ok(
              FileHelper.loadFile(
                s"./it/resources/atsData_2022.json",
                Map("testUtr" -> generatedNino.nino, "<TAXYEAR>" -> currentTaxYearForTesting.toString)
              )
            )
          )
      )

      val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer 1")

      val result = route(fakeApplication(), request)
      result.map(status) mustBe Some(OK)
    }

    "return a 400 when TaxYearUtil.extractTaxYear returns invalid tax year" in {

      val failureUrl = "/annual-tax-summary/income-before-tax"

      server.stubFor(
        get(urlEqualTo(backendUrl))
          .willReturn(
            ok(
              FileHelper.loadFile(
                s"./it/resources/atsData_2022.json",
                Map("testUtr" -> generatedNino.nino, "<TAXYEAR>" -> currentTaxYearForTesting.toString)
              )
            )
          )
      )

      val request = FakeRequest(GET, failureUrl).withSession(SessionKeys.authToken -> "Bearer 1")

      val result = route(fakeApplication(), request)

      result.map(status) mustBe Some(SEE_OTHER)
      result.flatMap(redirectLocation) mustBe Some(controllers.routes.ErrorController.authorisedNoTaxYear.url)

    }

    "return a SEE_OTHER when the call to backend to retrieve ats-data throws a NOT_FOUND" in {

      server.stubFor(
        get(urlEqualTo(backendUrl))
          .willReturn(aResponse().withStatus(NOT_FOUND))
      )

      val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer 1")

      val result = route(fakeApplication(), request)

      result.map(status) mustBe Some(SEE_OTHER)
    }

    List(
      BAD_REQUEST,
      IM_A_TEAPOT,
      INTERNAL_SERVER_ERROR,
      SERVICE_UNAVAILABLE
    ).foreach { httpResponse =>
      s"return an INTERNAL_SERVER_ERROR when the call to backend to retrieve ats-data throws a $httpResponse" in {

        server.stubFor(
          get(urlEqualTo(backendUrl))
            .willReturn(aResponse().withStatus(httpResponse))
        )

        val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer 1")

        val result = route(fakeApplication(), request)

        result.map(status) mustBe Some(INTERNAL_SERVER_ERROR)
      }
    }
  }
}
