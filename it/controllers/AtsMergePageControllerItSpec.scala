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

import com.github.tomakehurst.wiremock.client.WireMock.{status => _, _}
import connectors.DataCacheConnector
import models.{AgentToken, AtsListData}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import utils.{FileHelper, Globals, IntegrationSpec, LoginPage}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class AtsMergePageControllerItSpec extends IntegrationSpec with MockitoSugar {

  lazy override implicit val ec = inject[ExecutionContext]

  lazy implicit val hc = inject[HeaderCarrier]

  val atsListData = Json.fromJson[AtsListData](Json.parse(FileHelper.loadFile("./it/resources/atsList.json"))).get

  val agentTokenMock = AgentToken("uar", generatedSaUtr.utr, Instant.now().toEpochMilli)

  val mockDataCacheConnector = mock[DataCacheConnector]

  val agentToken = LoginPage.agentToken(generatedSaUtr.utr)

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .configure(
      "microservice.services.auth.port"             -> server.port(),
      "microservice.services.tax-summaries.port"    -> server.port(),
      "microservice.services.message-frontend.port" -> server.port()
    )
    .overrides(
      api.inject.bind[DataCacheConnector].toInstance(mockDataCacheConnector)
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
         |        "currentLogin": "2021-06-07T10:52:02.594Z",
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
         |    "allEnrolments": [{
         |        "key": "IR-SA-AGENT",
         |        "identifiers": [
         |          {
         |            "key": "IRAgentReference",
         |            "value": "uar"
         |          }],
         |        "state": "Activated"
         |     }]
         |}
         |""".stripMargin

    server.stubFor(
      post(urlEqualTo("/auth/authorise"))
        .willReturn(ok(authResponse))
    )
  }

  when(mockDataCacheConnector.storeAgentToken(any[String])(any[HeaderCarrier], any[ExecutionContext]))
    .thenReturn(Future.successful("token"))

  when(mockDataCacheConnector.fetchAndGetAtsListForSession(any[HeaderCarrier]))
    .thenReturn(Future.successful(Some(atsListData)))

  when(mockDataCacheConnector.getAgentToken(any[HeaderCarrier], any()))
    .thenReturn(Future.successful(Some(agentTokenMock)))

  when(mockDataCacheConnector.storeAtsListForSession(any())(any(), any()))
    .thenReturn(Future.successful(Some(atsListData)))

  "/income-before-tax" must {

    lazy val url = s"/annual-tax-summary/paye/main?ref=PORTAL&id=$agentToken"

    lazy val backendUrlSa = s"/taxs/$generatedSaUtr/ats-list"

    lazy val backendUrlPaye =
      s"/taxs/$generatedNino/${appConfig.taxYear - appConfig.maxTaxYearsTobeDisplayed}/${appConfig.taxYear}/paye-ats-data"

    lazy val messageFrontendUrl = "/messages/count?read=No"

    "return an OK response with appropriate query parameters for Agent when data is retrieved from backend for both atsList and payeData" in {

      server.stubFor(
        get(urlEqualTo(backendUrlSa))
          .willReturn(ok(FileHelper.loadFile("./it/resources/atsList.json")))
      )

      server.stubFor(
        get(urlEqualTo(backendUrlPaye))
          .willReturn(ok(FileHelper.loadFile("./it/resources/payeData.json")))
      )

      val request = FakeRequest(GET, url).withSession(SessionKeys.authToken -> "Bearer 1")

      val result = route(fakeApplication(), request)

      result.map(status) mustBe Some(OK)

      request.getQueryString(Globals.TAXS_USER_TYPE_QUERY_PARAMETER) mustBe Some("PORTAL")

      request.getQueryString(Globals.TAXS_AGENT_TOKEN_ID).isDefined mustBe true
    }

    "return an OK response without query parameters when data is retrieved from backend for both atsList and payeData" in {

      lazy val url = s"/annual-tax-summary/paye/main"

      lazy val backendUrlSa = s"/taxs/$generatedSaUtr/ats-list"

      lazy val backendUrlPaye =
        s"/taxs/$generatedNino/${appConfig.taxYear - appConfig.maxTaxYearsTobeDisplayed}/${appConfig.taxYear}/paye-ats-data"

      server.stubFor(
        get(urlEqualTo(backendUrlSa))
          .willReturn(ok(FileHelper.loadFile("./it/resources/atsList.json")))
      )

      server.stubFor(
        get(urlEqualTo(backendUrlPaye))
          .willReturn(ok(FileHelper.loadFile("./it/resources/payeData.json")))
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
          .willReturn(ok(FileHelper.loadFile("./it/resources/atsList.json")))
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
          .willReturn(ok(FileHelper.loadFile("./it/resources/payeData.json")))
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

    "return an OK response with unread message count indicatior when message-frontend call is successful" in {

      val messageCount = Random.nextInt(100) + 1
      server.stubFor(
        get(urlEqualTo(backendUrlSa))
          .willReturn(ok(FileHelper.loadFile("./it/resources/atsList.json")))
      )

      server.stubFor(
        get(urlEqualTo(backendUrlPaye))
          .willReturn(ok(FileHelper.loadFile("./it/resources/payeData.json")))
      )

      server.stubFor(get(urlEqualTo(messageFrontendUrl)).willReturn(ok(s"""{"count": $messageCount}""")))

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
            .willReturn(ok(FileHelper.loadFile("./it/resources/payeData.json")))
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
      s"return an INTERNAL_SERVER_ERROR when the call to backend to retrieve payeData throws a $httpResponse" in {

        server.stubFor(
          get(urlEqualTo(backendUrlSa))
            .willReturn(ok(FileHelper.loadFile("./it/resources/atsList.json")))
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
