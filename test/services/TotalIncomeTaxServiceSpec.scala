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

import controllers.auth.AuthenticatedRequest
import models.AtsData
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import play.api.test.FakeRequest
import services.atsData.AtsTestData
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestConstants._
import utils.{BaseSpec, GenericViewModel}
import view_models._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class TotalIncomeTaxServiceSpec extends BaseSpec {

  val genericViewModel: GenericViewModel = AtsList(
    utr = "3000024376",
    forename = "forename",
    surname = "surname",
    yearList = List(2015)
  )

  implicit val hc = HeaderCarrier()

  val mockAtsService = mock[AtsService]

  def sut = new TotalIncomeTaxService(mockAtsService) with MockitoSugar {
    val taxYear = 2015
  }

  "TotalIncomeTaxService getIncomeData" must {

    "return a GenericViewModel when TaxYearUtil.extractTaxYear returns a taxYear" in {
      when(
        mockAtsService.createModel(any(), any())(
          any(),
          any()
        )
      ).thenReturn(Future(genericViewModel))
      lazy val request = AuthenticatedRequest(
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
      val result       = Await.result(sut.getIncomeData(sut.taxYear)(hc, request), 1500 millis)
      result mustEqual genericViewModel
    }
  }

  "TotalIncomeTaxService.totalIncomeConverter" must {

    val savingsTax = SavingsTax(
      Amount.gbp(2900),
      Amount.gbp(3000),
      Amount.gbp(3100),
      Amount.gbp(3200),
      Amount.gbp(3300),
      Amount.gbp(3400)
    )

    val savingsRates = SavingsRates(
      Rate("130%"),
      Rate("140%"),
      Rate("150%")
    )

    "return complete TotalIncomeTax data when given complete AtsData for scottish tax payer" in {
      val incomeData: AtsData    = AtsTestData.totalIncomeTaxData
      val result: TotalIncomeTax = sut.totalIncomeConverter(incomeData)

      val scottishTax = ScottishTax(
        Amount.gbp(1800),
        Amount.gbp(1900),
        Amount.gbp(2000),
        Amount.gbp(2100),
        Amount.gbp(2200),
        Amount.gbp(2300),
        Amount.gbp(2400),
        Amount.gbp(2500),
        Amount.gbp(2600),
        Amount.gbp(2700),
        Amount.gbp(2800)
      )

      val scottishRates = ScottishRates(
        Rate("80%"),
        Rate("90%"),
        Rate("100%"),
        Rate("110%"),
        Rate("120%")
      )

      result mustEqual TotalIncomeTax(
        2019,
        "1111111111",
        Amount(100, "GBP"),
        Amount(200, "GBP"),
        Amount(300, "GBP"),
        Amount(400, "GBP"),
        Amount(500, "GBP"),
        Amount(600, "GBP"),
        Amount(700, "GBP"),
        Amount(800, "GBP"),
        Amount(900, "GBP"),
        Amount(1000, "GBP"),
        Amount(1100, "GBP"),
        Amount(1200, "GBP"),
        Amount(1300, "GBP"),
        Amount(1400, "GBP"),
        Amount(1500, "GBP"),
        Amount(1600, "GBP"),
        Amount(-1700, "GBP"),
        scottishTax,
        Amount.gbp(3500),
        Amount.gbp(3600),
        Amount.empty,
        savingsTax,
        "0002",
        Rate("10%"),
        Rate("20%"),
        Rate("30%"),
        Rate("40%"),
        Rate("50%"),
        Rate("60%"),
        Rate("70%"),
        scottishRates,
        savingsRates,
        "Mr",
        "John",
        "Smith"
      )
    }

    "return complete TotalIncomeTax data when given complete AtsData for welsh tax payer" in {
      val incomeData: AtsData    = AtsTestData.incomeTaxDataForWelshTaxPayer
      val result: TotalIncomeTax = sut.totalIncomeConverter(incomeData)

      result mustEqual TotalIncomeTax(
        2019,
        "1111111111",
        Amount(100, "GBP"),
        Amount(200, "GBP"),
        Amount(300, "GBP"),
        Amount(400, "GBP"),
        Amount(500, "GBP"),
        Amount(600, "GBP"),
        Amount(700, "GBP"),
        Amount(800, "GBP"),
        Amount(900, "GBP"),
        Amount(1000, "GBP"),
        Amount(1100, "GBP"),
        Amount(1200, "GBP"),
        Amount(1300, "GBP"),
        Amount(1400, "GBP"),
        Amount(1500, "GBP"),
        Amount(1600, "GBP"),
        Amount(1700, "GBP"),
        ScottishTax.empty,
        Amount.gbp(3500),
        Amount.empty,
        Amount.gbp(2600),
        savingsTax,
        "0003",
        Rate("10%"),
        Rate("20%"),
        Rate("30%"),
        Rate("40%"),
        Rate("50%"),
        Rate("60%"),
        Rate("70%"),
        ScottishRates.empty,
        savingsRates,
        "Mr",
        "John",
        "Smith"
      )
    }

    "return a isScottishTaxPayer as true and isWelshTaxPayer as false when incomeTaxStatus is 0002" in {
      val incomeData: AtsData    = AtsTestData.totalIncomeTaxData
      val result: TotalIncomeTax = sut.totalIncomeConverter(incomeData)
      result.isScottishTaxPayer mustEqual true
      result.isWelshTaxPayer mustEqual false
    }

    "return a isScottishTaxPayer as false and isWelshTaxPayer as true when incomeTaxStatus is 0003" in {
      val incomeData: AtsData    = AtsTestData.incomeTaxDataForWelshTaxPayer
      val result: TotalIncomeTax = sut.totalIncomeConverter(incomeData)
      result.isScottishTaxPayer mustEqual false
      result.isWelshTaxPayer mustEqual true
    }

  }
}
