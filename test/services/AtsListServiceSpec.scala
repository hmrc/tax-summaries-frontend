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

package services

import config.ApplicationConfig
import connectors.MiddleConnector
import controllers.auth.requests
import controllers.auth.requests.AuthenticatedRequest
import models._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, reset, times, verify, when}
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import repository.TaxsAgentTokenSessionCacheRepository
import services.atsData.AtsTestData
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.{SaUtr, TaxIdentifier, Uar}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.DataKey
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utils.TestConstants._
import utils.{AgentTokenException, AuthorityUtils, BaseSpec}
import view_models.AtsList

import scala.concurrent.Future
import scala.io.{BufferedSource, Source}

class AtsListServiceSpec extends BaseSpec {

  val data: AtsListData = {
    val source: BufferedSource = Source.fromURL(getClass.getResource("/test_list_utr.json"))
    val sourceString: String   = source.mkString
    source.close()
    val json                   = Json.parse(sourceString)
    Json.fromJson[AtsListData](json).get
  }

  val mockMiddleConnector: MiddleConnector             = mock[MiddleConnector]
  private val mockTaxsAgentTokenSessionCacheRepository = mock[TaxsAgentTokenSessionCacheRepository]
  val mockAuditService: AuditService                   = mock[AuditService]
  val mockAuthUtils: AuthorityUtils                    = mock[AuthorityUtils]
  val mockAppConfig: ApplicationConfig                 = mock[ApplicationConfig]

  override def beforeEach(): Unit = {
    reset(mockMiddleConnector)
    reset(mockTaxsAgentTokenSessionCacheRepository)
    reset(mockAuditService)
    reset(mockAuthUtils)

    // By default we don't want to be an agent
    when(mockTaxsAgentTokenSessionCacheRepository.getFromSession[AgentToken](DataKey(any()))(any(), any()))
      .thenReturn(
        Future
          .successful(None)
      )

    when(mockMiddleConnector.connectToAtsList(any(), any(), any())(any())) thenReturn Future.successful(
      AtsSuccessResponseWithPayload[AtsListData](data)
    )

    when(
      mockMiddleConnector.connectToAtsListOnBehalfOf(any[SaUtr], any(), any())(any[HeaderCarrier])
    ) thenReturn Future
      .successful(AtsSuccessResponseWithPayload[AtsListData](data))

    when(mockAuditService.sendEvent(any(), any())(any())) thenReturn Future.successful(
      AuditResult.Success
    )

    when(mockAuthUtils.checkUtr(any[String], any[Option[AgentToken]])(any[AuthenticatedRequest[_]])).thenReturn(true)
    when(mockAuthUtils.getRequestedUtr(any[TaxIdentifier], any[Option[AgentToken]])) thenReturn SaUtr(testUtr)

    when(mockAppConfig.taxYear).thenReturn(2020)
    ()
  }

  implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] =
    requests.AuthenticatedRequest(
      "userId",
      None,
      Some(SaUtr(testUtr)),
      None,
      isAgentActive = false,
      ConfidenceLevel.L50,
      fakeCredentials,
      FakeRequest()
    )
  implicit val hc: HeaderCarrier                                     = new HeaderCarrier

  val agentToken: AgentToken = AgentToken(
    agentUar = testUar,
    clientUtr = testUtr,
    timestamp = 0
  )

  def sut: AtsListService =
    new AtsListService(
      mockAuditService,
      mockMiddleConnector,
      mockTaxsAgentTokenSessionCacheRepository,
      mockAuthUtils,
      appConfig
    )

  "createModel" must {

    "Return a ats list when received a success response from connector" in {

      when(mockMiddleConnector.connectToAtsList(any(), any(), any())(any())) thenReturn Future.successful(
        AtsSuccessResponseWithPayload[AtsListData](AtsTestData.atsListData)
      )

      whenReady(sut.createModel()) { result =>
        result mustBe Right(AtsList("1111111111", "John", "Smith", List(2022)))
      }

    }

    "Return an empty ats list when received a not found response from connector" in {

      when(mockMiddleConnector.connectToAtsList(any(), any(), any())(any())) thenReturn Future
        .successful(AtsNotFoundResponse("Not found"))

      whenReady(sut.createModel()) { result =>
        result mustBe Right(AtsList.empty)
      }

    }

    "Return the error status ats list when received an error response from connector" in {

      when(mockMiddleConnector.connectToAtsList(any(), any(), any())(any())) thenReturn Future
        .successful(AtsErrorResponse("INTERNAL_SERVER_ERROR"))

      val result = sut.createModel().futureValue.left.value
      result mustBe an[AtsErrorResponse]
    }
  }

  "getAtsYearList" must {

    "Return a ats list with 2020 year data" in {

      whenReady(sut.getAtsYearList) { result =>
        result.value.atsYearList.get.contains(2020) mustBe true
      }

    }

    "Return a ats list without 2020 year data" in {
      val dataMinus2020 = data copy (atsYearList = data.atsYearList.map(_.filter(_ != 2020)))

      when(mockAppConfig.taxYear).thenReturn(2023)

      when(mockMiddleConnector.connectToAtsList(any(), any(), any())(any())) thenReturn Future.successful(
        AtsSuccessResponseWithPayload[AtsListData](dataMinus2020)
      )

      whenReady(sut.getAtsYearList) { result =>
        result.value.atsYearList.get.contains(2020) mustBe false
      }

    }

    "Return a failed future when the call to the MS fails" in {

      when(mockMiddleConnector.connectToAtsList(any[SaUtr], any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.failed(new Exception("failed")))

      whenReady(sut.getAtsYearList.failed) { exception =>
        exception mustBe an[Exception]

        verify(mockMiddleConnector, times(1)).connectToAtsList(any[SaUtr], any(), any())(any[HeaderCarrier])
      }
    }

    "Return the ats year list data for a user from the MS" in {

      whenReady(sut.getAtsYearList) { result =>
        result mustBe Right(data)

        verify(mockAuditService, times(1)).sendEvent(any[String], any[Map[String, String]])(
          any()
        )
        verify(mockMiddleConnector, times(1)).connectToAtsList(any[SaUtr], any(), any())(any[HeaderCarrier])
      }
    }

    // must this be the case? (EDGE CASE)
    "Return the ats year list data for a user from the MS when they have an agentToken in their cache" in {

      when(mockAuthUtils.checkUtr(any[String], any[Option[AgentToken]])(any[AuthenticatedRequest[_]])).thenReturn(false)

      whenReady(sut.getAtsYearList) { result =>
        result mustBe Right(data)

        verify(mockAuditService, times(1)).sendEvent(any[String], any[Map[String, String]])(
          any()
        )
        verify(mockMiddleConnector, times(1)).connectToAtsList(any[SaUtr], any(), any())(any[HeaderCarrier])
      }
    }

    "Agent" must {

      val agentRequest =
        requests.AuthenticatedRequest(
          "userId",
          Some(Uar(testUar)),
          Some(SaUtr(testUtr)),
          None,
          isAgentActive = false,
          ConfidenceLevel.L50,
          fakeCredentials,
          FakeRequest()
        )

      "Return the ats year list data for a user from the MS" in {

        whenReady(sut.getAtsYearList(hc, agentRequest)) { result =>
          result mustBe Right(data)

          verify(mockMiddleConnector, times(1)).connectToAtsListOnBehalfOf(any[SaUtr], any(), any())(
            any[HeaderCarrier]
          )
        }
      }

      "Return the ats year list data for a user when the agent token doesn't match the user" in {

        when(mockAuthUtils.checkUtr(any[String], any[Option[AgentToken]])(any[AuthenticatedRequest[_]]))
          .thenReturn(false)

        whenReady(sut.getAtsYearList(hc, agentRequest)) { result =>
          result mustBe Right(data)

          verify(mockMiddleConnector, times(1)).connectToAtsListOnBehalfOf(any[SaUtr], any(), any())(
            any[HeaderCarrier]
          )
        }
      }

      "Return a left" when {

        "the connector returns a 404" in {

          when(mockMiddleConnector.connectToAtsList(any(), any(), any())(any())) thenReturn Future
            .successful(AtsNotFoundResponse("Not found"))

          val result = sut.getAtsYearList.futureValue.left.value
          result mustBe an[AtsNotFoundResponse]

          verify(mockAuditService).sendEvent(any[String], any[Map[String, String]])(
            any()
          )
          verify(mockMiddleConnector, times(1)).connectToAtsList(any[SaUtr], any(), any())(any[HeaderCarrier])
        }
      }

      "Return a left" when {
        "the connector returns a 500" in {

          when(mockMiddleConnector.connectToAtsList(any(), any(), any())(any())) thenReturn Future
            .successful(AtsErrorResponse("Something went wrong"))

          val result = sut.getAtsYearList.futureValue.left.value
          result mustBe an[AtsErrorResponse]

          verify(mockAuditService).sendEvent(any[String], any[Map[String, String]])(
            any()
          )

          verify(mockMiddleConnector, times(1)).connectToAtsList(any[SaUtr], any(), any())(any[HeaderCarrier])
        }
      }

      "Return a failed future when an exception is thrown in the AuthUtils.getRequestedUtr method" in {

        when(mockAuthUtils.checkUtr(any[String], any[Option[AgentToken]])(any[AuthenticatedRequest[_]]))
          .thenReturn(false)
        when(mockAuthUtils.getRequestedUtr(any[TaxIdentifier], any[Option[AgentToken]]))
          .thenThrow(AgentTokenException("Token is empty"))

        whenReady(sut.getAtsYearList.failed) { exception =>
          exception mustBe a[AgentTokenException]
          exception.getMessage mustBe "Token is empty"

          verify(mockMiddleConnector, never).connectToAtsListOnBehalfOf(any[SaUtr], any(), any())(
            any[HeaderCarrier]
          )
        }
      }
    }
  }
}
