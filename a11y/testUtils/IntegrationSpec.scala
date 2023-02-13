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

package testUtils

import com.github.tomakehurst.wiremock.client.WireMock._
import config.ApplicationConfig
import connectors.DataCacheConnector
import models.{AgentToken, AtsData, AtsListData}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.test.Injecting
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.TestConstants.mock
import utils.WireMockHelper
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class IntegrationSpec
    extends AnyWordSpec
    with GuiceOneAppPerSuite
    with Matchers
    with WireMockHelper
    with ScalaFutures
    with IntegrationPatience
    with Injecting {

  val generatedNino = new Generator().nextNino

  val generatedSaUtr = new Generator().nextAtedUtr

  lazy val ec = inject[ExecutionContext]

  lazy val messages = inject[Messages]

  lazy val appConfig: ApplicationConfig = inject[ApplicationConfig]

  lazy val fakeTaxYear: Int = 2022

  val mockDataCacheConnector = mock[DataCacheConnector]

  val atsListData: AtsListData = Json.fromJson[AtsListData](Json.parse(FileHelper.loadFile("./it/resources/atsList.json"))).get

  val atsData: AtsData = Json.fromJson[AtsData](Json.parse(FileHelper.loadFile(s"./it/resources/atsData_$fakeTaxYear.json"))).get

  val agentTokenMock = AgentToken("uar", generatedSaUtr.utr, Instant.now().toEpochMilli)

  val authResponseNoSA =
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
       |    "allEnrolments": []
       |}
       |""".stripMargin

  val authResponseSA =
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


  override def beforeEach() = {
    super.beforeEach()

    server.stubFor(
      put(urlMatching(s"/keystore/tax-summaries-frontend/.*"))
        .willReturn(ok(Json.toJson(CacheMap("id", Map.empty)).toString))
    )

    server.stubFor(
      post(urlEqualTo("/auth/authorise"))
        .willReturn(ok(authResponseNoSA))
    )

    when(mockDataCacheConnector.storeAgentToken(any[String])(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.successful("token"))

    when(mockDataCacheConnector.fetchAndGetAtsListForSession(any[HeaderCarrier]))
      .thenReturn(Future.successful(Some(atsListData)))

    when(mockDataCacheConnector.getAgentToken(any[HeaderCarrier], any()))
      .thenReturn(Future.successful(Some(agentTokenMock)))

    when(mockDataCacheConnector.storeAtsListForSession(any())(any(), any()))
      .thenReturn(Future.successful(Some(atsListData)))

    when(mockDataCacheConnector.fetchAndGetAtsForSession(any())(any()))
      .thenReturn(Future.successful(Some(atsData)))

    when(mockDataCacheConnector.storeAtsForSession(any())(any(), any()))
      .thenReturn(Future.successful(Some(atsData)))
  }
}
