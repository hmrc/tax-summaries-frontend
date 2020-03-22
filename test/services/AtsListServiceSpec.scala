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
import models.AtsListData
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.{SaUtr, TaxIdentifier, Uar}
import uk.gov.hmrc.play.test.UnitSpec
import utils.{AccountUtils, AgentTokenException, AuthorityUtils}
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => eqTo, _}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import utils.TestConstants._

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import uk.gov.hmrc.http.HeaderCarrier

class AtsListServiceSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar with ScalaFutures {

  val data = {
    val source = Source.fromURL(getClass.getResource("/test_list_utr.json")).mkString
    val json = Json.parse(source)
    Json.fromJson[AtsListData](json).get
  }

  class TestService extends AtsListService {

    implicit val request = AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest())
    implicit val hc = new HeaderCarrier

    val agentToken = AgentToken(
      agentUar = testUar,
      clientUtr = testUtr,
      timestamp = "0"
    )

    override lazy val middleConnector: MiddleConnector = mock[MiddleConnector]
    override lazy val dataCache: DataCacheConnector = mock[DataCacheConnector]
    override lazy val cryptoService: CryptoService = mock[CryptoService]
    override lazy val authUtils: AuthorityUtils = mock[AuthorityUtils]
    override lazy val auditService: AuditService = mock[AuditService]
    override lazy val accountUtils: AccountUtils = mock[AccountUtils]


    when(dataCache.storeAtsTaxYearForSession(eqTo(2014))(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(Some(2014)))
    when(dataCache.storeAtsTaxYearForSession(eqTo(2015))(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(Some(2015)))
    when(dataCache.storeAtsListForSession(any[AtsListData])(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(Some(data)))

    when(dataCache.fetchAndGetAtsTaxYearForSession(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(Some(2014)))

    when(dataCache.fetchAndGetAtsListForSession(any[HeaderCarrier])).thenReturn(Future.successful(Some(data)))

    // By default we don't want to be an agent
    when(dataCache.getAgentToken(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(None))

    when(middleConnector.connectToAtsList(any[SaUtr])(any[HeaderCarrier])).thenReturn(Future.successful(data))
    when(middleConnector.connectToAtsListOnBehalfOf(any[Uar], any[SaUtr])(any[HeaderCarrier])).thenReturn(Future.successful(data))

    when(authUtils.checkUtr(any[String], any[Option[AgentToken]])(any[AuthenticatedRequest[_]])).thenReturn(true)

  }

  "storeSelectedTaxYear" should {

    "Return a successful future upon success" in new TestService {

      val result = storeSelectedTaxYear(2014)

      whenReady(result) { result =>
        result shouldBe 2014
      }
    }

    "Return a failed future when None is returned from the dataCache" in new TestService {

      when(dataCache.storeAtsTaxYearForSession(eqTo(2014))(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(None))

      val result = storeSelectedTaxYear(2014)

      whenReady(result.failed) { exception =>
        exception shouldBe a [NoSuchElementException]
      }
    }

    "Return a failed future when the dataCache future has failed" in new TestService {

      when(dataCache.storeAtsTaxYearForSession(eqTo(2014))(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.failed(new Exception("failed")))

      val result = storeSelectedTaxYear(2014)

      whenReady(result.failed) { exception =>
        exception shouldBe an [Exception]
      }
    }
  }

  "fetchSelectedTaxYear" should {

    "Return a successful future upon success" in new TestService {

      whenReady(fetchSelectedTaxYear) { result =>
        result shouldBe 2014
      }
    }

    "Return a failed future when None is returned from the dataCache" in new TestService {

      when(dataCache.fetchAndGetAtsTaxYearForSession(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(None))

      whenReady(fetchSelectedTaxYear.failed) { exception =>
        exception shouldBe a [NoSuchElementException]
      }
    }

    "Return a failed future when the dataCache future has failed" in new TestService {

      when(dataCache.fetchAndGetAtsTaxYearForSession(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.failed(new Exception("failed")))

      whenReady(fetchSelectedTaxYear.failed) { exception =>
        exception shouldBe an [Exception]
      }
    }
  }

  "getAtsYearList" should {

    "Return a failed future when the call to the dataCache fails (fetch)" in new TestService {

      when(dataCache.fetchAndGetAtsListForSession(any[HeaderCarrier])).thenReturn(Future.failed(new Exception("failed")))

      whenReady(getAtsYearList.failed) { exception =>
        exception shouldBe an [Exception]

        verify(dataCache, times(1)).fetchAndGetAtsListForSession(any[HeaderCarrier])
        verify(dataCache, never()).storeAtsListForSession(eqTo(data))(any[HeaderCarrier], any[ExecutionContext])
        verify(middleConnector, never()).connectToAtsList(any[SaUtr])(any[HeaderCarrier])
      }
    }

   "Return a failed future when the call to the dataCache fails (store)" in new TestService {

      when(dataCache.fetchAndGetAtsListForSession(any[HeaderCarrier])).thenReturn(Future.successful(None))
      when(dataCache.storeAtsListForSession(any[AtsListData])(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.failed(new Exception("failed")))

      whenReady(getAtsYearList.failed) { exception =>
        exception shouldBe an [Exception]

        verify(dataCache, times(1)).fetchAndGetAtsListForSession(any[HeaderCarrier])
        verify(dataCache, times(1)).storeAtsListForSession(eqTo(data))(any[HeaderCarrier], any[ExecutionContext])
        verify(middleConnector, times(1)).connectToAtsList(any[SaUtr])(any[HeaderCarrier])
      }
    }

    "Return a failed future when the call to the MS fails" in new TestService {

      when(dataCache.fetchAndGetAtsListForSession(any[HeaderCarrier])).thenReturn(Future.successful(None))
      when(middleConnector.connectToAtsList(any[SaUtr])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new Exception("failed")))

      whenReady(getAtsYearList.failed) { exception =>
        exception shouldBe an [Exception]

        verify(dataCache, times(1)).fetchAndGetAtsListForSession(any[HeaderCarrier])
        verify(dataCache, never()).storeAtsListForSession(eqTo(data))(any[HeaderCarrier], any[ExecutionContext])
        verify(middleConnector, times(1)).connectToAtsList(any[SaUtr])(any[HeaderCarrier])
      }
    }

    "User" should {

      "Return the ats year list data for a user from the cache" in new TestService {

        when(accountUtils.isAgent(request)).thenReturn(true)

        whenReady(getAtsYearList) { result =>
          result shouldBe data
        }

        verify(auditService, never()).sendEvent(any[String], any[Map[String, String]], any[Option[String]])(any[Request[AnyRef]], any[HeaderCarrier])
        verify(dataCache, times(1)).fetchAndGetAtsListForSession(any[HeaderCarrier])
        verify(dataCache, never()).storeAtsListForSession(eqTo(data))(any[HeaderCarrier], any[ExecutionContext])
        verify(middleConnector, never()).connectToAtsList(any[SaUtr])(any[HeaderCarrier])
      }
    }

    "Return the ats year list data for a user from the MS" in new TestService {

      when(dataCache.fetchAndGetAtsListForSession(any[HeaderCarrier])).thenReturn(Future.successful(None))

      whenReady(getAtsYearList) { result =>
        result shouldBe data

        verify(auditService, times(1)).sendEvent(any[String], any[Map[String, String]], any[Option[String]])(any[Request[AnyRef]], any[HeaderCarrier])
        verify(dataCache, times(1)).fetchAndGetAtsListForSession(any[HeaderCarrier])
        verify(dataCache, times(1)).storeAtsListForSession(eqTo(data))(any[HeaderCarrier], any[ExecutionContext])
        verify(middleConnector, times(1)).connectToAtsList(any[SaUtr])(any[HeaderCarrier])
      }
    }

    // Should this be the case? (EDGE CASE)
    "Return the ats year list data for a user from the MS when they have an agentToken in their cache" in new TestService {

      when(dataCache.getAgentToken(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Some(agentToken))
      when(authUtils.checkUtr(any[String], any[Option[AgentToken]])(any[AuthenticatedRequest[_]])).thenReturn(false)

      whenReady(getAtsYearList) { result =>
        result shouldBe data

        verify(auditService, times(1)).sendEvent(any[String], any[Map[String, String]], any[Option[String]])(any[Request[AnyRef]], any[HeaderCarrier])
        verify(dataCache, times(1)).fetchAndGetAtsListForSession(any[HeaderCarrier])
        verify(dataCache, times(1)).storeAtsListForSession(eqTo(data))(any[HeaderCarrier], any[ExecutionContext])
        verify(middleConnector, times(1)).connectToAtsList(any[SaUtr])(any[HeaderCarrier])
      }
    }

    "Agent" should {

      val agentRequest = AuthenticatedRequest("userId", Some(Uar(testUar)), Some(SaUtr(testUtr)), None, None, None, None, FakeRequest())

      "Return the ats year list data for a user from the cache" in new TestService {

        when(accountUtils.isAgent(agentRequest)).thenReturn(true)

        whenReady(getAtsYearList(hc, agentRequest)) { result =>
          result shouldBe data

          verify(dataCache, times(1)).fetchAndGetAtsListForSession(any[HeaderCarrier])
          verify(dataCache, never()).storeAtsListForSession(eqTo(data))(any[HeaderCarrier], any[ExecutionContext])
          verify(middleConnector, never()).connectToAtsListOnBehalfOf(any[Uar], any[SaUtr])(any[HeaderCarrier])
        }
      }

      "Return the ats year list data for a user from the MS" in new TestService {

        when(dataCache.fetchAndGetAtsListForSession(any[HeaderCarrier])).thenReturn(Future.successful(None))

        whenReady(getAtsYearList(hc, agentRequest)) { result =>
          result shouldBe data

          verify(dataCache, times(1)).fetchAndGetAtsListForSession(any[HeaderCarrier])
          verify(dataCache, times(1)).storeAtsListForSession(eqTo(data))(any[HeaderCarrier], any[ExecutionContext])
          verify(middleConnector, times(1)).connectToAtsListOnBehalfOf(any[Uar], any[SaUtr])(any[HeaderCarrier])
        }
      }

      "Return the ats year list data for a user when the agent token doesn't match the user" in new TestService {

        when(authUtils.checkUtr(any[String], any[Option[AgentToken]])(any[AuthenticatedRequest[_]])).thenReturn(false)

        whenReady(getAtsYearList(hc, agentRequest)) { result =>
          result shouldBe data

          verify(dataCache, times(1)).fetchAndGetAtsListForSession(any[HeaderCarrier])
          verify(dataCache, times(1)).storeAtsListForSession(eqTo(data))(any[HeaderCarrier], any[ExecutionContext])
          verify(middleConnector, times(1)).connectToAtsListOnBehalfOf(any[Uar], any[SaUtr])(any[HeaderCarrier])
        }
      }

      "Return a failed future when an exception is thrown in the AuthUtils.getRequestedUtr method" in new TestService {

        when(authUtils.checkUtr(any[String], any[Option[AgentToken]])(any[AuthenticatedRequest[_]])).thenReturn(false)
        when(authUtils.getRequestedUtr(any[TaxIdentifier], any[Option[AgentToken]])).thenThrow(new AgentTokenException("Token is empty"))

        whenReady(getAtsYearList.failed) { exception =>
          exception shouldBe a [AgentTokenException]
          exception.getMessage shouldBe "Token is empty"

          verify(dataCache, times(1)).fetchAndGetAtsListForSession(any[HeaderCarrier])
          verify(dataCache, never()).storeAtsListForSession(eqTo(data))(any[HeaderCarrier], any[ExecutionContext])
          verify(middleConnector, never()).connectToAtsListOnBehalfOf(any[Uar], any[SaUtr])(any[HeaderCarrier])
        }
      }
    }
  }
}
