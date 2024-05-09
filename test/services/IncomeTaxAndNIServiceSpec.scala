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

class IncomeTaxAndNIServiceSpec extends BaseSpec {

  override val taxYear = 2023

  val genericViewModel: GenericViewModel = AtsList(
    utr = "3000024376",
    forename = "forename",
    surname = "surname",
    yearList = List(taxYear)
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockAtsService: AtsService = mock[AtsService]

  def sut: IncomeTaxAndNIService = new IncomeTaxAndNIService(mockAtsService) with MockitoSugar

  "TotalIncomeTaxService getIncomeData" must {

    "return a GenericViewModel when TaxYearUtil.extractTaxYear returns a taxYear" in {
      when(
        mockAtsService.createModel(any(), any())(
          any(),
          any()
        )
      ).thenReturn(Future(genericViewModel))
      lazy val request = requests.AuthenticatedRequest(
        userId = "userId",
        agentRef = None,
        saUtr = Some(SaUtr(testUtr)),
        nino = None,
        isAgentActive = false,
        confidenceLevel = ConfidenceLevel.L50,
        credentials = fakeCredentials,
        request = FakeRequest("GET", "?taxYear=2023")
      )
      val result       = Await.result(sut.getIncomeAndNIData(taxYear)(hc, request), 1500 millis)
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

    "return complete IncomeTaxAndNI data when given complete AtsData for scottish tax payer" in {
      val incomeData: AtsData    = AtsTestData.totalIncomeTaxData
      val result: IncomeTaxAndNI = sut.totalIncomeConverter(incomeData)

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

      result mustEqual IncomeTaxAndNI(
        year = 2022,
        utr = "1111111111",
        employeeNicAmount = Amount(100, "GBP"),
        totalIncomeTaxAndNics = Amount(200, "GBP"),
        yourTotalTax = Amount(300, "GBP"),
        totalTaxFree = Amount(400, "GBP"),
        totalTaxFreeAllowance = Amount(400, "GBP"),
        yourIncomeBeforeTax = Amount(500, "GBP"),
        totalIncomeTaxAmount = Amount(600, "GBP"),
        totalCapitalGainsTax = Amount(700, "GBP"),
        taxableGains = Amount(800, "GBP"),
        cgTaxPerCurrencyUnit = Amount(900, "GBP"),
        nicsAndTaxPerCurrencyUnit = Amount(1000, "GBP"),
        totalCgTaxRate = Rate("10.00%"),
        nicsAndTaxRate = Rate("20.00%"),
        startingRateForSavings = Amount(100, "GBP"),
        startingRateForSavingsAmount = Amount(200, "GBP"),
        basicRateIncomeTax = Amount(300, "GBP"),
        basicRateIncomeTaxAmount = Amount(400, "GBP"),
        higherRateIncomeTax = Amount(500, "GBP"),
        higherRateIncomeTaxAmount = Amount(600, "GBP"),
        additionalRateIncomeTax = Amount(700, "GBP"),
        additionalRateIncomeTaxAmount = Amount(800, "GBP"),
        ordinaryRate = Amount(900, "GBP"),
        ordinaryRateAmount = Amount(1000, "GBP"),
        upperRate = Amount(1100, "GBP"),
        upperRateAmount = Amount(1200, "GBP"),
        additionalRate = Amount(1300, "GBP"),
        additionalRateAmount = Amount(1400, "GBP"),
        otherAdjustmentsIncreasing = Amount(1500, "GBP"),
        marriageAllowanceReceivedAmount = Amount(1600, "GBP"),
        otherAdjustmentsReducing = Amount(-1700, "GBP"),
        scottishTax = scottishTax,
        totalIncomeTax = Amount.gbp(3500),
        scottishIncomeTax = Amount.gbp(3600),
        welshIncomeTax = Amount.empty,
        savingsTax = savingsTax,
        incomeTaxStatus = "0002",
        startingRateForSavingsRateRate = Rate("10%"),
        basicRateIncomeTaxRateRate = Rate("20%"),
        higherRateIncomeTaxRateRate = Rate("30%"),
        additionalRateIncomeTaxRateRate = Rate("40%"),
        ordinaryRateTaxRateRate = Rate("50%"),
        upperRateRateRate = Rate("60%"),
        additionalRateRateRate = Rate("70%"),
        scottishRates = scottishRates,
        savingsRates = savingsRates,
        title = "Mr",
        forename = "John",
        surname = "Smith"
      )
    }

    "return complete IncomeTaxAndNI data when given complete AtsData for welsh tax payer" in {
      val incomeData: AtsData    = AtsTestData.incomeTaxDataForWelshTaxPayer
      val result: IncomeTaxAndNI = sut.totalIncomeConverter(incomeData)

      result mustEqual IncomeTaxAndNI(
        year = 2022,
        utr = "1111111111",
        employeeNicAmount = Amount(100, "GBP"),
        totalIncomeTaxAndNics = Amount(200, "GBP"),
        yourTotalTax = Amount(300, "GBP"),
        totalTaxFree = Amount(400, "GBP"),
        totalTaxFreeAllowance = Amount(400, "GBP"),
        yourIncomeBeforeTax = Amount(500, "GBP"),
        totalIncomeTaxAmount = Amount(600, "GBP"),
        totalCapitalGainsTax = Amount(700, "GBP"),
        taxableGains = Amount(800, "GBP"),
        cgTaxPerCurrencyUnit = Amount(900, "GBP"),
        nicsAndTaxPerCurrencyUnit = Amount(1000, "GBP"),
        totalCgTaxRate = Rate("10.00%"),
        nicsAndTaxRate = Rate("20.00%"),
        startingRateForSavings = Amount(100, "GBP"),
        startingRateForSavingsAmount = Amount(200, "GBP"),
        basicRateIncomeTax = Amount(300, "GBP"),
        basicRateIncomeTaxAmount = Amount(400, "GBP"),
        higherRateIncomeTax = Amount(500, "GBP"),
        higherRateIncomeTaxAmount = Amount(600, "GBP"),
        additionalRateIncomeTax = Amount(700, "GBP"),
        additionalRateIncomeTaxAmount = Amount(800, "GBP"),
        ordinaryRate = Amount(900, "GBP"),
        ordinaryRateAmount = Amount(1000, "GBP"),
        upperRate = Amount(1100, "GBP"),
        upperRateAmount = Amount(1200, "GBP"),
        additionalRate = Amount(1300, "GBP"),
        additionalRateAmount = Amount(1400, "GBP"),
        otherAdjustmentsIncreasing = Amount(1500, "GBP"),
        marriageAllowanceReceivedAmount = Amount(1600, "GBP"),
        otherAdjustmentsReducing = Amount(1700, "GBP"),
        scottishTax = ScottishTax.empty,
        totalIncomeTax = Amount.gbp(3500),
        scottishIncomeTax = Amount.empty,
        welshIncomeTax = Amount.gbp(2600),
        savingsTax = savingsTax,
        incomeTaxStatus = "0003",
        startingRateForSavingsRateRate = Rate("10%"),
        basicRateIncomeTaxRateRate = Rate("20%"),
        higherRateIncomeTaxRateRate = Rate("30%"),
        additionalRateIncomeTaxRateRate = Rate("40%"),
        ordinaryRateTaxRateRate = Rate("50%"),
        upperRateRateRate = Rate("60%"),
        additionalRateRateRate = Rate("70%"),
        scottishRates = ScottishRates.empty,
        savingsRates = savingsRates,
        title = "Mr",
        forename = "John",
        surname = "Smith"
      )
    }

    "return IncomeTaxAndNI data with empty values when summary data missing" in {
      val incomeData: AtsData    = AtsTestData.incomeTaxDataForWelshTaxPayer.copy(summary_data = None)
      val result: IncomeTaxAndNI = sut.totalIncomeConverter(incomeData)

      result mustEqual IncomeTaxAndNI(
        year = 2022,
        utr = "1111111111",
        employeeNicAmount = Amount.empty,
        totalIncomeTaxAndNics = Amount.empty,
        yourTotalTax = Amount.empty,
        totalTaxFree = Amount.empty,
        totalTaxFreeAllowance = Amount.empty,
        yourIncomeBeforeTax = Amount.empty,
        totalIncomeTaxAmount = Amount.empty,
        totalCapitalGainsTax = Amount.empty,
        taxableGains = Amount.empty,
        cgTaxPerCurrencyUnit = Amount.empty,
        nicsAndTaxPerCurrencyUnit = Amount.empty,
        totalCgTaxRate = Rate.empty,
        nicsAndTaxRate = Rate.empty,
        startingRateForSavings = Amount(100, "GBP"),
        startingRateForSavingsAmount = Amount(200, "GBP"),
        basicRateIncomeTax = Amount(300, "GBP"),
        basicRateIncomeTaxAmount = Amount(400, "GBP"),
        higherRateIncomeTax = Amount(500, "GBP"),
        higherRateIncomeTaxAmount = Amount(600, "GBP"),
        additionalRateIncomeTax = Amount(700, "GBP"),
        additionalRateIncomeTaxAmount = Amount(800, "GBP"),
        ordinaryRate = Amount(900, "GBP"),
        ordinaryRateAmount = Amount(1000, "GBP"),
        upperRate = Amount(1100, "GBP"),
        upperRateAmount = Amount(1200, "GBP"),
        additionalRate = Amount(1300, "GBP"),
        additionalRateAmount = Amount(1400, "GBP"),
        otherAdjustmentsIncreasing = Amount(1500, "GBP"),
        marriageAllowanceReceivedAmount = Amount(1600, "GBP"),
        otherAdjustmentsReducing = Amount(1700, "GBP"),
        scottishTax = ScottishTax.empty,
        totalIncomeTax = Amount.gbp(3500),
        scottishIncomeTax = Amount.empty,
        welshIncomeTax = Amount.gbp(2600),
        savingsTax = savingsTax,
        incomeTaxStatus = "0003",
        startingRateForSavingsRateRate = Rate("10%"),
        basicRateIncomeTaxRateRate = Rate("20%"),
        higherRateIncomeTaxRateRate = Rate("30%"),
        additionalRateIncomeTaxRateRate = Rate("40%"),
        ordinaryRateTaxRateRate = Rate("50%"),
        upperRateRateRate = Rate("60%"),
        additionalRateRateRate = Rate("70%"),
        scottishRates = ScottishRates.empty,
        savingsRates = savingsRates,
        title = "Mr",
        forename = "John",
        surname = "Smith"
      )
    }

    "return a isScottishTaxPayer as true and isWelshTaxPayer as false when incomeTaxStatus is 0002" in {
      val incomeData: AtsData    = AtsTestData.totalIncomeTaxData
      val result: IncomeTaxAndNI = sut.totalIncomeConverter(incomeData)
      result.isScottishTaxPayer mustEqual true
      result.isWelshTaxPayer mustEqual false
    }

    "return a isScottishTaxPayer as false and isWelshTaxPayer as true when incomeTaxStatus is 0003" in {
      val incomeData: AtsData    = AtsTestData.incomeTaxDataForWelshTaxPayer
      val result: IncomeTaxAndNI = sut.totalIncomeConverter(incomeData)
      result.isScottishTaxPayer mustEqual false
      result.isWelshTaxPayer mustEqual true
    }

  }
}
