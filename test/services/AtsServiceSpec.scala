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

package services

import connectors.{DataCacheConnector, MiddleConnector}
import controllers.auth.AuthenticatedRequest
import models.AtsData
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.{SaUtr, Uar}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import utils.JsonUtil._
import utils.TestConstants._
import utils.{AccountUtils, AuthorityUtils}

import scala.concurrent.{ExecutionContext, Future}

class AtsServiceSpec extends UnitSpec with GuiceOneAppPerSuite with ScalaFutures with MockitoSugar {

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
    implicit val request = AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest())
  }

  "AtsService checkUtrAgainstCache" should {

    "not write data to the cache" when {

      "the user is an agent and the retrieved cached utr equals the requested utr" in new TestService {

        when(accountUtils.isAgent(request)).thenReturn(true)
        when(authUtils.checkUtr(eqTo(Some(testUtr)), eqTo(None))(any[AuthenticatedRequest[_]])).thenReturn(true)

        when(dataCache.getAgentToken(any[HeaderCarrier], any[ExecutionContext])).thenReturn(None)
        when(dataCache.fetchAndGetAtsForSession(eqTo(2014))(any[HeaderCarrier])).thenReturn(Some(data))
        when(dataCache.storeAtsForSession(any[AtsData])(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(Some(data)))

        val result = getAts(2014)

        whenReady(result) { result =>
          result shouldBe data
        }

        verify(auditService, never()).sendEvent(any[String], any[Map[String, String]], any[Option[String]])(any[Request[_]], any[HeaderCarrier])
        verify(dataCache, never()).storeAtsForSession(any[AtsData])(any[HeaderCarrier], any[ExecutionContext])
      }
    }

   "write data to the cache" when {
     "user is not an agent and the retrieved cached utr is different to the requested utr " in new TestService {

       when(authUtils.checkUtr(eqTo(Some(testNonMatchingUtr)), eqTo(None))(any[AuthenticatedRequest[_]])).thenReturn(false)

       when(dataCache.getAgentToken(any[HeaderCarrier], any[ExecutionContext])).thenReturn(None)
       when(dataCache.fetchAndGetAtsForSession(eqTo(2014))(any[HeaderCarrier])).thenReturn(Some(data))
       when(dataCache.storeAtsForSession(any[AtsData])(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(Some(data)))
       when(middleConnector.connectToAts(any[SaUtr], eqTo(2014))(any[HeaderCarrier])).thenReturn(Future.successful(data))

       val result = getAts(2014)

       whenReady(result) { result =>
         result shouldBe data
       }

       verify(middleConnector, times(1)).connectToAts(any[SaUtr], any[Int])(any[HeaderCarrier])
       verify(auditService, times(1)).sendEvent(any[String], any[Map[String, String]], any[Option[String]])(any[Request[_]], any[HeaderCarrier])
       verify(dataCache, times(1)).storeAtsForSession(any[AtsData])(any[HeaderCarrier], any[ExecutionContext])
     }

     "user is an agent and the retrieved cached utr is different to the requested utr" in new TestService {
       val agentRequest = AuthenticatedRequest("userId", Some(Uar(testUar)), Some(SaUtr(testUtr)), None, None, None, None, FakeRequest())

       when(authUtils.checkUtr(eqTo(Some(testUtr)), eqTo(Some(agentToken)))(any[AuthenticatedRequest[_]])).thenReturn(false)

       when(dataCache.getAgentToken(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Some(agentToken))
       when(dataCache.fetchAndGetAtsForSession(eqTo(2014))(any[HeaderCarrier])).thenReturn(Some(data))
       when(dataCache.storeAtsForSession(any[AtsData])(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(Some(data)))
       when(middleConnector.connectToAtsOnBehalfOf(any[Uar], any[SaUtr], eqTo(2014))(any[HeaderCarrier])).thenReturn(Future.successful(data))

       val result = getAts(2014)(hc, agentRequest)

       whenReady(result) { result =>
         result shouldBe data
       }

       verify(middleConnector, times(1)).connectToAtsOnBehalfOf(any[Uar], any[SaUtr], any[Int])(any[HeaderCarrier])
       verify(auditService, times(1)).sendEvent(any[String], any[Map[String, String]], any[Option[String]])(any[Request[_]], any[HeaderCarrier])
       verify(dataCache, times(1)).storeAtsForSession(any[AtsData])(any[HeaderCarrier], any[ExecutionContext])
     }

     "there is no data in the cache and user is an agent" in new TestService {
       val agentRequest = AuthenticatedRequest("userId", Some(Uar(testUar)), Some(SaUtr(testUtr)), None, None, None, None, FakeRequest())

       when(dataCache.fetchAndGetAtsForSession(eqTo(2014))(any[HeaderCarrier])).thenReturn(None)
       when(dataCache.getAgentToken(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Some(agentToken))

       when(authUtils.checkUtr(eqTo(Some(testUtr)), eqTo(Some(agentToken)))(any[AuthenticatedRequest[_]])).thenReturn(false)


       when(dataCache.storeAtsForSession(any[AtsData])(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(Some(data)))
       when(middleConnector.connectToAtsOnBehalfOf(any[Uar], any[SaUtr], eqTo(2014))(any[HeaderCarrier])).thenReturn(Future.successful(data))

       val result = getAts(2014)(hc, agentRequest)

       whenReady(result) { result =>
         result shouldBe data
       }

       verify(middleConnector, times(1)).connectToAtsOnBehalfOf(any[Uar], any[SaUtr], any[Int])(any[HeaderCarrier])
       verify(auditService, times(1)).sendEvent(any[String], any[Map[String, String]], any[Option[String]])(any[Request[_]], any[HeaderCarrier])
       verify(dataCache, times(1)).storeAtsForSession(any[AtsData])(any[HeaderCarrier], any[ExecutionContext])
     }
   }

  }
}
