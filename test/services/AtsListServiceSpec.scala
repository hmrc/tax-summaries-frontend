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
import models.{AgentToken, AtsListData}
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
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import utils.TestConstants._

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import uk.gov.hmrc.http.HeaderCarrier

class AtsListServiceSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  val data = {
    val source = Source.fromURL(getClass.getResource("/test_list_utr.json")).mkString
    val json = Json.parse(source)
    Json.fromJson[AtsListData](json).get
  }

  val mockMiddleConnector: MiddleConnector = mock[MiddleConnector]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockAuditService: AuditService = mock[AuditService]
  val mockAuthUtils: AuthorityUtils = mock[AuthorityUtils]
  val mockAccountUtils: AccountUtils = mock[AccountUtils]

  override def beforeEach() = {
    reset(mockMiddleConnector)
    reset(mockDataCacheConnector)
    reset(mockAuditService)
    reset(mockAuthUtils)
    reset(mockAccountUtils)

    when(mockDataCacheConnector.storeAtsTaxYearForSession(eqTo(2014))(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(Some(2014)))
    when(mockDataCacheConnector.storeAtsTaxYearForSession(eqTo(2015))(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(Some(2015)))
    when(mockDataCacheConnector.storeAtsListForSession(any[AtsListData])(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(Some(data)))

    when(mockDataCacheConnector.fetchAndGetAtsTaxYearForSession(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(Some(2014)))

    when(mockDataCacheConnector.fetchAndGetAtsListForSession(any[HeaderCarrier])).thenReturn(Future.successful(Some(data)))

    // By default we don't want to be an agent
    when(mockDataCacheConnector.getAgentToken(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(None))

    when(mockMiddleConnector.connectToAtsList(any[SaUtr])(any[HeaderCarrier])).thenReturn(Future.successful(data))
    when(mockMiddleConnector.connectToAtsListOnBehalfOf(any[Uar], any[SaUtr])(any[HeaderCarrier])).thenReturn(Future.successful(data))

    when(mockAuthUtils.checkUtr(any[String], any[Option[AgentToken]])(any[AuthenticatedRequest[_]])).thenReturn(true)

  }

  implicit val request = AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest())
  implicit val hc = new HeaderCarrier

  val agentToken = AgentToken(
    agentUar = testUar,
    clientUtr = testUtr,
    timestamp = 0
  )

  def sut = new AtsListService(mockAuditService, mockMiddleConnector, mockDataCacheConnector) {
    override lazy val authUtils: AuthorityUtils = mockAuthUtils
    override lazy val accountUtils: AccountUtils = mockAccountUtils
  }

  when(mockDataCacheConnector.storeAtsTaxYearForSession(eqTo(2014))(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(Some(2014)))

  "storeSelectedTaxYear" should {

    "Return a successful future upon success" in {

      val result = sut.storeSelectedTaxYear(2014)

      whenReady(result) { result =>
        result shouldBe 2014
      }
    }

    "Return a failed future when None is returned from the dataCache" in {

      when(mockDataCacheConnector.storeAtsTaxYearForSession(eqTo(2014))(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(None))

      val result = sut.storeSelectedTaxYear(2014)
      whenReady(result.failed) { exception =>
        exception shouldBe a [NoSuchElementException]
      }
    }

    "Return a failed future when the dataCache future has failed" in  {

      when(mockDataCacheConnector.storeAtsTaxYearForSession(eqTo(2014))(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.failed(new Exception("failed")))

      val result = sut.storeSelectedTaxYear(2014)

      whenReady(result.failed) { exception =>
        exception shouldBe an [Exception]
      }
    }
  }

  "fetchSelectedTaxYear" should {

    "Return a successful future upon success" in  {

      whenReady(sut.fetchSelectedTaxYear) { result =>
        result shouldBe 2014
      }
    }

    "Return a failed future when None is returned from the dataCache" in {

      when(mockDataCacheConnector.fetchAndGetAtsTaxYearForSession(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(None))

      whenReady(sut.fetchSelectedTaxYear.failed) { exception =>
        exception shouldBe a [NoSuchElementException]
      }
    }

    "Return a failed future when the dataCache future has failed" in  {

      when(mockDataCacheConnector.fetchAndGetAtsTaxYearForSession(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.failed(new Exception("failed")))

      whenReady(sut.fetchSelectedTaxYear.failed) { exception =>
        exception shouldBe an [Exception]
      }
    }
  }

  "getAtsYearList" should {

    "Return a failed future when the call to the dataCache fails (fetch)" in  {

      when(mockDataCacheConnector.fetchAndGetAtsListForSession(any[HeaderCarrier])).thenReturn(Future.failed(new Exception("failed")))

      whenReady(sut.getAtsYearList.failed) { exception =>
        exception shouldBe an [Exception]

        verify(mockDataCacheConnector, times(1)).fetchAndGetAtsListForSession(any[HeaderCarrier])
        verify(mockDataCacheConnector, never()).storeAtsListForSession(eqTo(data))(any[HeaderCarrier], any[ExecutionContext])
        verify(mockMiddleConnector, never()).connectToAtsList(any[SaUtr])(any[HeaderCarrier])
      }
    }

   "Return a failed future when the call to the dataCache fails (store)" in  {
      when(mockDataCacheConnector.fetchAndGetAtsListForSession(any[HeaderCarrier])).thenReturn(Future.successful(None))
      when(mockDataCacheConnector.storeAtsListForSession(any[AtsListData])(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.failed(new Exception("failed")))

      whenReady(sut.getAtsYearList.failed) { exception =>
        exception shouldBe an [Exception]

        verify(mockDataCacheConnector, times(1)).fetchAndGetAtsListForSession(any[HeaderCarrier])
        verify(mockDataCacheConnector, times(1)).storeAtsListForSession(eqTo(data))(any[HeaderCarrier], any[ExecutionContext])
        verify(mockMiddleConnector, times(1)).connectToAtsList(any[SaUtr])(any[HeaderCarrier])
      }
    }

    "Return a failed future when the call to the MS fails" in  {

      when(mockDataCacheConnector.fetchAndGetAtsListForSession(any[HeaderCarrier])).thenReturn(Future.successful(None))
      when(mockMiddleConnector.connectToAtsList(any[SaUtr])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new Exception("failed")))

      whenReady(sut.getAtsYearList.failed) { exception =>
        exception shouldBe an [Exception]

        verify(mockDataCacheConnector, times(1)).fetchAndGetAtsListForSession(any[HeaderCarrier])
        verify(mockDataCacheConnector, never()).storeAtsListForSession(eqTo(data))(any[HeaderCarrier], any[ExecutionContext])
        verify(mockMiddleConnector, times(1)).connectToAtsList(any[SaUtr])(any[HeaderCarrier])
      }
    }

    "User" should {

      "Return the ats year list data for a user from the cache" in  {

        when(mockAccountUtils.isAgent(request)).thenReturn(true)

        whenReady(sut.getAtsYearList) { result =>
          result shouldBe data
        }

        verify(mockAuditService, never()).sendEvent(any[String], any[Map[String, String]], any[Option[String]])(any[Request[AnyRef]], any[HeaderCarrier])
        verify(mockDataCacheConnector, times(1)).fetchAndGetAtsListForSession(any[HeaderCarrier])
        verify(mockDataCacheConnector, never()).storeAtsListForSession(eqTo(data))(any[HeaderCarrier], any[ExecutionContext])
        verify(mockMiddleConnector, never()).connectToAtsList(any[SaUtr])(any[HeaderCarrier])
      }
    }

    "Return the ats year list data for a user from the MS" in {

      when(mockDataCacheConnector.fetchAndGetAtsListForSession(any[HeaderCarrier])).thenReturn(Future.successful(None))

      whenReady(sut.getAtsYearList) { result =>
        result shouldBe data

        verify(mockAuditService, times(1)).sendEvent(any[String], any[Map[String, String]], any[Option[String]])(any[Request[AnyRef]], any[HeaderCarrier])
        verify(mockDataCacheConnector, times(1)).fetchAndGetAtsListForSession(any[HeaderCarrier])
        verify(mockDataCacheConnector, times(1)).storeAtsListForSession(eqTo(data))(any[HeaderCarrier], any[ExecutionContext])
        verify(mockMiddleConnector, times(1)).connectToAtsList(any[SaUtr])(any[HeaderCarrier])
      }
    }

    // Should this be the case? (EDGE CASE)
    "Return the ats year list data for a user from the MS when they have an agentToken in their cache" in {

      when(mockDataCacheConnector.getAgentToken(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Some(agentToken))
      when(mockAuthUtils.checkUtr(any[String], any[Option[AgentToken]])(any[AuthenticatedRequest[_]])).thenReturn(false)

      whenReady(sut.getAtsYearList) { result =>
        result shouldBe data

        verify(mockAuditService, times(1)).sendEvent(any[String], any[Map[String, String]], any[Option[String]])(any[Request[AnyRef]], any[HeaderCarrier])
        verify(mockDataCacheConnector, times(1)).fetchAndGetAtsListForSession(any[HeaderCarrier])
        verify(mockDataCacheConnector, times(1)).storeAtsListForSession(eqTo(data))(any[HeaderCarrier], any[ExecutionContext])
        verify(mockMiddleConnector, times(1)).connectToAtsList(any[SaUtr])(any[HeaderCarrier])
      }
    }

    "Agent" should {

      val agentRequest = AuthenticatedRequest("userId", Some(Uar(testUar)), Some(SaUtr(testUtr)), None, None, None, None, FakeRequest())

      "Return the ats year list data for a user from the cache" in {

        when(mockAccountUtils.isAgent(agentRequest)).thenReturn(true)

        whenReady(sut.getAtsYearList(hc, agentRequest)) { result =>
          result shouldBe data

          verify(mockDataCacheConnector, times(1)).fetchAndGetAtsListForSession(any[HeaderCarrier])
          verify(mockDataCacheConnector, never()).storeAtsListForSession(eqTo(data))(any[HeaderCarrier], any[ExecutionContext])
          verify(mockMiddleConnector, never()).connectToAtsListOnBehalfOf(any[Uar], any[SaUtr])(any[HeaderCarrier])
        }
      }

      "Return the ats year list data for a user from the MS" in {

        when(mockDataCacheConnector.fetchAndGetAtsListForSession(any[HeaderCarrier])).thenReturn(Future.successful(None))

        whenReady(sut.getAtsYearList(hc, agentRequest)) { result =>
          result shouldBe data

          verify(mockDataCacheConnector, times(1)).fetchAndGetAtsListForSession(any[HeaderCarrier])
          verify(mockDataCacheConnector, times(1)).storeAtsListForSession(eqTo(data))(any[HeaderCarrier], any[ExecutionContext])
          verify(mockMiddleConnector, times(1)).connectToAtsListOnBehalfOf(any[Uar], any[SaUtr])(any[HeaderCarrier])
        }
      }

      "Return the ats year list data for a user when the agent token doesn't match the user" in  {

        when(mockAuthUtils.checkUtr(any[String], any[Option[AgentToken]])(any[AuthenticatedRequest[_]])).thenReturn(false)

        whenReady(sut.getAtsYearList(hc, agentRequest)) { result =>
          result shouldBe data

          verify(mockDataCacheConnector, times(1)).fetchAndGetAtsListForSession(any[HeaderCarrier])
          verify(mockDataCacheConnector, times(1)).storeAtsListForSession(eqTo(data))(any[HeaderCarrier], any[ExecutionContext])
          verify(mockMiddleConnector, times(1)).connectToAtsListOnBehalfOf(any[Uar], any[SaUtr])(any[HeaderCarrier])
        }
      }

      "Return a failed future when an exception is thrown in the AuthUtils.getRequestedUtr method" in {

        when(mockAuthUtils.checkUtr(any[String], any[Option[AgentToken]])(any[AuthenticatedRequest[_]])).thenReturn(false)
        when(mockAuthUtils.getRequestedUtr(any[TaxIdentifier], any[Option[AgentToken]])).thenThrow(new AgentTokenException("Token is empty"))

        whenReady(sut.getAtsYearList.failed) { exception =>
          exception shouldBe a [AgentTokenException]
          exception.getMessage shouldBe "Token is empty"

          verify(mockDataCacheConnector, times(1)).fetchAndGetAtsListForSession(any[HeaderCarrier])
          verify(mockDataCacheConnector, never()).storeAtsListForSession(eqTo(data))(any[HeaderCarrier], any[ExecutionContext])
          verify(mockMiddleConnector, never()).connectToAtsListOnBehalfOf(any[Uar], any[SaUtr])(any[HeaderCarrier])
        }
      }
    }
  }
}
