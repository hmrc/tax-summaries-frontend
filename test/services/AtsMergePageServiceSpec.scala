/*
 * Copyright 2021 HM Revenue & Customs
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
import connectors.DataCacheConnector
import controllers.auth.AuthenticatedRequest
import models._
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.{BAD_GATEWAY, INTERNAL_SERVER_ERROR}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.session
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.{SaUtr, Uar}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec
import utils.JsonUtil._
import utils.TestConstants.{testNino, _}
import view_models.{AtsList, AtsMergePageViewModel}

import scala.concurrent.{ExecutionContext, Future}

class AtsMergePageServiceSpec
    extends UnitSpec with GuiceOneAppPerSuite with ScalaFutures with MockitoSugar with BeforeAndAfterEach {

  val data = {
    val json = loadAndParseJsonWithDummyData("/summary_json_test.json")
    Json.fromJson[AtsData](json).get
  }

  val mockPayeAtsService: PayeAtsService = mock[PayeAtsService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockAtsListService: AtsListService = mock[AtsListService]
  val mockAtsYearListService: AtsYearListService = mock[AtsYearListService]
  val appConfig = app.injector.instanceOf[ApplicationConfig]

  override def beforeEach() =
    reset(mockDataCacheConnector)

  implicit val hc = new HeaderCarrier
  implicit lazy val ec = app.injector.instanceOf[ExecutionContext]
  implicit val request =
    AuthenticatedRequest(
      "userId",
      None,
      Some(SaUtr(testUtr)),
      Some(testNino),
      true,
      ConfidenceLevel.L50,
      fakeCredentials,
      FakeRequest())

  val agentToken = AgentToken(
    agentUar = testUar,
    clientUtr = testUtr,
    timestamp = 0
  )

  def sut =
    new AtsMergePageService(
      mockDataCacheConnector,
      mockPayeAtsService,
      mockAtsListService,
      mockAtsYearListService,
      appConfig)

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
    ConfidenceLevel.L50,
    fakeCredentials,
    FakeRequest("GET", controllers.routes.AtsMergePageController.onPageLoad + "/?ref=PORTAL&id=bxk2Z3Q84R0W2XSklMb7Kg")
  )

  "AtsMergePageService" when {

    "getSaAndPayeYearList is called" must {

      "return a AtsMergePageViewModel" when {

        "saData and payeData is successfully received" in {

          when(mockAtsYearListService.getAtsListData).thenReturn(Right(saDataResponse))
          when(mockPayeAtsService.getPayeTaxYearData(testNino, appConfig.taxYear - 1, appConfig.taxYear))
            .thenReturn(Right(payeDataResponse))

          val result = sut.getSaAndPayeYearList.futureValue
          result shouldBe Right(AtsMergePageViewModel(saDataResponse, payeDataResponse, appConfig))

          verify(mockDataCacheConnector, never())
            .storeAgentToken(any[String])(any[HeaderCarrier], any[ExecutionContext])
        }
      }

      "return INTERNAL_SERVER_ERROR" when {

        "saData returns error and paye returns success response" in {

          when(mockAtsYearListService.getAtsListData).thenReturn(Left(BAD_GATEWAY))
          when(mockPayeAtsService.getPayeTaxYearData(testNino, appConfig.taxYear - 1, appConfig.taxYear))
            .thenReturn(Right(payeDataResponse))

          val result = sut.getSaAndPayeYearList.futureValue
          result shouldBe Left(INTERNAL_SERVER_ERROR)
        }

        "saData returns success and paye returns error response" in {

          when(mockAtsYearListService.getAtsListData).thenReturn(Right(saDataResponse))
          when(mockPayeAtsService.getPayeTaxYearData(testNino, appConfig.taxYear - 1, appConfig.taxYear))
            .thenReturn(Left(HttpResponse(BAD_GATEWAY, "bad gateway")))

          val result = sut.getSaAndPayeYearList.futureValue
          result shouldBe Left(INTERNAL_SERVER_ERROR)
        }

        "saData and paye both return error response" in {

          when(mockAtsYearListService.getAtsListData).thenReturn(Left(BAD_GATEWAY))
          when(mockPayeAtsService.getPayeTaxYearData(testNino, appConfig.taxYear - 1, appConfig.taxYear))
            .thenReturn(Left(HttpResponse(BAD_GATEWAY, "bad gateway")))

          val result = sut.getSaAndPayeYearList.futureValue
          result shouldBe Left(INTERNAL_SERVER_ERROR)
        }
      }

    }
  }
}
