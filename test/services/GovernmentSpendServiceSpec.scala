/*
 * Copyright 2022 HM Revenue & Customs
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
import controllers.auth.AuthenticatedRequest
import models.{AtsData, SpendData}
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.FakeRequest
import services.atsData.AtsTestData
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.TestConstants._
import utils.{BaseSpec, GenericViewModel}
import view_models.{Amount, AtsList, GovernmentSpend}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class GovernmentSpendServiceSpec extends BaseSpec {

  val genericViewModel: GenericViewModel = AtsList(
    utr = "3000024376",
    forename = "forename",
    surname = "surname",
    yearList = List(2015)
  )

  override val taxYear = 2015

  val mockAtsService                       = mock[AtsService]
  val mockMiddleConnector: MiddleConnector = mock[MiddleConnector]

  implicit val hc = new HeaderCarrier

  val request = AuthenticatedRequest(
    "userId",
    None,
    Some(SaUtr(testUtr)),
    None,
    true,
    false,
    ConfidenceLevel.L50,
    fakeCredentials,
    FakeRequest("GET", "?taxYear=2015")
  )

  def sut = new GovernmentSpendService(mockAtsService, mockMiddleConnector) with MockitoSugar

  "GovernmentSpendService getGovernmentSpendData" must {

    "return a GenericViewModel when atsYearListService returns Success(taxYear)" in {
      when(mockAtsService.createModel(any(), any[Function1[AtsData, GenericViewModel]]())(any(), any()))
        .thenReturn(Future(genericViewModel))
      val result = Await.result(sut.getGovernmentSpendData(taxYear)(hc, request), 1500 millis)
      result mustEqual genericViewModel
    }
  }

  "GovernmentSpendService govSpend" must {

    "return a complete GovernmentSpend when given complete AtsData" in {
      val atsData = AtsTestData.govSpendingData
      val result  = sut.govSpend(atsData)

      result mustBe GovernmentSpend(
        2019,
        "1111111111",
        List("welfare" -> SpendData(Amount(100, "GBP"), 10)),
        "Mr",
        "John",
        "Smith",
        Amount(200, "GBP"),
        "0002",
        Amount(500, "GBP")
      )
    }

    "return a isScottishTaxPayer as true when incomeTaxStatus is 0002" in {
      val atsData = AtsTestData.govSpendingData
      val result  = sut.govSpend(atsData)

      result.isScottishTaxPayer mustBe true
    }

    "return a isScottishTaxPayer as false when incomeTaxStatus is not 0002" in {
      val atsData = AtsTestData.govSpendingDataForWelshUser
      val result  = sut.govSpend(atsData)

      result.isScottishTaxPayer mustBe false
    }
  }

  "GovernmentSpendService getGovernmentSpendDataV2" must {

    "return a government spend map" in {

      val expectedBody = Seq(("Environment", 5.5))

      when(mockMiddleConnector.connectToGovernmentSpend(any())(any())) thenReturn Future
        .successful(
          Right(
            HttpResponse(OK, Json.parse("""{"Environment":5.5}"""), Map("" -> List("")))
          )
        )

      val result = sut.getGovernmentSpendFigures(taxYear).value.futureValue

      result.value mustBe expectedBody
    }

    "sort data by percentage" in {

      val expectedBody = Seq(("Welfare", 23.4), ("Environment", 5.5), ("Culture", 2.3))

      when(mockMiddleConnector.connectToGovernmentSpend(any())(any())) thenReturn Future
        .successful(
          Right(
            HttpResponse(OK, Json.parse("""{"Environment":5.5, "Culture":2.3, "Welfare":23.4}"""), Map("" -> Seq("")))
          )
        )

      val result = sut.getGovernmentSpendFigures(taxYear).value.futureValue

      result.value mustBe expectedBody
    }

    "sort the categories in correct order for taxYear 18/19" in {

      val taxYear = 2018

      val expectedBody = Seq(("Welfare", 23.4), ("Environment", 5.5), ("Culture", 5.5))

      when(mockMiddleConnector.connectToGovernmentSpend(any())(any())) thenReturn Future
        .successful(
          Right(
            HttpResponse(OK, Json.parse("""{"Environment":5.5, "Culture":5.5, "Welfare":23.4}"""), Map("" -> List("")))
          )
        )

      val result = sut.getGovernmentSpendFigures(taxYear).value.futureValue

      result.value mustBe expectedBody
    }
  }
}
