/*
 * Copyright 2020 HM Revenue & Customs
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
import uk.gov.hmrc.domain.{Generator, SaUtrGenerator}
import utils.TestConstants.testUtr
import view_models._

import scala.util.Random

trait TestConstants {

  // We only want one test nino and utr throughout, therefore assign a value in the object declaration
  lazy val testUtr = new SaUtrGenerator().nextSaUtr.utr
  lazy val testUar = "V" + genRandNumString(4) + "H"
  lazy val testInvalidUtr = genRandNumString(4)
  lazy val testKey = genRandNumString(22)
  lazy val testOid = genRandNumString(12)
  lazy val testNonMatchingUtr = new SaUtrGenerator().nextSaUtr.utr
  lazy val testNino = new Generator().nextNino
  def genRandNumString(length: Int) = Random.nextInt(9).toString * length

  val testTotalIncomeTax = TotalIncomeTax(
    year = 2014,
    utr = "",
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

  val capitalGains = CapitalGains(
    taxYear = 2014,
    utr = testUtr,
    taxableGains = Amount(20000, "GBP"),
    lessTaxFreeAmount = Amount(10600, "GBP"),
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
    adjustmentsAmount = Amount(500, "GBP"),
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

  val fakeTaxYear = 2019

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
        ("NationalDebtInterest", SpendData(Amount(2898.13, "GBP"), 5.1)),
        ("PublicOrderAndSafety", SpendData(Amount(2898.13, "GBP"), 4.3)),
        ("Transport", SpendData(Amount(2898.13, "GBP"), 4.3)),
        ("BusinessAndIndustry", SpendData(Amount(2898.13, "GBP"), 3.6)),
        ("GovernmentAdministration", SpendData(Amount(2898.13, "GBP"), 2.1)),
        ("HousingAndUtilities", SpendData(Amount(2898.13, "GBP"), 1.6)),
        ("Environment", SpendData(Amount(2898.13, "GBP"), 1.5)),
        ("Culture", SpendData(Amount(2898.13, "GBP"), 1.5)),
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

  val expectedPercentageOrder: List[BigDecimal] =
    List(23.5, 20.2, 12.8, 11.8, 5.3, 5.1, 4.3, 4.3, 3.6, 2.1, 1.6, 1.5, 1.5, 1.2, 1)

  val expectedCategoryOrder: List[String] =
    List(
      "Welfare",
      "Health",
      "Education",
      "StatePensions",
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
}

object TestConstants extends TestConstants
