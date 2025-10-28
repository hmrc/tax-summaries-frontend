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

package services.atsData

import models.*
import utils.TaxYearForTesting
import view_models.{Amount, Rate}

object AtsTestData extends TaxYearForTesting {

  val atsAllowancesData = AtsData(
    currentTaxYearSA,
    Some("1111111111"),
    None,
    None,
    None,
    Some(
      DataHolder(
        Some(
          Map(
            "personal_tax_free_amount"              -> Amount(100, "GBP"),
            "marriage_allowance_transferred_amount" -> Amount(200, "GBP"),
            "other_allowances_amount"               -> Amount(300, "GBP"),
            "total_tax_free_amount"                 -> Amount(400, "GBP")
          )
        ),
        None,
        None
      )
    ),
    None,
    None,
    Map(
      "title"    -> "Mr",
      "forename" -> "John",
      "surname"  -> "Smith"
    ),
    None,
    None
  )

  val incomeData = AtsData(
    currentTaxYearSA,
    Some("1111111111"),
    None,
    None,
    Some(
      DataHolder(
        Some(
          Map(
            "self_employment_income"   -> Amount(100, "GBP"),
            "income_from_employment"   -> Amount(200, "GBP"),
            "state_pension"            -> Amount(300, "GBP"),
            "other_pension_income"     -> Amount(400, "GBP"),
            "taxable_state_benefits"   -> Amount(500, "GBP"),
            "other_income"             -> Amount(600, "GBP"),
            "benefits_from_employment" -> Amount(700, "GBP"),
            "total_income_before_tax"  -> Amount(800, "GBP")
          )
        ),
        None,
        None
      )
    ),
    None,
    None,
    None,
    Map(
      "title"    -> "Mr",
      "forename" -> "John",
      "surname"  -> "Smith"
    ),
    None,
    None
  )

  val totalIncomeTaxData = AtsData(
    taxYear = currentTaxYearSA,
    utr = Some("1111111111"),
    income_tax = Some(
      DataHolder(
        Some(
          Map(
            "starting_rate_for_savings"          -> Amount(100, "GBP"),
            "starting_rate_for_savings_amount"   -> Amount(200, "GBP"),
            "basic_rate_income_tax"              -> Amount(300, "GBP"),
            "basic_rate_income_tax_amount"       -> Amount(400, "GBP"),
            "higher_rate_income_tax"             -> Amount(500, "GBP"),
            "higher_rate_income_tax_amount"      -> Amount(600, "GBP"),
            "additional_rate_income_tax"         -> Amount(700, "GBP"),
            "additional_rate_income_tax_amount"  -> Amount(800, "GBP"),
            "ordinary_rate"                      -> Amount(900, "GBP"),
            "ordinary_rate_amount"               -> Amount(1000, "GBP"),
            "upper_rate"                         -> Amount(1100, "GBP"),
            "upper_rate_amount"                  -> Amount(1200, "GBP"),
            "additional_rate"                    -> Amount(1300, "GBP"),
            "additional_rate_amount"             -> Amount(1400, "GBP"),
            "other_adjustments_increasing"       -> Amount(1500, "GBP"),
            "marriage_allowance_received_amount" -> Amount(1600, "GBP"),
            "other_adjustments_reducing"         -> Amount(1700, "GBP"),
            "scottish_starter_rate_tax"          -> Amount.gbp(1800),
            "scottish_starter_income"            -> Amount.gbp(1900),
            "scottish_basic_rate_tax"            -> Amount.gbp(2000),
            "scottish_basic_income"              -> Amount.gbp(2100),
            "scottish_intermediate_rate_tax"     -> Amount.gbp(2200),
            "scottish_intermediate_income"       -> Amount.gbp(2300),
            "scottish_higher_rate_tax"           -> Amount.gbp(2400),
            "scottish_higher_income"             -> Amount.gbp(2500),
            "scottish_advanced_rate_tax"         -> Amount.gbp(3700),
            "scottish_advanced_income"           -> Amount.gbp(3800),
            "scottish_additional_rate_tax"       -> Amount.gbp(2600),
            "scottish_additional_income"         -> Amount.gbp(2700),
            "scottish_top_rate_tax"              -> Amount.gbp(2800),
            "scottish_top_income"                -> Amount.gbp(2900),
            "scottish_total_tax"                 -> Amount.gbp(3000),
            "savings_lower_rate_tax"             -> Amount.gbp(2900),
            "savings_lower_income"               -> Amount.gbp(3000),
            "savings_higher_rate_tax"            -> Amount.gbp(3100),
            "savings_higher_income"              -> Amount.gbp(3200),
            "savings_additional_rate_tax"        -> Amount.gbp(3300),
            "savings_additional_income"          -> Amount.gbp(3400),
            "total_income_tax"                   -> Amount(3500, "GBP"),
            "scottish_income_tax"                -> Amount(3600, "GBP")
          )
        ),
        Some(
          Map(
            "starting_rate_for_savings_rate"  -> Rate("10%"),
            "basic_rate_income_tax_rate"      -> Rate("20%"),
            "higher_rate_income_tax_rate"     -> Rate("30%"),
            "additional_rate_income_tax_rate" -> Rate("40%"),
            "ordinary_rate_tax_rate"          -> Rate("50%"),
            "upper_rate_rate"                 -> Rate("60%"),
            "additional_rate_rate"            -> Rate("70%"),
            "scottish_starter_rate"           -> Rate("80%"),
            "scottish_basic_rate"             -> Rate("90%"),
            "scottish_intermediate_rate"      -> Rate("100%"),
            "scottish_higher_rate"            -> Rate("110%"),
            "scottish_advanced_rate"          -> Rate("160%"),
            "scottish_additional_rate"        -> Rate("120%"),
            "savings_lower_rate"              -> Rate("130%"),
            "scottish_top_rate"               -> Rate("130%"),
            "savings_higher_rate"             -> Rate("140%"),
            "savings_additional_rate"         -> Rate("150%")
          )
        ),
        Some("0002")
      )
    ),
    summary_data = Some(
      DataHolder(
        Some(
          Map(
            "employee_nic_amount"            -> Amount(100, "GBP"),
            "total_income_tax_and_nics"      -> Amount(200, "GBP"),
            "your_total_tax"                 -> Amount(300, "GBP"),
            "personal_tax_free_amount"       -> Amount(400, "GBP"),
            "total_tax_free_amount"          -> Amount(400, "GBP"),
            "total_income_before_tax"        -> Amount(500, "GBP"),
            "total_income_tax"               -> Amount(600, "GBP"),
            "total_cg_tax"                   -> Amount(700, "GBP"),
            "taxable_gains"                  -> Amount(800, "GBP"),
            "cg_tax_per_currency_unit"       -> Amount(900, "GBP"),
            "nics_and_tax_per_currency_unit" -> Amount(1000, "GBP")
          )
        ),
        Some(
          Map(
            "total_cg_tax_rate" -> Rate("10.00%"),
            "nics_and_tax_rate" -> Rate("20.00%")
          )
        ),
        None
      )
    ),
    income_data = None,
    allowance_data = None,
    capital_gains_data = None,
    gov_spending = None,
    taxPayerData = Map(
      "title"    -> "Mr",
      "forename" -> "John",
      "surname"  -> "Smith"
    ),
    errors = None,
    None
  )

  val totalIncomeTaxDataWithBRDReductionAmount: AtsData = {
    val pl = totalIncomeTaxData.income_tax.flatMap(_.payload).map {
      _ ++ Map(
        "brdReduction" -> Amount(BigDecimal(300), "GBP")
      )
    }
    val sd = totalIncomeTaxData.income_tax.map(dh => dh.copy(payload = pl))
    totalIncomeTaxData.copy(income_tax = sd)
  }

  val totalIncomeTaxDataWithBRDChargeAmount: AtsData = {
    val updatedPayload: Option[Map[String, Amount]] = totalIncomeTaxData.income_tax.flatMap(_.payload).map {
      _ ++ Map(
        "brdCharge" -> Amount(BigDecimal(300), "GBP")
      )
    }
    val incomeTaxSection: Option[DataHolder]        = totalIncomeTaxData.income_tax.map(_.copy(payload = updatedPayload))
    totalIncomeTaxData.copy(income_tax = incomeTaxSection)
  }

  val govSpendingDataForWelshUser = AtsData(
    currentTaxYearSA,
    Some("1111111111"),
    Some(
      DataHolder(
        Some(
          Map(
            "scottish_income_tax" -> Amount(0, "GBP"),
            "welsh_income_tax"    -> Amount(500, "GBP")
          )
        ),
        None,
        Some("0003")
      )
    ),
    None,
    None,
    None,
    None,
    Some(
      GovernmentSpendingOutputWrapper(
        currentTaxYearSA,
        Some(
          Map(
            "Welfare"                  -> SpendData(Amount(100, "GBP"), 10),
            "Health"                   -> SpendData(Amount(100, "GBP"), 10),
            "StatePensions"            -> SpendData(Amount(100, "GBP"), 10),
            "Education"                -> SpendData(Amount(100, "GBP"), 10),
            "Defence"                  -> SpendData(Amount(100, "GBP"), 10),
            "NationalDebtInterest"     -> SpendData(Amount(100, "GBP"), 10),
            "Transport"                -> SpendData(Amount(100, "GBP"), 10),
            "PublicOrderAndSafety"     -> SpendData(Amount(100, "GBP"), 10),
            "BusinessAndIndustry"      -> SpendData(Amount(100, "GBP"), 10),
            "GovernmentAdministration" -> SpendData(Amount(100, "GBP"), 10),
            "HousingAndUtilities"      -> SpendData(Amount(100, "GBP"), 10),
            "Environment"              -> SpendData(Amount(100, "GBP"), 10),
            "Culture"                  -> SpendData(Amount(100, "GBP"), 10),
            "OverseasAid"              -> SpendData(Amount(100, "GBP"), 10),
            "UkContributionToEuBudget" -> SpendData(Amount(100, "GBP"), 10)
          )
        ),
        Amount(200, "GBP"),
        None
      )
    ),
    Map(
      "title"    -> "Mr",
      "forename" -> "John",
      "surname"  -> "Smith"
    ),
    None,
    None
  )

  val govSpendingData = AtsData(
    currentTaxYearSA,
    Some("1111111111"),
    Some(
      DataHolder(
        Some(
          Map(
            "scottish_income_tax" -> Amount(500, "GBP")
          )
        ),
        None,
        Some("0002")
      )
    ),
    None,
    None,
    None,
    None,
    Some(
      GovernmentSpendingOutputWrapper(
        currentTaxYearSA,
        Some(
          Map(
            "Health"                     -> SpendData(Amount(100, "GBP"), 10),
            "Culture"                    -> SpendData(Amount(100, "GBP"), 10),
            "NationalDebtInterest"       -> SpendData(Amount(100, "GBP"), 10),
            "BusinessAndIndustry"        -> SpendData(Amount(100, "GBP"), 10),
            "Welfare"                    -> SpendData(Amount(100, "GBP"), 10),
            "Transport"                  -> SpendData(Amount(100, "GBP"), 10),
            "PublicOrderAndSafety"       -> SpendData(Amount(100, "GBP"), 10),
            "GovernmentAdministration"   -> SpendData(Amount(100, "GBP"), 10),
            "StatePensions"              -> SpendData(Amount(100, "GBP"), 10),
            "Education"                  -> SpendData(Amount(100, "GBP"), 10),
            "HousingAndUtilities"        -> SpendData(Amount(100, "GBP"), 10),
            "Environment"                -> SpendData(Amount(100, "GBP"), 10),
            "Defence"                    -> SpendData(Amount(100, "GBP"), 10),
            "OutstandingPaymentsToTheEU" -> SpendData(Amount(100, "GBP"), 10),
            "OverseasAid"                -> SpendData(Amount(100, "GBP"), 10)
          )
        ),
        Amount(200, "GBP"),
        None
      )
    ),
    Map(
      "title"    -> "Mr",
      "forename" -> "John",
      "surname"  -> "Smith"
    ),
    None,
    None
  )

  val summaryData = AtsData(
    taxYear = currentTaxYearSA,
    utr = Some("1111111111"),
    income_tax = None,
    summary_data = Some(
      DataHolder(
        Some(
          Map(
            "employee_nic_amount"            -> Amount(100, "GBP"),
            "total_income_tax_and_nics"      -> Amount(200, "GBP"),
            "your_total_tax"                 -> Amount(300, "GBP"),
            "personal_tax_free_amount"       -> Amount(400, "GBP"),
            "total_tax_free_amount"          -> Amount(400, "GBP"),
            "total_income_before_tax"        -> Amount(500, "GBP"),
            "total_income_tax"               -> Amount(600, "GBP"),
            "total_cg_tax"                   -> Amount(700, "GBP"),
            "taxable_gains"                  -> Amount(800, "GBP"),
            "cg_tax_per_currency_unit"       -> Amount(900, "GBP"),
            "nics_and_tax_per_currency_unit" -> Amount(1000, "GBP")
          )
        ),
        Some(
          Map(
            "total_cg_tax_rate" -> Rate("10%"),
            "nics_and_tax_rate" -> Rate("20%")
          )
        ),
        None
      )
    ),
    income_data = None,
    allowance_data = None,
    capital_gains_data = None,
    gov_spending = None,
    taxPayerData = Map(
      "title"    -> "Mr",
      "forename" -> "John",
      "surname"  -> "Smith"
    ),
    errors = None,
    None
  )

  val capitalGainsData = AtsData(
    currentTaxYearSA,
    Some("1111111111"),
    None,
    None,
    None,
    None,
    Some(
      DataHolder(
        Some(
          Map(
            "taxable_gains"                    -> Amount(100, "GBP"),
            "less_tax_free_amount"             -> Amount(200, "GBP"),
            "pay_cg_tax_on"                    -> Amount(300, "GBP"),
            "amount_at_entrepreneurs_rate"     -> Amount(400, "GBP"),
            "amount_due_at_entrepreneurs_rate" -> Amount(500, "GBP"),
            "amount_at_ordinary_rate"          -> Amount(600, "GBP"),
            "amount_due_at_ordinary_rate"      -> Amount(700, "GBP"),
            "amount_at_higher_rate"            -> Amount(800, "GBP"),
            "amount_due_at_higher_rate"        -> Amount(900, "GBP"),
            "amount_due_rpci_lower_rate"       -> Amount.gbp(1000),
            "amount_at_rpci_lower_rate"        -> Amount.gbp(1100),
            "amount_due_rpci_higher_rate"      -> Amount.gbp(1200),
            "amount_at_rpci_higher_rate"       -> Amount.gbp(1300),
            "amount_due_ci_lower_rate"         -> Amount.gbp(1310),
            "amount_at_ci_lower_rate"          -> Amount.gbp(1320),
            "amount_due_ci_higher_rate"        -> Amount.gbp(1330),
            "amount_at_ci_higher_rate"         -> Amount.gbp(1340),
            "amount_due_rp_lower_rate"         -> Amount.gbp(1350),
            "amount_at_rp_lower_rate"          -> Amount.gbp(1360),
            "amount_due_rp_higher_rate"        -> Amount.gbp(1370),
            "amount_at_rp_higher_rate"         -> Amount.gbp(1380),
            "adjustments"                      -> Amount(1400, "GBP"),
            "total_cg_tax"                     -> Amount(1500, "GBP"),
            "cg_tax_per_currency_unit"         -> Amount(1600, "GBP")
          )
        ),
        Some(
          Map(
            "cg_entrepreneurs_rate"          -> Rate("10%"),
            "cg_ordinary_rate"               -> Rate("20%"),
            "cg_upper_rate"                  -> Rate("30%"),
            "prop_interest_rate_lower_rate"  -> Rate("40%"),
            "prop_interest_rate_higher_rate" -> Rate("50%"),
            "ci_interest_rate_lower_rate"    -> Rate("51%"),
            "ci_interest_rate_higher_rate"   -> Rate("52%"),
            "rp_interest_rate_lower_rate"    -> Rate("53%"),
            "rp_interest_rate_higher_rate"   -> Rate("54%"),
            "total_cg_tax_rate"              -> Rate("60%")
          )
        ),
        None
      )
    ),
    None,
    Map(
      "title"    -> "Mr",
      "forename" -> "John",
      "surname"  -> "Smith"
    ),
    None,
    None
  )

  val atsListData = AtsListData(
    "1111111111",
    Some(
      Map(
        "forename" -> "John",
        "surname"  -> "Smith"
      )
    ),
    Some(
      List(
        currentTaxYearSA
      )
    )
  )

  val incomeTaxDataForWelshTaxPayer = AtsData(
    taxYear = currentTaxYearSA,
    utr = Some("1111111111"),
    income_tax = Some(
      DataHolder(
        Some(
          Map(
            "starting_rate_for_savings"          -> Amount(100, "GBP"),
            "starting_rate_for_savings_amount"   -> Amount(200, "GBP"),
            "basic_rate_income_tax"              -> Amount(300, "GBP"),
            "basic_rate_income_tax_amount"       -> Amount(400, "GBP"),
            "higher_rate_income_tax"             -> Amount(500, "GBP"),
            "higher_rate_income_tax_amount"      -> Amount(600, "GBP"),
            "additional_rate_income_tax"         -> Amount(700, "GBP"),
            "additional_rate_income_tax_amount"  -> Amount(800, "GBP"),
            "ordinary_rate"                      -> Amount(900, "GBP"),
            "ordinary_rate_amount"               -> Amount(1000, "GBP"),
            "upper_rate"                         -> Amount(1100, "GBP"),
            "upper_rate_amount"                  -> Amount(1200, "GBP"),
            "additional_rate"                    -> Amount(1300, "GBP"),
            "additional_rate_amount"             -> Amount(1400, "GBP"),
            "other_adjustments_increasing"       -> Amount(1500, "GBP"),
            "marriage_allowance_received_amount" -> Amount(1600, "GBP"),
            "other_adjustments_reducing"         -> Amount(-1700, "GBP"),
            "savings_lower_rate_tax"             -> Amount.gbp(2900),
            "savings_lower_income"               -> Amount.gbp(3000),
            "savings_higher_rate_tax"            -> Amount.gbp(3100),
            "savings_higher_income"              -> Amount.gbp(3200),
            "savings_additional_rate_tax"        -> Amount.gbp(3300),
            "savings_additional_income"          -> Amount.gbp(3400),
            "total_income_tax"                   -> Amount(3500, "GBP"),
            "welsh_income_tax"                   -> Amount(2600, "GBP")
          )
        ),
        Some(
          Map(
            "starting_rate_for_savings_rate"  -> Rate("10%"),
            "basic_rate_income_tax_rate"      -> Rate("20%"),
            "higher_rate_income_tax_rate"     -> Rate("30%"),
            "additional_rate_income_tax_rate" -> Rate("40%"),
            "ordinary_rate_tax_rate"          -> Rate("50%"),
            "upper_rate_rate"                 -> Rate("60%"),
            "additional_rate_rate"            -> Rate("70%"),
            "savings_lower_rate"              -> Rate("130%"),
            "savings_higher_rate"             -> Rate("140%"),
            "savings_additional_rate"         -> Rate("150%")
          )
        ),
        Some("0003")
      )
    ),
    summary_data = Some(
      DataHolder(
        Some(
          Map(
            "employee_nic_amount"            -> Amount(100, "GBP"),
            "total_income_tax_and_nics"      -> Amount(200, "GBP"),
            "your_total_tax"                 -> Amount(300, "GBP"),
            "personal_tax_free_amount"       -> Amount(400, "GBP"),
            "total_tax_free_amount"          -> Amount(400, "GBP"),
            "total_income_before_tax"        -> Amount(500, "GBP"),
            "total_income_tax"               -> Amount(600, "GBP"),
            "total_cg_tax"                   -> Amount(700, "GBP"),
            "taxable_gains"                  -> Amount(800, "GBP"),
            "cg_tax_per_currency_unit"       -> Amount(900, "GBP"),
            "nics_and_tax_per_currency_unit" -> Amount(1000, "GBP")
          )
        ),
        Some(
          Map(
            "total_cg_tax_rate" -> Rate("10.00%"),
            "nics_and_tax_rate" -> Rate("20.00%")
          )
        ),
        None
      )
    ),
    income_data = None,
    allowance_data = None,
    capital_gains_data = None,
    gov_spending = None,
    taxPayerData = Map(
      "title"    -> "Mr",
      "forename" -> "John",
      "surname"  -> "Smith"
    ),
    errors = None,
    None
  )
}
