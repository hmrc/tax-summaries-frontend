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

import controllers.auth.requests
import controllers.auth.requests.AuthenticatedRequest
import models._
import models.admin.{PAYEServiceToggle, SelfAssessmentServiceToggle}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import repository.TaxsAgentTokenSessionCacheRepository
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.{SaUtr, Uar}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.DataKey
import uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlag
import utils.BaseSpec
import utils.JsonUtil._
import utils.TestConstants._
import view_models.{AtsList, AtsMergePageViewModel}

import scala.concurrent.Future

class AtsMergePageServiceSpec extends BaseSpec with GuiceOneAppPerSuite with ScalaFutures with BeforeAndAfterEach {

  val data: AtsData = {
    val json = loadAndParseJsonWithDummyData("/summary_json_test_2021.json")
    Json.fromJson[AtsData](json).get
  }

  val mockPayeAtsService: PayeAtsService               = mock[PayeAtsService]
  private val mockTaxsAgentTokenSessionCacheRepository = mock[TaxsAgentTokenSessionCacheRepository]
  val mockAtsListService: AtsListService               = mock[AtsListService]
  val cryptoService: CryptoService                     = inject[CryptoService]

  override def beforeEach(): Unit = {
    reset(mockTaxsAgentTokenSessionCacheRepository, mockFeatureFlagService)
    when(mockFeatureFlagService.get(ArgumentMatchers.eq(SelfAssessmentServiceToggle)))
      .thenReturn(Future.successful(FeatureFlag(SelfAssessmentServiceToggle, isEnabled = true)))
    when(mockFeatureFlagService.get(ArgumentMatchers.eq(PAYEServiceToggle)))
      .thenReturn(Future.successful(FeatureFlag(PAYEServiceToggle, isEnabled = true)))
  }

  implicit val hc: HeaderCarrier = new HeaderCarrier

  val agentToken: AgentToken = AgentToken(
    agentUar = testUar,
    clientUtr = testUtr,
    timestamp = 0
  )

  def sut: AtsMergePageService =
    new AtsMergePageService(
      mockTaxsAgentTokenSessionCacheRepository,
      mockPayeAtsService,
      mockAtsListService,
      appConfig,
      cryptoService,
      mockFeatureFlagService
    )

  val saDataResponse: AtsList = AtsList(
    utr = testUtr,
    forename = "forename",
    surname = "surname",
    yearList = List(
      2023,
      2023
    )
  )

  val payeDataResponse: List[Int] = List(2022, 2022)

  val agentRequestWithQuery: AuthenticatedRequest[AnyContentAsEmpty.type] = requests.AuthenticatedRequest(
    userId = "userId",
    agentRef = Some(Uar(testUar)),
    saUtr = Some(SaUtr(testUtr)),
    nino = Some(testNino),
    isAgentActive = true,
    confidenceLevel = ConfidenceLevel.L50,
    credentials = fakeCredentials,
    request = FakeRequest(
      "GET",
      controllers.routes.AtsMergePageController.onPageLoad.toString + "/?ref=PORTAL&id=bxk2Z3Q84R0W2XSklMb7Kg"
    )
  )

  "AtsMergePageService" when {

    "getSaAndPayeYearList is called" must {
      "return a AtsMergePageViewModel" when {

        "saData and payeData is successfully received" in {
          implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] =
            requests.AuthenticatedRequest(
              userId = "userId",
              agentRef = None,
              saUtr = Some(SaUtr(testUtr)),
              nino = Some(testNino),
              isAgentActive = true,
              confidenceLevel = ConfidenceLevel.L50,
              credentials = fakeCredentials,
              request = FakeRequest()
            )

          when(mockAtsListService.createModel()).thenReturn(Future(Right(saDataResponse)))
          when(
            mockPayeAtsService
              .getPayeTaxYearData(
                testNino,
                appConfig.taxYear - appConfig.maxTaxYearsTobeDisplayed + 1,
                appConfig.taxYear
              )
          )
            .thenReturn(Future(Right(payeDataResponse)))

          val result = sut.getSaAndPayeYearList.futureValue
          result mustBe Right(AtsMergePageViewModel(saDataResponse, payeDataResponse, appConfig, ConfidenceLevel.L50))

          verify(mockTaxsAgentTokenSessionCacheRepository, never)
            .putSession[AgentToken](DataKey(any()), any())(any(), any(), any())

        }

        "saData is successfully received and nino is not present" in {
          implicit val requestNoNino: AuthenticatedRequest[AnyContentAsEmpty.type] =
            requests.AuthenticatedRequest(
              userId = "userId",
              agentRef = None,
              saUtr = Some(SaUtr(testUtr)),
              nino = None,
              isAgentActive = true,
              confidenceLevel = ConfidenceLevel.L50,
              credentials = fakeCredentials,
              request = FakeRequest()
            )
          when(mockAtsListService.createModel()).thenReturn(Future(Right(saDataResponse)))

          val result = sut.getSaAndPayeYearList.futureValue
          result mustBe Right(AtsMergePageViewModel(saDataResponse, List(), appConfig, ConfidenceLevel.L50))

          verify(mockTaxsAgentTokenSessionCacheRepository, never)
            .putSession[AgentToken](DataKey(any()), any())(any(), any(), any())
        }
      }

      "return INTERNAL_SERVER_ERROR" when {

        "saData returns error and paye returns success response" in {
          implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] =
            requests.AuthenticatedRequest(
              userId = "userId",
              agentRef = None,
              saUtr = Some(SaUtr(testUtr)),
              nino = Some(testNino),
              isAgentActive = true,
              confidenceLevel = ConfidenceLevel.L50,
              credentials = fakeCredentials,
              request = FakeRequest()
            )

          when(mockAtsListService.createModel()).thenReturn(Future(Left(AtsErrorResponse("bad gateway"))))
          when(
            mockPayeAtsService
              .getPayeTaxYearData(
                testNino,
                appConfig.taxYear - appConfig.maxTaxYearsTobeDisplayed + 1,
                appConfig.taxYear
              )
          )
            .thenReturn(Future(Right(payeDataResponse)))

          val result = sut.getSaAndPayeYearList.futureValue
          result.left.value mustBe an[AtsErrorResponse]
        }

        "saData returns success and paye returns error response" in {
          implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] =
            requests.AuthenticatedRequest(
              userId = "userId",
              agentRef = None,
              saUtr = Some(SaUtr(testUtr)),
              nino = Some(testNino),
              isAgentActive = true,
              confidenceLevel = ConfidenceLevel.L50,
              credentials = fakeCredentials,
              request = FakeRequest()
            )
          when(mockAtsListService.createModel()).thenReturn(Future(Right(saDataResponse)))
          when(
            mockPayeAtsService
              .getPayeTaxYearData(
                testNino,
                appConfig.taxYear - appConfig.maxTaxYearsTobeDisplayed + 1,
                appConfig.taxYear
              )
          )
            .thenReturn(Future(Left(AtsErrorResponse("bad gateway"))))

          val result = sut.getSaAndPayeYearList.futureValue
          result.swap.value mustBe an[AtsErrorResponse]
        }

        "saData and paye both return error response" in {
          implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] =
            requests.AuthenticatedRequest(
              userId = "userId",
              agentRef = None,
              saUtr = Some(SaUtr(testUtr)),
              nino = Some(testNino),
              isAgentActive = true,
              confidenceLevel = ConfidenceLevel.L50,
              credentials = fakeCredentials,
              request = FakeRequest()
            )
          when(mockAtsListService.createModel()).thenReturn(Future(Left(AtsErrorResponse("bad gateway"))))
          when(
            mockPayeAtsService
              .getPayeTaxYearData(
                testNino,
                appConfig.taxYear - appConfig.maxTaxYearsTobeDisplayed + 1,
                appConfig.taxYear
              )
          )
            .thenReturn(Future(Left(AtsErrorResponse("bad gateway"))))

          val result = sut.getSaAndPayeYearList.futureValue
          result.left.value mustBe an[AtsErrorResponse]
        }
      }
    }
  }
}
