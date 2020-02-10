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

package services.paye

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
import services.{AgentToken, AuditService}
import uk.gov.hmrc.domain.{Nino, SaUtr, Uar}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import utils.JsonUtil._
import utils.TestConstants._
import utils.{AccountUtils, AuthorityUtils}

import scala.concurrent.{ExecutionContext, Future}

class PayeAtsServiceSpec extends UnitSpec with GuiceOneAppPerSuite with ScalaFutures with MockitoSugar {

  val data = {
    val json = loadAndParseJsonWithDummyData("/summary_json_test.json")
    Json.fromJson[AtsData](json).get
  }

  val payeData = {
    val json = loadAndParseJsonWithDummyData("/summary_paye_json_test.json")
    Json.fromJson[AtsData](json).get
  }

  class TestService extends PayeAtsService {

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
    implicit val ninoRequest = AuthenticatedRequest("userId", None, None, Some(Nino(testNino)), None, None, None, FakeRequest())
  }

  "AtsService checkNinoAgainstCache" should {
    "not write data to the cache" when {
      "retrieved cached nino equals the requested nino" in new TestService {

        when(dataCache.fetchAndGetAtsForSession(eqTo(2014))(any[HeaderCarrier])).thenReturn(Some(payeData))

        val result = getAts(2014)(hc, ninoRequest)

        whenReady(result) { result =>
          result shouldBe payeData
        }

        verify(auditService, never()).sendEvent(any[String], any[Map[String, String]], any[Option[String]])(any[Request[_]], any[HeaderCarrier])
        verify(dataCache, never()).storeAtsForSession(any[AtsData])(any[HeaderCarrier], any[ExecutionContext])
      }

    }

    "write data to the cache" when {
      "there is no data in the cache" in new TestService {
        when(dataCache.fetchAndGetAtsForSession(eqTo(2014))(any[HeaderCarrier])).thenReturn(None)
        when(dataCache.storeAtsForSession(any[AtsData])(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(Some(payeData)))
        when(middleConnector.connectToPayeAts(any[Nino], eqTo(2014))(any[HeaderCarrier])).thenReturn(Future.successful(payeData))

        val result = getAts(2014)(hc, ninoRequest)

        whenReady(result) { result =>
          result shouldBe payeData
        }

        verify(middleConnector, times(1)).connectToPayeAts(any[Nino], any[Int])(any[HeaderCarrier])
        verify(auditService, times(1)).sendEvent(any[String], any[Map[String, String]], any[Option[String]])(any[Request[_]], any[HeaderCarrier])
        verify(dataCache, times(1)).storeAtsForSession(any[AtsData])(any[HeaderCarrier], any[ExecutionContext])
      }
    }
  }
}
