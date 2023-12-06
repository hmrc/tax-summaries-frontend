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
import controllers.auth.AuthenticatedRequest
import models.{AtsData, SpendData}
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.MockitoSugar
import play.api.test.FakeRequest
import services.atsData.AtsTestData
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier
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

  implicit val hc: HeaderCarrier = new HeaderCarrier

  val request = AuthenticatedRequest(
    "userId",
    None,
    Some(SaUtr(testUtr)),
    None,
    true,
    false,
    ConfidenceLevel.L50,
    fakeCredentials,
    FakeRequest("GET", "?taxYear=2015"),
    None
  )

  def sut = new GovernmentSpendService(mockAtsService, mockMiddleConnector) with MockitoSugar

  "GovernmentSpendService getGovernmentSpendData" must {

    "return a GenericViewModel when atsYearListService returns Success(taxYear)" in {
      when(mockAtsService.createModel(meq(taxYear), any[Function1[AtsData, GenericViewModel]]())(any(), any()))
        .thenReturn(Future(genericViewModel))
      val result = Await.result(sut.getGovernmentSpendData(taxYear)(hc, request), 1500 millis)
      result mustEqual genericViewModel
    }
  }

  "GovernmentSpendService govSpend" must {
    "return a complete GovernmentSpend with sorted spending when given complete AtsData" in {
      val atsData = AtsTestData.govSpendingData
      val result  = sut.govSpend(atsData)

      result mustBe GovernmentSpend(
        2022,
        "1111111111",
        List(
          ("Health", SpendData(Amount(100, "GBP"), 10)),
          ("Welfare", SpendData(Amount(100, "GBP"), 10)),
          ("StatePensions", SpendData(Amount(100, "GBP"), 10)),
          ("Education", SpendData(Amount(100, "GBP"), 10)),
          ("NationalDebtInterest", SpendData(Amount(100, "GBP"), 10)),
          ("BusinessAndIndustry", SpendData(Amount(100, "GBP"), 10)),
          ("Defence", SpendData(Amount(100, "GBP"), 10)),
          ("Transport", SpendData(Amount(100, "GBP"), 10)),
          ("PublicOrderAndSafety", SpendData(Amount(100, "GBP"), 10)),
          ("GovernmentAdministration", SpendData(Amount(100, "GBP"), 10)),
          ("HousingAndUtilities", SpendData(Amount(100, "GBP"), 10)),
          ("Environment", SpendData(Amount(100, "GBP"), 10)),
          ("Culture", SpendData(Amount(100, "GBP"), 10)),
          ("OutstandingPaymentsToTheEU", SpendData(Amount(100, "GBP"), 10)),
          ("OverseasAid", SpendData(Amount(100, "GBP"), 10))
        ),
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
}
