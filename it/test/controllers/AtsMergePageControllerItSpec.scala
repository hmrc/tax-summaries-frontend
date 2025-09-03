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

package controllers

import com.github.tomakehurst.wiremock.client.WireMock.{status as _, *}
import models.AgentToken
import models.admin.{PAYEServiceToggle, SelfAssessmentServiceToggle}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any as mockAny
import org.mockito.Mockito.{reset as mockReset, when}
import play.api
import play.api.Application
import play.api.cache.AsyncCacheApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repository.TaxsAgentTokenSessionCacheRepository
import services.PertaxAuthService
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.mongo.cache.DataKey
import uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlag
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import utils.{Globals, IntegrationSpec, LoginPage}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class AtsMergePageControllerItSpec extends IntegrationSpec {
  private val mockPertaxAuthService               = mock[PertaxAuthService]
  lazy override implicit val ec: ExecutionContext = inject[ExecutionContext]

  lazy implicit val hc: HeaderCarrier = inject[HeaderCarrier]

  val agentTokenMock: AgentToken = AgentToken("uar", generatedSaUtr.utr, Instant.now().toEpochMilli)

  private val mockTaxsAgentTokenSessionCacheRepository = mock[TaxsAgentTokenSessionCacheRepository]

  val agentToken: String = LoginPage.agentToken(generatedSaUtr.utr)

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .configure(
      "microservice.services.auth.port"          -> server.port(),
      "microservice.services.tax-summaries.port" -> server.port()
    )
    .overrides(
      api.inject.bind[AsyncCacheApi].toInstance(mock[AsyncCacheApi]),
      api.inject.bind[FeatureFlagService].toInstance(mockFeatureFlagService),
      api.inject.bind[PertaxAuthService].toInstance(mockPertaxAuthService)
    )
    .build()

  override def beforeEach(): Unit = {

    super.beforeEach()

    val authResponse =
      s"""
         |{
         |    "confidenceLevel": 200,
         |    "nino": "$generatedNino",
         |    "saUtr": "$generatedSaUtr",
         |    "name": {
         |        "name": "John",
         |        "lastName": "Smith"
         |    },
         |    "loginTimes": {
         |        "currentLogin": "$currentTaxYearSA-06-07T10:52:02.594Z",
         |        "previousLogin": null
         |    },
         |    "optionalCredentials": {
         |        "providerId": "4911434741952698",
         |        "providerType": "GovernmentGateway"
         |    },
         |    "authProviderId": {
         |        "ggCredId": "xyz"
         |    },
         |    "externalId": "testExternalId",
         |    "allEnrolments": []
         |}
         |""".stripMargin

    server.stubFor(
      post(urlEqualTo("/auth/authorise"))
        .willReturn(ok(authResponse))
    )

    mockReset(mockFeatureFlagService, mockPertaxAuthService)

    when(mockPertaxAuthService.authorise(mockAny())).thenReturn(Future.successful(None))

    when(mockFeatureFlagService.get(ArgumentMatchers.eq(PAYEServiceToggle)))
      .thenReturn(Future.successful(FeatureFlag(PAYEServiceToggle, isEnabled = true)))
    when(mockFeatureFlagService.get(ArgumentMatchers.eq(SelfAssessmentServiceToggle)))
      .thenReturn(Future.successful(FeatureFlag(SelfAssessmentServiceToggle, isEnabled = true)))
  }

  when(
    mockTaxsAgentTokenSessionCacheRepository
      .putSession[AgentToken](DataKey(mockAny), mockAny)(mockAny, mockAny, mockAny)
  ).thenReturn(Future.successful((Globals.TAXS_AGENT_TOKEN_KEY, "token")))

  when(mockTaxsAgentTokenSessionCacheRepository.getFromSession[AgentToken](DataKey(mockAny))(mockAny, mockAny))
    .thenReturn(
      Future
        .successful(Some(agentTokenMock))
    )

  private def atsList(taxYears: Seq[Int]) = {
    val ty = taxYears.foldLeft("")((acc, c) => acc + (if (acc.isEmpty) "" else ",") + c)
    """{
      |  "utr":"$utr",
      |  "taxPayer":{"title":"Mr","forename":"forename","surname":"surname"},
      |  "atsYearList":[$YEARS]
      |}""".stripMargin.replace("$YEARS", ty)
  }

  private def allPreviousYears: Seq[Int] = {
    val yearFrom = appConfig.taxYearSA - appConfig.maxTaxYearsTobeDisplayed + 1
    yearFrom to appConfig.taxYearSA
  }

  "/income-before-tax" must {

    lazy val url = s"/annual-tax-summary/paye/main?ref=PORTAL&id=$agentToken"

    lazy val backendUrlSa = s"/taxs/$generatedSaUtr/${appConfig.taxYearSA}/4/ats-list"

    lazy val backendUrlPaye =
      s"/taxs/$generatedNino/${appConfig.taxYearSA - appConfig.maxTaxYearsTobeDisplayed + 1}/${appConfig.taxYearSA}/paye-ats-data"

    "return an OK response with appropriate query parameters for Agent when data is retrieved from backend for both atsList and payeData" in {

      server.stubFor(
        get(urlEqualTo(backendUrlSa))
          .willReturn(ok(atsList(allPreviousYears)))
      )

      server.stubFor(
        get(urlEqualTo(backendUrlPaye))
          .willReturn(
            ok(
              payeAtsDataForYearRange()
            )
          )
      )

      val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer 1")

      val result = route(fakeApplication(), request)

      result.map(status) mustBe Some(OK)

      request.getQueryString(Globals.TAXS_USER_TYPE_QUERY_PARAMETER) mustBe Some("PORTAL")

      request.getQueryString(Globals.TAXS_AGENT_TOKEN_ID).isDefined mustBe true
    }

    "return an OK response without query parameters when data is retrieved from backend for both atsList and payeData" in {

      lazy val url = s"/annual-tax-summary/paye/main"

      lazy val backendUrlSa = s"/taxs/$generatedSaUtr/${appConfig.taxYearSA}/4/ats-list"

      lazy val backendUrlPaye =
        s"/taxs/$generatedNino/${appConfig.taxYearSA - appConfig.maxTaxYearsTobeDisplayed}/${appConfig.taxYearSA}/paye-ats-data"

      server.stubFor(
        get(urlEqualTo(backendUrlSa))
          .willReturn(ok(atsList(allPreviousYears)))
      )

      server.stubFor(
        get(urlEqualTo(backendUrlPaye))
          .willReturn(
            ok(
              payeAtsDataForYearRange()
            )
          )
      )

      val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer 1")

      val result = route(fakeApplication(), request)

      result.map(status) mustBe Some(OK)

      request.getQueryString(Globals.TAXS_USER_TYPE_QUERY_PARAMETER) mustBe None

      request.getQueryString(Globals.TAXS_AGENT_TOKEN_ID).isDefined mustBe false
    }

    "return an OK response when data is retrieved from backend for atsList but no payeData found" in {

      server.stubFor(
        get(urlEqualTo(backendUrlSa))
          .willReturn(ok(atsList(allPreviousYears)))
      )

      server.stubFor(
        get(urlEqualTo(backendUrlPaye))
          .willReturn(aResponse().withStatus(NOT_FOUND))
      )

      val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer 1")

      val result = route(fakeApplication(), request)

      result.map(status) mustBe Some(OK)

      request.getQueryString(Globals.TAXS_USER_TYPE_QUERY_PARAMETER) mustBe Some("PORTAL")

      request.getQueryString(Globals.TAXS_AGENT_TOKEN_ID).isDefined mustBe true
    }

    "return an OK response when data is retrieved from backend for payeData but no atsList data found" in {

      server.stubFor(
        get(urlEqualTo(backendUrlSa))
          .willReturn(aResponse().withStatus(NOT_FOUND))
      )

      server.stubFor(
        get(urlEqualTo(backendUrlPaye))
          .willReturn(
            ok(
              payeAtsDataForYearRange()
            )
          )
      )

      val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer 1")

      val result = route(fakeApplication(), request)

      result.map(status) mustBe Some(OK)

      request.getQueryString(Globals.TAXS_USER_TYPE_QUERY_PARAMETER) mustBe Some("PORTAL")

      request.getQueryString(Globals.TAXS_AGENT_TOKEN_ID).isDefined mustBe true
    }

    "return an OK response when call to backend is successful but no data is found for either atsList or payeData" in {

      server.stubFor(
        get(urlEqualTo(backendUrlSa))
          .willReturn(aResponse().withStatus(NOT_FOUND))
      )

      server.stubFor(
        get(urlEqualTo(backendUrlPaye))
          .willReturn(aResponse().withStatus(NOT_FOUND))
      )

      val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer 1")

      val result = route(fakeApplication(), request)

      result.map(status) mustBe Some(OK)

      request.getQueryString(Globals.TAXS_USER_TYPE_QUERY_PARAMETER) mustBe Some("PORTAL")

      request.getQueryString(Globals.TAXS_AGENT_TOKEN_ID).isDefined mustBe true
    }

    List(
      BAD_REQUEST,
      IM_A_TEAPOT,
      INTERNAL_SERVER_ERROR,
      SERVICE_UNAVAILABLE
    ).foreach { httpResponse =>
      s"return an INTERNAL_SERVER_ERROR when the call to backend to retrieve atsList data throws a $httpResponse" in {

        server.stubFor(
          get(urlEqualTo(backendUrlSa))
            .willReturn(aResponse().withStatus(httpResponse))
        )

        server.stubFor(
          get(urlEqualTo(backendUrlPaye))
            .willReturn(
              ok(
                payeAtsDataForYearRange(noOfYears = 2)
              )
            )
        )

        val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer 1")

        val result = route(fakeApplication(), request)

        result.map(status) mustBe Some(INTERNAL_SERVER_ERROR)
      }
    }

    List(
      BAD_REQUEST,
      IM_A_TEAPOT,
      INTERNAL_SERVER_ERROR,
      SERVICE_UNAVAILABLE
    ).foreach { httpResponse =>
      s"return a success response when the call to backend to retrieve payeData throws a $httpResponse but sa data present for ALL previous tax years" in {
        server.stubFor(get(urlEqualTo(backendUrlSa)).willReturn(ok(atsList(allPreviousYears))))

        server.stubFor(
          get(urlEqualTo(backendUrlPaye))
            .willReturn(aResponse().withStatus(httpResponse))
        )

        val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer 1")

        val result = route(fakeApplication(), request)

        result.map(status) mustBe Some(OK)
      }
    }

    List(
      BAD_REQUEST,
      IM_A_TEAPOT,
      INTERNAL_SERVER_ERROR,
      SERVICE_UNAVAILABLE
    ).foreach { httpResponse =>
      s"return an INTERNAL_SERVER_ERROR when the call to backend to retrieve payeData throws a $httpResponse and sa data not present for all years (one year missing)" in {

        server.stubFor(
          get(urlEqualTo(backendUrlSa)).willReturn(
            ok(atsList(allPreviousYears.take(appConfig.maxTaxYearsTobeDisplayed - 1)))
          )
        )

        server.stubFor(
          get(urlEqualTo(backendUrlPaye))
            .willReturn(aResponse().withStatus(httpResponse))
        )

        val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer 1")

        val result = route(fakeApplication(), request)

        result.map(status) mustBe Some(INTERNAL_SERVER_ERROR)
      }
    }

    List(
      BAD_REQUEST,
      IM_A_TEAPOT,
      INTERNAL_SERVER_ERROR,
      SERVICE_UNAVAILABLE
    ).foreach { httpResponse =>
      s"return an INTERNAL_SERVER_ERROR when the call to backend to retrieve both atsList data and payeData throws a $httpResponse" in {

        server.stubFor(
          get(urlEqualTo(backendUrlSa))
            .willReturn(aResponse().withStatus(httpResponse))
        )

        server.stubFor(
          get(urlEqualTo(backendUrlPaye))
            .willReturn(aResponse().withStatus(httpResponse))
        )

        val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer 1")

        val result = route(fakeApplication(), request)

        result.map(status) mustBe Some(INTERNAL_SERVER_ERROR)
      }
    }
  }
}
