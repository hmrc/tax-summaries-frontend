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

package utils

import models.SpendData
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.domain.{Generator, Nino, SaUtrGenerator}
import view_models._

import scala.util.Random

trait TestConstants extends BaseSpec {

  // We only want one test nino and utr throughout, therefore assign a value in the object declaration
  lazy val testUtr: String                  = new SaUtrGenerator().nextSaUtr.utr
  lazy val testUar: String                  = "V" + genRandNumString(4) + "H"
  lazy val testInvalidUtr: String           = genRandNumString(4)
  lazy val testKey: String                  = genRandNumString(22)
  lazy val testOid: String                  = genRandNumString(12)
  lazy val testNonMatchingUtr: String       = new SaUtrGenerator().nextSaUtr.utr
  lazy val testNino: Nino                   = new Generator().nextNino
  val fakeCredentials: Credentials          = new Credentials("provider ID", "provider type")
  def genRandNumString(length: Int): String = Random.nextInt(9).toString * length

  val testIncomeTaxAndNI: IncomeTaxAndNI = IncomeTaxAndNI(
    year = taxYear,
    utr = testUtr,
    employeeNicAmount = Amount(1200, "GBP"),
    totalIncomeTaxAndNics = Amount(1400, "GBP"),
    yourTotalTax = Amount(1800, "GBP"),
    totalTaxFree = Amount(9440, "GBP"),
    totalTaxFreeAllowance = Amount(9740, "GBP"),
    yourIncomeBeforeTax = Amount(11600, "GBP"),
    totalIncomeTaxAmount = Amount(372, "GBP"),
    totalCapitalGainsTax = Amount(5500, "GBP"),
    taxableGains = Amount(20000, "GBP"),
    cgTaxPerCurrencyUnit = Amount(0.1234, "GBP"),
    nicsAndTaxPerCurrencyUnit = Amount(0.5678, "GBP"),
    totalCgTaxRate = Rate("12.34%"),
    nicsAndTaxRate = Rate("56.78%"),
    Amount.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    ScottishTax.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    SavingsTax.empty,
    "",
    Rate.empty,
    Rate.empty,
    Rate.empty,
    Rate.empty,
    Rate.empty,
    Rate.empty,
    Rate.empty,
    ScottishRates.empty,
    SavingsRates.empty,
    "Mr",
    "forename",
    "surname"
  )

  val capitalGains: CapitalGains = CapitalGains(
    taxYear = taxYear,
    utr = testUtr,
    taxableGains = Amount(20000, "GBP"),
    lessTaxFreeAmount = -Amount(10600, "GBP"),
    payCgTaxOn = Amount(9400, "GBP"),
    entrepreneursReliefRateBefore = Amount(1111, "GBP"),
    entrepreneursReliefRateAmount = Amount(1000, "GBP"),
    ordinaryRateBefore = Amount(2222, "GBP"),
    ordinaryRateAmount = Amount(2000, "GBP"),
    upperRateBefore = Amount(3333, "GBP"),
    upperRateAmount = Amount(3000, "GBP"),
    rpciLowerTax = Amount.gbp(1000),
    rpciLowerTotalAmount = Amount.gbp(4000),
    rpciHigherTax = Amount.gbp(1500),
    rpciHigherTotalAmount = Amount.gbp(4500),
    adjustmentsAmount = -Amount(500, "GBP"),
    totalCapitalGainsTaxAmount = Amount(5500, "GBP"),
    cgTaxPerCurrencyUnit = Amount(0.1234, "GBP"),
    entrepreneursReliefRateRate = Rate("10%"),
    ordinaryRateRate = Rate("18%"),
    upperRateRate = Rate("28%"),
    rpciLowerRate = Rate("15%"),
    rpciHigherRate = Rate("25%"),
    totalCgTaxRate = Rate("12.34%"),
    title = "Mr",
    forename = "forename",
    surname = "surname"
  )

  val govSpendTotalTuple: (String, SpendData) = ("GovSpendTotal", SpendData(Amount(2898.13, "GBP"), 100.0))

  val fakeTaxYear: Int = taxYear - 3

  val fakeGovernmentSpend: GovernmentSpend =
    GovernmentSpend(
      fakeTaxYear,
      testUtr,
      List(
        ("Welfare", SpendData(Amount(2898.13, "GBP"), 23.5)),
        ("Health", SpendData(Amount(2898.13, "GBP"), 20.2)),
        ("StatePensions", SpendData(Amount(2898.13, "GBP"), 11.8)),
        ("Education", SpendData(Amount(2898.13, "GBP"), 12.8)),
        ("Defence", SpendData(Amount(2898.13, "GBP"), 5.3)),
        ("NationalDebtInterest", SpendData(Amount(2898.13, "GBP"), 5.3)),
        ("PublicOrderAndSafety", SpendData(Amount(2898.13, "GBP"), 4.3)),
        ("Transport", SpendData(Amount(2898.13, "GBP"), 4.3)),
        ("BusinessAndIndustry", SpendData(Amount(2898.13, "GBP"), 3.6)),
        ("GovernmentAdministration", SpendData(Amount(2898.13, "GBP"), 2.1)),
        ("HousingAndUtilities", SpendData(Amount(2898.13, "GBP"), 1.6)),
        ("Culture", SpendData(Amount(2898.13, "GBP"), 1.5)),
        ("Environment", SpendData(Amount(2898.13, "GBP"), 1.5)),
        ("OverseasAid", SpendData(Amount(2898.13, "GBP"), 1.2)),
        ("UkContributionToEuBudget", SpendData(Amount(2898.13, "GBP"), 1)),
        govSpendTotalTuple
      ),
      "Mr",
      "John",
      "Doe",
      Amount(23912.00, "GBP"),
      "0002",
      Amount(2000.00, "GBP")
    )

  val governmentSpendFromBackend: GovernmentSpend =
    GovernmentSpend(
      fakeTaxYear,
      testUtr,
      List(
        ("Welfare", SpendData(Amount(2898.13, "GBP"), 23.5)),
        ("Health", SpendData(Amount(2898.13, "GBP"), 20.2)),
        ("StatePensions", SpendData(Amount(2898.13, "GBP"), 11.8)),
        ("Education", SpendData(Amount(2898.13, "GBP"), 12.8)),
        ("Defence", SpendData(Amount(2898.13, "GBP"), 5.3)),
        ("NationalDebtInterest", SpendData(Amount(2898.13, "GBP"), 5.3)),
        ("PublicOrderAndSafety", SpendData(Amount(2898.13, "GBP"), 4.3)),
        ("Transport", SpendData(Amount(2898.13, "GBP"), 4.3)),
        ("BusinessAndIndustry", SpendData(Amount(2898.13, "GBP"), 3.6)),
        ("GovernmentAdministration", SpendData(Amount(2898.13, "GBP"), 2.1)),
        ("HousingAndUtilities", SpendData(Amount(2898.13, "GBP"), 1.6)),
        ("Culture", SpendData(Amount(2898.13, "GBP"), 1.5)),
        ("Environment", SpendData(Amount(2898.13, "GBP"), 1.5)),
        ("OverseasAid", SpendData(Amount(2898.13, "GBP"), 1.2)),
        ("UkContributionToEuBudget", SpendData(Amount(2898.13, "GBP"), 1))
      ),
      "Mr",
      "John",
      "Doe",
      Amount(23912.00, "GBP"),
      "0002",
      Amount(2000.00, "GBP")
    )

  val expectedPercentageOrder2022: List[BigDecimal] =
    List(23.5, 20.2, 12.8, 11.8, 5.3, 5.3, 4.3, 4.3, 3.6, 2.1, 1.6, 1.5, 1.5, 1.2, 1)

  val expectedPercentageOrder2020: List[BigDecimal] =
    List(22.1, 20.5, 12.4, 11.6, 6.9, 5.3, 4.3, 4.3, 3.8, 2.1, 1.8, 1.5, 1.5, 1.1, 0.8)

  val expectedPercentageOrder2021: List[BigDecimal] =
    List(21.9, 19.6, 14.4, 10.1, 9.6, 4.5, 4.5, 4.1, 3.9, 2.0, 1.4, 1.3, 1.2, 0.9, 0.6)

  val expectedCategoryOrderFor2022: List[String] =
    List(
      "Welfare",
      "Health",
      "StatePensions",
      "Education",
      "Defence",
      "NationalDebtInterest",
      "Transport",
      "PublicOrderAndSafety",
      "BusinessAndIndustry",
      "GovernmentAdministration",
      "HousingAndUtilities",
      "Environment",
      "Culture",
      "OverseasAid",
      "UkContributionToEuBudget"
    )

  val expectedCategoryOrderFor2020: List[String] =
    List(
      "Welfare",
      "Health",
      "StatePensions",
      "Education",
      "NationalDebtInterest",
      "Defence",
      "Transport",
      "PublicOrderAndSafety",
      "BusinessAndIndustry",
      "GovernmentAdministration",
      "HousingAndUtilities",
      "Culture",
      "Environment",
      "OverseasAid",
      "UkContributionToEuBudget"
    )

  val expectedCategoryOrderFor2021: List[String] =
    List(
      "Health",
      "Welfare",
      "BusinessAndIndustry",
      "StatePensions",
      "Education",
      "Transport",
      "Defence",
      "NationalDebtInterest",
      "PublicOrderAndSafety",
      "GovernmentAdministration",
      "HousingAndUtilities",
      "Environment",
      "Culture",
      "OverseasAid",
      "UkContributionToEuBudget"
    )
}

object TestConstants extends TestConstants
