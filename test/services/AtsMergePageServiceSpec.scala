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

import connectors.DataCacheConnector
import controllers.auth.AuthenticatedRequest
import models._
import org.mockito.ArgumentMatchers.any
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.{SaUtr, Uar}
import uk.gov.hmrc.http.HeaderCarrier
import utils.BaseSpec
import utils.JsonUtil._
import utils.TestConstants.{testNino, _}
import view_models.{AtsList, AtsMergePageViewModel}

import scala.concurrent.{ExecutionContext, Future}

class AtsMergePageServiceSpec extends BaseSpec with GuiceOneAppPerSuite with ScalaFutures with BeforeAndAfterEach {

  val data = {
    val json = loadAndParseJsonWithDummyData("/summary_json_test_2021.json")
    Json.fromJson[AtsData](json).get
  }

  val mockPayeAtsService: PayeAtsService         = mock[PayeAtsService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockAtsListService: AtsListService         = mock[AtsListService]

  override def beforeEach() =
    reset(mockDataCacheConnector)

  implicit val hc: HeaderCarrier = new HeaderCarrier

  val agentToken = AgentToken(
    agentUar = testUar,
    clientUtr = testUtr,
    timestamp = 0
  )

  def sut: AtsMergePageService =
    new AtsMergePageService(mockDataCacheConnector, mockPayeAtsService, mockAtsListService, appConfig)

  val saDataResponse: AtsList = AtsList(
    utr = testUtr,
    forename = "forename",
    surname = "surname",
    yearList = List(
      2014,
      2015
    )
  )

  val payeDataResponse = List(2018, 2019)

  val agentRequestWithQuery = AuthenticatedRequest(
    "userId",
    Some(Uar(testUar)),
    Some(SaUtr(testUtr)),
    Some(testNino),
    true,
    true,
    ConfidenceLevel.L50,
    fakeCredentials,
    FakeRequest(
      "GET",
      controllers.routes.AtsMergePageController.onPageLoad.toString + "/?ref=PORTAL&id=bxk2Z3Q84R0W2XSklMb7Kg"
    ),
    None
  )

  "AtsMergePageService" when {

    "getSaAndPayeYearList is called" must {

      "call data cache connector" when {

        "user is an agent" in {
          implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] =
            AuthenticatedRequest(
              "userId",
              Some(Uar("ref")),
              Some(SaUtr(testUtr)),
              Some(testNino),
              true,
              true,
              ConfidenceLevel.L50,
              fakeCredentials,
              FakeRequest("GET", "http://test.com?ref=PORTAL&id=something"),
              None
            )

          when(mockDataCacheConnector.storeAgentToken(any[String])(any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful("token"))
          when(mockAtsListService.createModel()).thenReturn(Future(Right(saDataResponse)))
          when(
            mockPayeAtsService
              .getPayeTaxYearData(testNino, appConfig.taxYear - appConfig.maxTaxYearsTobeDisplayed, appConfig.taxYear)
          )
            .thenReturn(Future(Right(payeDataResponse)))

          val result = sut.getSaAndPayeYearList.futureValue
          result mustBe Right(AtsMergePageViewModel(saDataResponse, payeDataResponse, appConfig, ConfidenceLevel.L50))

          verify(mockDataCacheConnector, times(1))
            .storeAgentToken(any[String])(any[HeaderCarrier], any[ExecutionContext])
        }
      }

      "return a AtsMergePageViewModel" when {

        "saData and payeData is successfully received" in {
          implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] =
            AuthenticatedRequest(
              "userId",
              None,
              Some(SaUtr(testUtr)),
              Some(testNino),
              true,
              true,
              ConfidenceLevel.L50,
              fakeCredentials,
              FakeRequest(),
              None
            )

          when(mockAtsListService.createModel()).thenReturn(Future(Right(saDataResponse)))
          when(
            mockPayeAtsService
              .getPayeTaxYearData(testNino, appConfig.taxYear - appConfig.maxTaxYearsTobeDisplayed, appConfig.taxYear)
          )
            .thenReturn(Future(Right(payeDataResponse)))

          val result = sut.getSaAndPayeYearList.futureValue
          result mustBe Right(AtsMergePageViewModel(saDataResponse, payeDataResponse, appConfig, ConfidenceLevel.L50))

          verify(mockDataCacheConnector, never)
            .storeAgentToken(any[String])(any[HeaderCarrier], any[ExecutionContext])
        }

        "saData is successfully received and nino is not present" in {
          implicit val requestNoNino =
            AuthenticatedRequest(
              "userId",
              None,
              Some(SaUtr(testUtr)),
              None,
              true,
              true,
              ConfidenceLevel.L50,
              fakeCredentials,
              FakeRequest(),
              None
            )
          when(mockAtsListService.createModel()).thenReturn(Future(Right(saDataResponse)))

          val result = sut.getSaAndPayeYearList.futureValue
          result mustBe Right(AtsMergePageViewModel(saDataResponse, List(), appConfig, ConfidenceLevel.L50))

          verify(mockDataCacheConnector, never)
            .storeAgentToken(any())(any(), any())
        }
      }

      "return INTERNAL_SERVER_ERROR" when {

        "saData returns error and paye returns success response" in {
          implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] =
            AuthenticatedRequest(
              "userId",
              None,
              Some(SaUtr(testUtr)),
              Some(testNino),
              true,
              true,
              ConfidenceLevel.L50,
              fakeCredentials,
              FakeRequest(),
              None
            )

          when(mockAtsListService.createModel()).thenReturn(Future(Left(AtsErrorResponse("bad gateway"))))
          when(
            mockPayeAtsService
              .getPayeTaxYearData(testNino, appConfig.taxYear - appConfig.maxTaxYearsTobeDisplayed, appConfig.taxYear)
          )
            .thenReturn(Future(Right(payeDataResponse)))

          val result = sut.getSaAndPayeYearList.futureValue
          result.left.value mustBe an[AtsErrorResponse]
        }

        "saData returns success and paye returns error response" in {
          implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] =
            AuthenticatedRequest(
              "userId",
              None,
              Some(SaUtr(testUtr)),
              Some(testNino),
              true,
              true,
              ConfidenceLevel.L50,
              fakeCredentials,
              FakeRequest(),
              None
            )
          when(mockAtsListService.createModel()).thenReturn(Future(Right(saDataResponse)))
          when(
            mockPayeAtsService
              .getPayeTaxYearData(testNino, appConfig.taxYear - appConfig.maxTaxYearsTobeDisplayed, appConfig.taxYear)
          )
            .thenReturn(Future(Left(AtsErrorResponse("bad gateway"))))

          val result = sut.getSaAndPayeYearList.futureValue
          result.swap.value mustBe an[AtsErrorResponse]
        }

        "saData and paye both return error response" in {
          implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] =
            AuthenticatedRequest(
              "userId",
              None,
              Some(SaUtr(testUtr)),
              Some(testNino),
              true,
              true,
              ConfidenceLevel.L50,
              fakeCredentials,
              FakeRequest(),
              None
            )
          when(mockAtsListService.createModel()).thenReturn(Future(Left(AtsErrorResponse("bad gateway"))))
          when(
            mockPayeAtsService
              .getPayeTaxYearData(testNino, appConfig.taxYear - appConfig.maxTaxYearsTobeDisplayed, appConfig.taxYear)
          )
            .thenReturn(Future(Left(AtsErrorResponse("bad gateway"))))

          val result = sut.getSaAndPayeYearList.futureValue
          result.left.value mustBe an[AtsErrorResponse]
        }
      }
    }
  }
}
