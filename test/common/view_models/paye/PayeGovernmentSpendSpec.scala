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

package common.view_models.paye

import common.models.{DataHolder, GovernmentSpendingOutputWrapper, PayeAtsData, SpendData}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.test.Injecting
import common.services.atsData.PayeAtsTestData
import common.utils.{BaseSpec, JsonUtil}
import common.view_models.Amount
import paye.view_models.{PayeGovernmentSpend, SpendRow}

class PayeGovernmentSpendSpec
    extends BaseSpec
    with Matchers
    with JsonUtil
    with ScalaFutures
    with IntegrationPatience
    with Injecting {

  lazy val payeAtsTestData: PayeAtsTestData = inject[PayeAtsTestData]

  "PayeGovernmentSpend" must {

    "Transform PayeAtsData to view model" when {

      val govSpendingData: PayeAtsData = PayeAtsData(
        taxYear = currentTaxYearSA,
        income_tax = None,
        summary_data = None,
        income_data = None,
        allowance_data = None,
        gov_spending = Some(
          GovernmentSpendingOutputWrapper(
            currentTaxYearSA,
            Some(
              Map(
                "UkContributionToEuBudget" -> SpendData(Amount(6.00, "GBP"), 0.60),
                "Welfare"                  -> SpendData(Amount(196.00, "GBP"), 19.60),
                "GovernmentAdministration" -> SpendData(Amount(20.00, "GBP"), 2.00),
                "Education"                -> SpendData(Amount(96.00, "GBP"), 9.60),
                "StatePensions"            -> SpendData(Amount(101.00, "GBP"), 10.10),
                "NationalDebtInterest"     -> SpendData(Amount(41.00, "GBP"), 4.10),
                "Defence"                  -> SpendData(Amount(45.00, "GBP"), 4.50),
                "PublicOrderAndSafety"     -> SpendData(Amount(39.00, "GBP"), 3.90),
                "Transport"                -> SpendData(Amount(45.00, "GBP"), 4.50),
                "BusinessAndIndustry"      -> SpendData(Amount(144.00, "GBP"), 14.40),
                "Culture"                  -> SpendData(Amount(12.00, "GBP"), 1.20),
                "HousingAndUtilities"      -> SpendData(Amount(14.00, "GBP"), 1.40),
                "Environment"              -> SpendData(Amount(13.00, "GBP"), 1.30),
                "OverseasAid"              -> SpendData(Amount(9.00, "GBP"), 0.90),
                "Health"                   -> SpendData(Amount(219.00, "GBP"), 21.90)
              )
            ),
            Amount(200, "GBP"),
            None
          )
        ),
        includeBRDMessage = false
      )

      val payeGovernmentSpendViewModel: PayeGovernmentSpend = PayeGovernmentSpend(
        currentTaxYearSA,
        List(
          SpendRow("Health", SpendData(Amount(219, "GBP"), 21.9)),
          SpendRow("Welfare", SpendData(Amount(196, "GBP"), 19.6)),
          SpendRow("BusinessAndIndustry", SpendData(Amount(144, "GBP"), 14.4)),
          SpendRow("StatePensions", SpendData(Amount(101, "GBP"), 10.1)),
          SpendRow("Education", SpendData(Amount(96, "GBP"), 9.6)),
          SpendRow("Transport", SpendData(Amount(45, "GBP"), 4.5)),
          SpendRow("Defence", SpendData(Amount(45, "GBP"), 4.5)),
          SpendRow("NationalDebtInterest", SpendData(Amount(41, "GBP"), 4.1)),
          SpendRow("PublicOrderAndSafety", SpendData(Amount(39, "GBP"), 3.9)),
          SpendRow("GovernmentAdministration", SpendData(Amount(20, "GBP"), 2.0)),
          SpendRow("HousingAndUtilities", SpendData(Amount(14, "GBP"), 1.4)),
          SpendRow("Environment", SpendData(Amount(13, "GBP"), 1.3)),
          SpendRow("Culture", SpendData(Amount(12, "GBP"), 1.2)),
          SpendRow("OverseasAid", SpendData(Amount(9, "GBP"), 0.9)),
          SpendRow("UkContributionToEuBudget", SpendData(Amount(6, "GBP"), 0.6))
        ),
        totalAmount = Amount(200, "GBP"),
        isScottish = false
      )

      val expectedPercentageOrder: List[BigDecimal] =
        List(21.9, 19.6, 14.4, 10.1, 9.6, 4.5, 4.5, 4.1, 3.9, 2.0, 1.4, 1.3, 1.2, 0.9, 0.6)
      val expectedCategoryOrder: List[String]       =
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

      "Scottish income is not present" in {

        val payeGovSpendingData = govSpendingData
        val result              = PayeGovernmentSpend(payeGovSpendingData, expectedCategoryOrder)

        result.orderedSpendRows.map(_.spendData.percentage) mustBe expectedPercentageOrder
        result mustBe payeGovernmentSpendViewModel
      }

      "Scottish income is present and greater than 0" in {
        val payeGovSpendingData = govSpendingData.copy(
          income_tax = Some(DataHolder(Some(Map("scottish_total_tax" -> Amount.gbp(500.00))), None, None))
        )
        val result              = PayeGovernmentSpend(payeGovSpendingData, expectedCategoryOrder)

        result.orderedSpendRows.map(_.spendData.percentage) mustBe expectedPercentageOrder
        result mustBe payeGovernmentSpendViewModel.copy(
          isScottish = true
        )
      }

      "Scottish income is present and equal to 0" in {
        val payeGovSpendingData = govSpendingData.copy(
          income_tax = Some(DataHolder(Some(Map("scottish_total_tax" -> Amount.gbp(0.00))), None, None))
        )
        val result              = PayeGovernmentSpend(payeGovSpendingData, expectedCategoryOrder)

        result.orderedSpendRows.map(_.spendData.percentage) mustBe expectedPercentageOrder
        result mustBe payeGovernmentSpendViewModel.copy(
          isScottish = false
        )
      }
    }

    s"reorder categories for tax year $currentTaxYearSA" in {
      val expectedCategoryOrderFor: List[String]    =
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
      val expectedPercentageOrder: List[BigDecimal] =
        List(23.5, 20.2, 12.8, 11.8, 5.3, 5.3, 4.3, 4.3, 3.6, 2.1, 1.6, 1.5, 1.5, 1.2, 1)
      val payeGovSpendingData                       = payeAtsTestData.govSpendingDataForTaxYear(currentTaxYearSA)
      val result                                    = PayeGovernmentSpend(payeGovSpendingData, expectedCategoryOrderFor)

      result.orderedSpendRows.map(_.spendData.percentage) mustBe expectedPercentageOrder
      result.orderedSpendRows.map(_.category) mustBe expectedCategoryOrderFor
    }
  }
}
