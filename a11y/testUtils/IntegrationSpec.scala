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

package testUtils

import com.github.tomakehurst.wiremock.client.WireMock.*
import config.ApplicationConfig
import models.{AgentToken, AtsData}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.test.Injecting
import repository.TaxsAgentTokenSessionCacheRepository
import uk.gov.hmrc.domain.{AtedUtr, Generator, Nino}
import uk.gov.hmrc.mongo.cache.DataKey
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import utils.TestConstants.mock
import utils.{Globals, TaxYearForTesting, WireMockHelper}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class IntegrationSpec
    extends AnyWordSpec
    with GuiceOneAppPerSuite
    with Matchers
    with WireMockHelper
    with ScalaFutures
    with IntegrationPatience
    with Injecting
    with TaxYearForTesting {

  val generatedNino: Nino = new Generator().nextNino

  val generatedSaUtr: AtedUtr = new Generator().nextAtedUtr

  lazy val ec: ExecutionContext = inject[ExecutionContext]

  lazy val messages: Messages = inject[Messages]

  lazy val appConfig: ApplicationConfig = inject[ApplicationConfig]

  lazy val fakeTaxYear: Int = currentTaxYear

  val mockTaxsAgentTokenSessionCacheRepository: TaxsAgentTokenSessionCacheRepository =
    mock[TaxsAgentTokenSessionCacheRepository]

  val atsData: AtsData =
    Json
      .fromJson[AtsData](
        Json.parse(
          atsData(currentTaxYear)
        )
      )
      .get

  val agentTokenMock: AgentToken = AgentToken("uar", generatedSaUtr.utr, Instant.now().toEpochMilli)

  implicit lazy val mockFeatureFlagService: FeatureFlagService = mock[FeatureFlagService]

  val authResponseNoSA: String =
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
       |        "currentLogin": "$currentTaxYear-06-07T10:52:02.594Z",
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

  val authResponseSA: String =
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
       |        "currentLogin": "$currentTaxYear-06-07T10:52:02.594Z",
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

  override def beforeEach(): Unit = {
    super.beforeEach()

    server.stubFor(
      post(urlEqualTo("/auth/authorise"))
        .willReturn(ok(authResponseNoSA))
    )

    server.stubFor(
      post(urlEqualTo("/pertax/authorise"))
        .willReturn(ok("{\"code\": \"ACCESS_GRANTED\", \"message\": \"Access granted\"}"))
    )

    when(mockTaxsAgentTokenSessionCacheRepository.putSession[AgentToken](DataKey(any), any)(any, any, any))
      .thenReturn(Future.successful((Globals.TAXS_AGENT_TOKEN_KEY, "token")))

    when(mockTaxsAgentTokenSessionCacheRepository.getFromSession[AgentToken](DataKey(any))(any, any))
      .thenReturn(Future.successful(Some(agentTokenMock)))
    ()
  }
}
