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

package services

import connectors.MiddleConnector
import models.requests
import models.*
import models.requests.AuthenticatedRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, reset, verify, when}
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import repository.TaxsAgentTokenSessionCacheRepository
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.{SaUtr, Uar}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.DataKey
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import utils.JsonUtil.*
import utils.TestConstants.*
import utils.{AccountUtils, AuthorityUtils, BaseSpec, GenericViewModel}
import view_models.{ATSUnavailableViewModel, Amount, NoATSViewModel}

import scala.concurrent.Future

class AtsServiceSpec extends BaseSpec {
  private val data: AtsData =
    Json
      .fromJson[AtsData](
        Json.parse(
          atsData(currentTaxYearSA)
        )
      )
      .get

  val mockMiddleConnector: MiddleConnector             = mock[MiddleConnector]
  private val mockTaxsAgentTokenSessionCacheRepository = mock[TaxsAgentTokenSessionCacheRepository]
  val mockAuditService: AuditService                   = mock[AuditService]
  val mockAuthUtils: AuthorityUtils                    = mock[AuthorityUtils]
  val mockAccountUtils: AccountUtils                   = mock[AccountUtils]

  override def beforeEach(): Unit = {
    reset(mockMiddleConnector)
    reset(mockTaxsAgentTokenSessionCacheRepository)
    reset(mockAuditService)
    reset(mockAuthUtils)
    reset(mockAccountUtils)

  }

  implicit val hc: HeaderCarrier = new HeaderCarrier

  implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] =
    requests.AuthenticatedRequest(
      userId = "userId",
      agentRef = None,
      saUtr = Some(SaUtr(testUtr)),
      nino = None,
      isAgentActive = false,
      confidenceLevel = ConfidenceLevel.L50,
      credentials = fakeCredentials,
      request = FakeRequest()
    )

  val agentToken: AgentToken = AgentToken(
    agentUar = testUar,
    clientUtr = testUtr,
    timestamp = 0
  )

  def sut: AtsService =
    new AtsService(
      mockMiddleConnector,
      mockTaxsAgentTokenSessionCacheRepository,
      appConfig,
      mockAuditService,
      mockAuthUtils
    ) {
      override val accountUtils: AccountUtils = mockAccountUtils
    }

  case class FakeViewModel(str: String) extends GenericViewModel

  def converter(atsData: AtsData): FakeViewModel = FakeViewModel(atsData.toString)

  "AtsService" when {

    "createModel is called" must {

      "return an instance of the desired view model" when {

        "connector returns a success response with valid payload" which {

          "a user who is not an agent" that {
            "getting data from mockMiddleConnector" in {
              when(mockAccountUtils.isAgent(any())) thenReturn false

              when(mockMiddleConnector.connectToAts(any(), any())(any())) thenReturn
                Future.successful(AtsSuccessResponseWithPayload[AtsData](data))

              when(mockTaxsAgentTokenSessionCacheRepository.getFromSession[AgentToken](DataKey(any()))(any(), any()))
                .thenReturn(
                  Future
                    .successful(Some(agentToken))
                )

              when(mockAuditService.sendEvent(any(), any())(any())) thenReturn Future.successful(Success)

              sut.createModel(currentTaxYearSA, converter).futureValue mustBe FakeViewModel(data.toString)

              verify(mockAuditService).sendEvent(any(), any())(any())
            }
          }

          "a user who is an agent" that {

            "getting data from middleconnector" in {

              when(mockAccountUtils.isAgent(any())) thenReturn true

              when(mockAuthUtils.getRequestedUtr(any(), any())) thenReturn SaUtr(testNonMatchingUtr)

              when(mockAccountUtils.getAccount(any())) thenReturn Uar(testUar)

              when(mockTaxsAgentTokenSessionCacheRepository.getFromSession[AgentToken](DataKey(any()))(any(), any()))
                .thenReturn(
                  Future
                    .successful(Some(agentToken))
                )

              when(mockAuditService.sendEvent(any(), any())(any())) thenReturn Future.successful(Success)

              when(
                mockMiddleConnector
                  .connectToAtsOnBehalfOf(any(), any())(any())
              ) thenReturn Future.successful(AtsSuccessResponseWithPayload[AtsData](data))

              implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] =
                requests.AuthenticatedRequest(
                  userId = "userId",
                  agentRef = Some(Uar(testUar)),
                  saUtr = Some(SaUtr(testUtr)),
                  nino = None,
                  isAgentActive = false,
                  confidenceLevel = ConfidenceLevel.L50,
                  credentials = fakeCredentials,
                  request = FakeRequest()
                )

              sut.createModel(currentTaxYearSA, converter).futureValue mustBe FakeViewModel(data.toString)

              verify(mockAuditService).sendEvent(any(), any())(any())
            }
          }
        }
      }

      "return a NoATSViewModel" when {

        "the connector returns a NoATSViewModel" in {

          when(mockAccountUtils.isAgent(any())) thenReturn false

          when(mockMiddleConnector.connectToAts(any(), any())(any())) thenReturn Future
            .successful(AtsNotFoundResponse("Not found"))

          sut.createModel(currentTaxYearSA, converter).futureValue mustBe a[NoATSViewModel]

          verify(mockAuditService, never).sendEvent(any(), any())(any())
        }

        "getting data from mockMiddleConnector where no tax liability" in {
          val dataNoTaxLiability: AtsData = data copy (taxLiability = None)
          when(mockAccountUtils.isAgent(any())) thenReturn false

          when(mockMiddleConnector.connectToAts(any(), any())(any())) thenReturn
            Future.successful(AtsSuccessResponseWithPayload[AtsData](dataNoTaxLiability))

          when(mockTaxsAgentTokenSessionCacheRepository.getFromSession[AgentToken](DataKey(any()))(any(), any()))
            .thenReturn(
              Future
                .successful(Some(agentToken))
            )

          when(mockAuditService.sendEvent(any(), any())(any())) thenReturn Future.successful(Success)

          sut.createModel(currentTaxYearSA, converter).futureValue mustBe a[NoATSViewModel]
        }

        "getting data from mockMiddleConnector where tax liability is negative value" in {
          val dataNegTaxLiability: AtsData = data copy (taxLiability = Some(Amount(BigDecimal(-100), "GBP")))
          when(mockAccountUtils.isAgent(any())) thenReturn false

          when(mockMiddleConnector.connectToAts(any(), any())(any())) thenReturn
            Future.successful(AtsSuccessResponseWithPayload[AtsData](dataNegTaxLiability))

          when(mockTaxsAgentTokenSessionCacheRepository.getFromSession[AgentToken](DataKey(any()))(any(), any()))
            .thenReturn(
              Future
                .successful(Some(agentToken))
            )

          when(mockAuditService.sendEvent(any(), any())(any())) thenReturn Future.successful(Success)

          sut.createModel(currentTaxYearSA, converter).futureValue mustBe a[NoATSViewModel]
        }

      }

      "return an AtsUnavailableViewModel" when {

        "the connector returns an AtsErrorResponse" in {

          when(mockAccountUtils.isAgent(any())) thenReturn false

          when(mockMiddleConnector.connectToAts(any(), any())(any())) thenReturn Future(
            AtsErrorResponse("Something went wrong")
          )

          sut.createModel(currentTaxYearSA, converter).futureValue mustBe a[ATSUnavailableViewModel]

          verify(mockAuditService, never).sendEvent(any(), any())(any())
        }

        "there is any other error in the AtsData" in {

          val dataWithError = data.copy(errors = Some(IncomingAtsError("Random error")))

          when(mockAccountUtils.isAgent(any())) thenReturn false

          when(mockMiddleConnector.connectToAts(any(), any())(any())) thenReturn Future
            .successful(AtsSuccessResponseWithPayload(dataWithError))

          sut.createModel(currentTaxYearSA, converter).futureValue mustBe a[ATSUnavailableViewModel]

          verify(mockAuditService, never).sendEvent(any(), any())(any())
        }
      }
    }
  }
}
