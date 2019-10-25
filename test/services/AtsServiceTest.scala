/*
 * Copyright 2019 HM Revenue & Customs
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

package services

import connectors.{DataCacheConnector, MiddleConnector}
import controllers.FakeTaxsPlayApplication
import models.AtsData
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.{SaUtr, Uar}
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import uk.gov.hmrc.play.test.UnitSpec
import utils.{AccountUtils, AuthorityUtils}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import utils.TestConstants._
import utils.JsonUtil._
import uk.gov.hmrc.http.HeaderCarrier

class AtsServiceTest extends UnitSpec with FakeTaxsPlayApplication with ScalaFutures with MockitoSugar {

  val data = {
    val json = loadAndParseJsonWithDummyData("/summary_json_test.json")
    Json.fromJson[AtsData](json).get
  }

  class TestService extends AtsService {

    override lazy val middleConnector: MiddleConnector = mock[MiddleConnector]
    override lazy val dataCache: DataCacheConnector = mock[DataCacheConnector]
    override lazy val auditService: AuditService = mock[AuditService]
    override lazy val authUtils: AuthorityUtils = mock[AuthorityUtils]
    override lazy val accountUtils: AccountUtils = mock[AccountUtils]

    val agentToken = AgentToken(
      agentUar = testUar,
      clientUtr = testUtr,
      timestamp = 0
    )

    implicit val hc = new HeaderCarrier
    implicit val request = FakeRequest()
  }

  "AtsService checkUtrAgainstCache" should {

    "not write data to the cache when the retrieved cached utr equals the requested utr " in new TestService {

      implicit val user = User(AuthorityUtils.saAuthority(testOid, testUtr))

      when(accountUtils.isAgent(user)).thenReturn(true)
      when(authUtils.checkUtr(eqTo(Some(testUtr)), eqTo(None))(any[User])).thenReturn(true)

      when(dataCache.getAgentToken(any[HeaderCarrier], any[ExecutionContext])).thenReturn(None)
      when(dataCache.fetchAndGetAtsForSession(eqTo(2014))(any[HeaderCarrier])).thenReturn(Some(data))
      when(dataCache.storeAtsForSession(any[AtsData])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(data)))
      when(middleConnector.connectToAts(any[SaUtr], eqTo(2014))(any[HeaderCarrier])).thenReturn(Future.successful(data))

      val result = getAts(2014)

      whenReady(result) { result =>
        result shouldBe data
      }

      verify(auditService, never())
        .sendEvent(any[String], any[Map[String, String]], any[Option[String]])(any[Request[_]], any[HeaderCarrier])
      verify(dataCache, never()).storeAtsForSession(any[AtsData])(any[HeaderCarrier], any[ExecutionContext])
    }

    "write data to the cache when the retrieved cached utr is different to the requested utr " in new TestService {

      implicit val user = User(AuthorityUtils.saAuthority(testOid, testNonMatchingUtr))

      when(authUtils.checkUtr(eqTo(Some(testNonMatchingUtr)), eqTo(None))(any[User])).thenReturn(false)

      when(dataCache.getAgentToken(any[HeaderCarrier], any[ExecutionContext])).thenReturn(None)
      when(dataCache.fetchAndGetAtsForSession(eqTo(2014))(any[HeaderCarrier])).thenReturn(Some(data))
      when(dataCache.storeAtsForSession(any[AtsData])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(data)))
      when(middleConnector.connectToAts(any[SaUtr], eqTo(2014))(any[HeaderCarrier])).thenReturn(Future.successful(data))

      val result = getAts(2014)

      whenReady(result) { result =>
        result shouldBe data
      }
      verify(auditService, times(1))
        .sendEvent(any[String], any[Map[String, String]], any[Option[String]])(any[Request[_]], any[HeaderCarrier])
      verify(dataCache, times(1)).storeAtsForSession(any[AtsData])(any[HeaderCarrier], any[ExecutionContext])
    }

    "write data to the cache when the retrieved cached utr is different to the requested utr - AGENT" in new TestService {

      implicit val user = User(AuthorityUtils.taxsAgentAuthority(testOid, testNonMatchingUtr))

      when(authUtils.checkUtr(eqTo(Some(testUtr)), eqTo(Some(agentToken)))(any[User])).thenReturn(false)

      when(dataCache.getAgentToken(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Some(agentToken))
      when(dataCache.fetchAndGetAtsForSession(eqTo(2014))(any[HeaderCarrier])).thenReturn(Some(data))
      when(dataCache.storeAtsForSession(any[AtsData])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(data)))
      when(middleConnector.connectToAtsOnBehalfOf(any[Uar], any[SaUtr], eqTo(2014))(any[HeaderCarrier]))
        .thenReturn(Future.successful(data))

      val result = getAts(2014)

      whenReady(result) { result =>
        result shouldBe data
      }
      verify(auditService, times(1))
        .sendEvent(any[String], any[Map[String, String]], any[Option[String]])(any[Request[_]], any[HeaderCarrier])
      verify(dataCache, times(1)).storeAtsForSession(any[AtsData])(any[HeaderCarrier], any[ExecutionContext])
    }

    "not write data to the cache if an agent with no token" in new TestService {

      implicit val user = User(AuthorityUtils.taxsAgentAuthority(testOid, testUtr))

      when(dataCache.getAgentToken(any[HeaderCarrier], any[ExecutionContext])).thenReturn(None)
      when(dataCache.fetchAndGetAtsForSession(eqTo(2014))(any[HeaderCarrier])).thenReturn(Some(data))
      when(middleConnector.connectToAts(any[SaUtr], eqTo(2014))(any[HeaderCarrier])).thenReturn(Future.successful(data))

      verify(auditService, never())
        .sendEvent(any[String], any[Map[String, String]], any[Option[String]])(any[Request[_]], any[HeaderCarrier])
      verify(dataCache, never()).storeAtsForSession(any[AtsData])(any[HeaderCarrier], any[ExecutionContext])
    }
  }
}
