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

package utils

import com.github.tomakehurst.wiremock.client.WireMock.{ok, post, urlEqualTo}
import config.ApplicationConfig
import org.mockito.Mockito.reset
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.Messages
import play.api.libs.json.JsValue
import play.api.test.Injecting
import uk.gov.hmrc.domain.{AtedUtr, Generator, Nino}
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService

import scala.concurrent.ExecutionContext

class IntegrationSpec
    extends AnyWordSpec
    with GuiceOneAppPerSuite
    with Matchers
    with WireMockHelper
    with ScalaFutures
    with IntegrationPatience
    with Injecting
    with MockitoSugar
    with TaxYearForTesting {

  protected val generatedNino: Nino = new Generator().nextNino

  protected val generatedSaUtr: AtedUtr = new Generator().nextAtedUtr

  protected lazy implicit val ec: ExecutionContext = inject[ExecutionContext]

  protected lazy val messages: Messages = inject[Messages]

  protected lazy val appConfig: ApplicationConfig = inject[ApplicationConfig]

  protected lazy val keystoreData: Map[String, JsValue] = Map.empty

  protected implicit lazy val mockFeatureFlagService: FeatureFlagService = mock[FeatureFlagService]

  override def beforeEach(): Unit = {

    super.beforeEach()
    reset(mockFeatureFlagService)

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
  }
}
