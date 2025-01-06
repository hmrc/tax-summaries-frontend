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
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import repository.TaxsAgentTokenSessionCacheRepository
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.DataKey
import uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlag
import utils.BaseSpec
import utils.TestConstants._
import view_models.{AtsList, AtsMergePageViewModel}

import scala.concurrent.Future

class AtsMergePageServiceSpec extends BaseSpec with GuiceOneAppPerSuite with ScalaFutures with BeforeAndAfterEach {
  private val mockPayeAtsService: PayeAtsService       = mock[PayeAtsService]
  private val mockTaxsAgentTokenSessionCacheRepository = mock[TaxsAgentTokenSessionCacheRepository]
  private val mockAtsListService: AtsListService       = mock[AtsListService]
  private val cryptoService: CryptoService             = inject[CryptoService]

  override def beforeEach(): Unit = {
    reset(mockTaxsAgentTokenSessionCacheRepository, mockFeatureFlagService)
    when(mockFeatureFlagService.get(ArgumentMatchers.eq(SelfAssessmentServiceToggle)))
      .thenReturn(Future.successful(FeatureFlag(SelfAssessmentServiceToggle, isEnabled = true)))
    when(mockFeatureFlagService.get(ArgumentMatchers.eq(PAYEServiceToggle)))
      .thenReturn(Future.successful(FeatureFlag(PAYEServiceToggle, isEnabled = true)))
  }

  private implicit val hc: HeaderCarrier = new HeaderCarrier

  private def sut: AtsMergePageService =
    new AtsMergePageService(
      mockTaxsAgentTokenSessionCacheRepository,
      mockPayeAtsService,
      mockAtsListService,
      appConfig,
      cryptoService,
      mockFeatureFlagService
    )

  private val saDataResponse: AtsList = AtsList(
    utr = testUtr,
    forename = "forename",
    surname = "surname",
    yearList = List(2023, 2023)
  )

  private val payeDataResponse: List[Int] = List(2022, 2022)

  private def createRequest(optNino: Option[Nino] = Some(testNino)): AuthenticatedRequest[AnyContentAsEmpty.type] =
    requests.AuthenticatedRequest(
      userId = "userId",
      agentRef = None,
      saUtr = Some(SaUtr(testUtr)),
      nino = optNino,
      isAgentActive = true,
      confidenceLevel = ConfidenceLevel.L50,
      credentials = fakeCredentials,
      request = FakeRequest()
    )

  private val yearFrom = appConfig.taxYear - appConfig.maxTaxYearsTobeDisplayed + 1

  "AtsMergePageService" when {
    "getSaAndPayeYearList is called" must {
      "return a AtsMergePageViewModel" when {
        "saData and payeData is successfully received" in {
          implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] = createRequest()
          when(mockAtsListService.createModel()).thenReturn(Future(Right(saDataResponse)))
          when(mockPayeAtsService.getPayeTaxYearData(testNino, yearFrom, appConfig.taxYear))
            .thenReturn(Future(Right(payeDataResponse)))
          val result                                                         = sut.getSaAndPayeYearList.futureValue
          result mustBe Right(AtsMergePageViewModel(saDataResponse, payeDataResponse, appConfig, ConfidenceLevel.L50))
          verify(mockTaxsAgentTokenSessionCacheRepository, never)
            .putSession[AgentToken](DataKey(any()), any())(any(), any(), any())
        }

        "saData is successfully received and nino is not present" in {
          implicit val requestNoNino: AuthenticatedRequest[AnyContentAsEmpty.type] = createRequest(optNino = None)
          when(mockAtsListService.createModel()).thenReturn(Future(Right(saDataResponse)))

          val result = sut.getSaAndPayeYearList.futureValue
          result mustBe Right(AtsMergePageViewModel(saDataResponse, List(), appConfig, ConfidenceLevel.L50))

          verify(mockTaxsAgentTokenSessionCacheRepository, never)
            .putSession[AgentToken](DataKey(any()), any())(any(), any(), any())
        }
      }

      "return INTERNAL_SERVER_ERROR" when {
        "saData returns error and paye returns success response" in {
          implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] = createRequest()

          when(mockAtsListService.createModel()).thenReturn(Future(Left(AtsErrorResponse("bad gateway"))))
          when(mockPayeAtsService.getPayeTaxYearData(testNino, yearFrom, appConfig.taxYear))
            .thenReturn(Future(Right(payeDataResponse)))

          val result = sut.getSaAndPayeYearList.futureValue
          result.left.value mustBe an[AtsErrorResponse]
        }

        "saData returns success and paye returns error response" in {
          implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] = createRequest()
          when(mockAtsListService.createModel()).thenReturn(Future(Right(saDataResponse)))
          when(mockPayeAtsService.getPayeTaxYearData(testNino, yearFrom, appConfig.taxYear))
            .thenReturn(Future(Left(AtsErrorResponse("bad gateway"))))

          val result = sut.getSaAndPayeYearList.futureValue
          result.swap.value mustBe an[AtsErrorResponse]
        }

        "saData and paye both return error response" in {
          implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] = createRequest()
          when(mockAtsListService.createModel()).thenReturn(Future(Left(AtsErrorResponse("bad gateway"))))
          when(mockPayeAtsService.getPayeTaxYearData(testNino, yearFrom, appConfig.taxYear))
            .thenReturn(Future(Left(AtsErrorResponse("bad gateway"))))

          val result = sut.getSaAndPayeYearList.futureValue
          result.left.value mustBe an[AtsErrorResponse]
        }
      }
    }
  }
}
