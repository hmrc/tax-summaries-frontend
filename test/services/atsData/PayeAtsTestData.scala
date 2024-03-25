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

import models._
import utils.BaseSpec
import view_models.paye._
import view_models.{Amount, Rate}

class PayeAtsTestData extends BaseSpec {

  val govSpendingData: PayeAtsData = PayeAtsData(
    2021,
    None,
    None,
    None,
    None,
    Some(
      GovernmentSpendingOutputWrapper(
        2021,
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
    )
  )

  val yourIncomeAndTaxesData: PayeAtsData = PayeAtsData(
    taxYear,
    None,
    Some(
      DataHolder(
        Some(
          Map(
            "employee_nic_amount"            -> Amount(100, "GBP"),
            "employer_nic_amount"            -> Amount(150, "GBP"),
            "total_income_tax_and_nics"      -> Amount(200, "GBP"),
            "your_total_tax"                 -> Amount(300, "GBP"),
            "personal_tax_free_amount"       -> Amount(400, "GBP"),
            "total_tax_free_amount"          -> Amount(400, "GBP"),
            "total_income_before_tax"        -> Amount(500, "GBP"),
            "total_income_tax"               -> Amount(600, "GBP"),
            "total_cg_tax"                   -> Amount(700, "GBP"),
            "taxable_gains"                  -> Amount(800, "GBP"),
            "cg_tax_per_currency_unit"       -> Amount(900, "GBP"),
            "nics_and_tax_per_currency_unit" -> Amount(1000, "GBP"),
            "income_after_tax_and_nics"      -> Amount(1100, "GBP"),
            "nics_and_tax_rate_amount"       -> Amount(20, "PERCENT")
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
    None,
    Some(
      DataHolder(
        Some(
          Map(
            "personal_tax_free_amount" -> Amount(9440, "GBP"),
            "other_allowances_amount"  -> Amount(300, "GBP"),
            "total_tax_free_amount"    -> Amount(9740, "GBP")
          )
        ),
        None,
        None
      )
    ),
    Some(
      GovernmentSpendingOutputWrapper(
        taxYear,
        None,
        Amount(200, "GBP"),
        None
      )
    )
  )

  val malformedYourIncomeAndTaxesData: PayeAtsData = PayeAtsData(
    taxYear,
    None,
    Some(
      DataHolder(
        Some(
          Map(
            "employee_nic_amount"            -> Amount(100, "GBP"),
            "employer_nic_amount"            -> Amount(150, "GBP"),
            "total_income_tax_and_nics"      -> Amount(200, "GBP"),
            "your_total_tax"                 -> Amount(300, "GBP"),
            "personal_tax_free_amount"       -> Amount(400, "GBP"),
            "total_tax_free_amount"          -> Amount(400, "GBP"),
            "total_income_tax"               -> Amount(600, "GBP"),
            "total_cg_tax"                   -> Amount(700, "GBP"),
            "taxable_gains"                  -> Amount(800, "GBP"),
            "cg_tax_per_currency_unit"       -> Amount(900, "GBP"),
            "nics_and_tax_per_currency_unit" -> Amount(1000, "GBP"),
            "income_after_tax_and_nics"      -> Amount(1100, "GBP"),
            "nics_and_tax_rate_amount"       -> Amount(20, "PERCENT")
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
    None,
    Some(
      DataHolder(
        Some(
          Map(
            "personal_tax_free_amount" -> Amount(9440, "GBP"),
            "other_allowances_amount"  -> Amount(300, "GBP"),
            "total_tax_free_amount"    -> Amount(9740, "GBP")
          )
        ),
        None,
        None
      )
    ),
    Some(
      GovernmentSpendingOutputWrapper(
        taxYear,
        None,
        Amount(200, "GBP"),
        None
      )
    )
  )

  val missingYourIncomeAndTaxesData: PayeAtsData = PayeAtsData(
    taxYear,
    None,
    None,
    None,
    Some(
      DataHolder(
        Some(
          Map(
            "personal_tax_free_amount" -> Amount(9440, "GBP"),
            "other_allowances_amount"  -> Amount(300, "GBP"),
            "total_tax_free_amount"    -> Amount(9740, "GBP")
          )
        ),
        None,
        None
      )
    ),
    Some(
      GovernmentSpendingOutputWrapper(
        taxYear,
        None,
        Amount(200, "GBP"),
        None
      )
    )
  )

  val YourIncomeAndTaxesDataWithMissingTotalTaxFreeAmount: PayeAtsData = PayeAtsData(
    taxYear,
    None,
    Some(
      DataHolder(
        Some(
          Map(
            "employee_nic_amount"            -> Amount(100, "GBP"),
            "employer_nic_amount"            -> Amount(150, "GBP"),
            "total_income_tax_and_nics"      -> Amount(200, "GBP"),
            "your_total_tax"                 -> Amount(300, "GBP"),
            "personal_tax_free_amount"       -> Amount(800, "GBP"),
            "total_income_before_tax"        -> Amount(500, "GBP"),
            "total_income_tax"               -> Amount(600, "GBP"),
            "total_cg_tax"                   -> Amount(700, "GBP"),
            "taxable_gains"                  -> Amount(800, "GBP"),
            "cg_tax_per_currency_unit"       -> Amount(900, "GBP"),
            "nics_and_tax_per_currency_unit" -> Amount(1000, "GBP"),
            "income_after_tax_and_nics"      -> Amount(1100, "GBP"),
            "nics_and_tax_rate_amount"       -> Amount(20, "PERCENT")
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
    None,
    Some(
      DataHolder(
        Some(
          Map(
            "personal_tax_free_amount" -> Amount(9440, "GBP"),
            "other_allowances_amount"  -> Amount(300, "GBP"),
            "total_tax_free_amount"    -> Amount(9740, "GBP")
          )
        ),
        None,
        None
      )
    ),
    Some(
      GovernmentSpendingOutputWrapper(
        taxYear,
        None,
        Amount(200, "GBP"),
        None
      )
    )
  )

  val YourIncomeAndTaxesDataWithMissingEmployeeNicAmount: PayeAtsData = PayeAtsData(
    taxYear,
    None,
    Some(
      DataHolder(
        Some(
          Map(
            "employer_nic_amount"            -> Amount(150, "GBP"),
            "total_income_tax_and_nics"      -> Amount(200, "GBP"),
            "your_total_tax"                 -> Amount(300, "GBP"),
            "personal_tax_free_amount"       -> Amount(800, "GBP"),
            "total_income_before_tax"        -> Amount(500, "GBP"),
            "total_income_tax"               -> Amount(600, "GBP"),
            "total_cg_tax"                   -> Amount(700, "GBP"),
            "taxable_gains"                  -> Amount(800, "GBP"),
            "cg_tax_per_currency_unit"       -> Amount(900, "GBP"),
            "nics_and_tax_per_currency_unit" -> Amount(1000, "GBP"),
            "income_after_tax_and_nics"      -> Amount(1100, "GBP"),
            "nics_and_tax_rate_amount"       -> Amount(20, "PERCENT")
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
    None,
    Some(
      DataHolder(
        Some(
          Map(
            "personal_tax_free_amount" -> Amount(9440, "GBP"),
            "other_allowances_amount"  -> Amount(300, "GBP"),
            "total_tax_free_amount"    -> Amount(9740, "GBP")
          )
        ),
        None,
        None
      )
    ),
    Some(
      GovernmentSpendingOutputWrapper(
        taxYear,
        None,
        Amount(200, "GBP"),
        None
      )
    )
  )

  val totalIncomeTaxAndSummaryData: PayeAtsData = PayeAtsData(
    taxYear,
    Some(
      DataHolder(
        Some(
          Map(
            "scottish_starter_rate_amount"         -> Amount.gbp(380),
            "scottish_starter_rate"                -> Amount.gbp(2000),
            "scottish_basic_rate_amount"           -> Amount.gbp(2030),
            "scottish_basic_rate"                  -> Amount.gbp(10150),
            "scottish_intermediate_rate_amount"    -> Amount.gbp(4080),
            "scottish_intermediate_rate"           -> Amount.gbp(19430),
            "scottish_higher_rate_amount"          -> Amount.gbp(12943),
            "scottish_higher_rate"                 -> Amount.gbp(31570),
            "scottish_total_tax"                   -> Amount.gbp(19433),
            "basic_rate_income_tax_amount"         -> Amount.gbp(480),
            "basic_rate_income_tax"                -> Amount.gbp(3000),
            "higher_rate_income_tax_amount"        -> Amount.gbp(3030),
            "higher_rate_income_tax"               -> Amount.gbp(20150),
            "ordinary_rate_amount"                 -> Amount.gbp(5080),
            "ordinary_rate"                        -> Amount.gbp(29430),
            "upper_rate_amount"                    -> Amount.gbp(22943),
            "upper_rate"                           -> Amount.gbp(41570),
            "total_UK_income_tax"                  -> Amount.gbp(20224),
            "total_income_tax_2"                   -> Amount.gbp(10477),
            "less_tax_adjustment_previous_year"    -> Amount.gbp(350),
            "marriage_allowance_received_amount"   -> Amount.gbp(200),
            "married_couples_allowance_adjustment" -> Amount.gbp(400),
            "tax_underpaid_previous_year"          -> Amount.gbp(450)
          )
        ),
        Some(
          Map(
            "paye_scottish_starter_rate"      -> Rate("19%"),
            "paye_scottish_basic_rate"        -> Rate("20%"),
            "paye_scottish_intermediate_rate" -> Rate("21%"),
            "paye_scottish_higher_rate"       -> Rate("41%"),
            "paye_ordinary_rate"              -> Rate("19%"),
            "paye_higher_rate_income_tax"     -> Rate("20%"),
            "paye_basic_rate_income_tax"      -> Rate("21%"),
            "paye_upper_rate"                 -> Rate("41%")
          )
        ),
        None
      )
    ),
    Some(
      DataHolder(
        Some(
          Map(
            "employee_nic_amount"     -> Amount.gbp(70),
            "employer_nic_amount"     -> Amount.gbp(90),
            "total_income_tax_2_nics" -> Amount.gbp(431)
          )
        ),
        None,
        None
      )
    ),
    None,
    None,
    None
  )

  val payeYourIncomeAndTaxesViewModel: PayeYourIncomeAndTaxes =
    PayeYourIncomeAndTaxes(
      taxYear,
      Amount(1000, "GBP"),
      Amount(800, "GBP"),
      Amount(200, "GBP"),
      Amount(100, "GBP"),
      "20"
    )

  val payeGovernmentSpendViewModel: PayeGovernmentSpend = PayeGovernmentSpend(
    taxYear,
    List(
      SpendRow("Welfare", SpendData(Amount(451, "GBP"), 23.5)),
      SpendRow("Health", SpendData(Amount(388, "GBP"), 20.2)),
      SpendRow("StatePensions", SpendData(Amount(246, "GBP"), 12.8)),
      SpendRow("Education", SpendData(Amount(226, "GBP"), 11.8)),
      SpendRow("NationalDebtInterest", SpendData(Amount(102, "GBP"), 5.3)),
      SpendRow("Defence", SpendData(Amount(102, "GBP"), 5.3)),
      SpendRow("Transport", SpendData(Amount(83, "GBP"), 4.3)),
      SpendRow("PublicOrderAndSafety", SpendData(Amount(83, "GBP"), 4.3)),
      SpendRow("BusinessAndIndustry", SpendData(Amount(69, "GBP"), 3.6)),
      SpendRow("GovernmentAdministration", SpendData(Amount(40, "GBP"), 2.1)),
      SpendRow("HousingAndUtilities", SpendData(Amount(31, "GBP"), 1.6)),
      SpendRow("Culture", SpendData(Amount(29, "GBP"), 1.5)),
      SpendRow("Environment", SpendData(Amount(29, "GBP"), 1.5)),
      SpendRow("OverseasAid", SpendData(Amount(23, "GBP"), 1.2)),
      SpendRow("UkContributionToEuBudget", SpendData(Amount(19, "GBP"), 1))
    ),
    totalAmount = Amount(200, "GBP"),
    isScottish = false
  )

  val payeGovernmentSpendViewModel2020: PayeGovernmentSpend = PayeGovernmentSpend(
    2020,
    List(
      SpendRow("Welfare", SpendData(Amount(451, "GBP"), 22.1)),
      SpendRow("Health", SpendData(Amount(388, "GBP"), 20.5)),
      SpendRow("StatePensions", SpendData(Amount(246, "GBP"), 12.4)),
      SpendRow("Education", SpendData(Amount(226, "GBP"), 11.6)),
      SpendRow("NationalDebtInterest", SpendData(Amount(102, "GBP"), 6.9)),
      SpendRow("Defence", SpendData(Amount(102, "GBP"), 5.3)),
      SpendRow("Transport", SpendData(Amount(83, "GBP"), 4.3)),
      SpendRow("PublicOrderAndSafety", SpendData(Amount(83, "GBP"), 4.3)),
      SpendRow("BusinessAndIndustry", SpendData(Amount(69, "GBP"), 3.8)),
      SpendRow("GovernmentAdministration", SpendData(Amount(40, "GBP"), 2.1)),
      SpendRow("HousingAndUtilities", SpendData(Amount(31, "GBP"), 1.8)),
      SpendRow("Culture", SpendData(Amount(29, "GBP"), 1.5)),
      SpendRow("Environment", SpendData(Amount(29, "GBP"), 1.5)),
      SpendRow("OverseasAid", SpendData(Amount(23, "GBP"), 1.1)),
      SpendRow("UkContributionToEuBudget", SpendData(Amount(19, "GBP"), 0.8))
    ),
    totalAmount = Amount(200, "GBP"),
    isScottish = false
  )

  val payeGovernmentSpendViewModel2021: PayeGovernmentSpend = PayeGovernmentSpend(
    2021,
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

  val payeIncomeTaxAndNicsViewModel: PayeIncomeTaxAndNics = PayeIncomeTaxAndNics(
    taxYear,
    List(
      TaxBand("scottish_starter_rate", Amount(2000, "GBP"), Amount(380, "GBP"), Rate("19%")),
      TaxBand("scottish_basic_rate", Amount(10150, "GBP"), Amount(2030, "GBP"), Rate("20%")),
      TaxBand("scottish_intermediate_rate", Amount(19430, "GBP"), Amount(4080, "GBP"), Rate("21%")),
      TaxBand("scottish_higher_rate", Amount(31570, "GBP"), Amount(12943, "GBP"), Rate("41%"))
    ),
    List(
      TaxBand("ordinary_rate", Amount(19430, "GBP"), Amount(4080, "GBP"), Rate("19%")),
      TaxBand("higher_rate_income_tax", Amount(10150, "GBP"), Amount(2030, "GBP"), Rate("20%")),
      TaxBand("basic_rate_income_tax", Amount(2000, "GBP"), Amount(380, "GBP"), Rate("21%")),
      TaxBand("upper_rate", Amount(31570, "GBP"), Amount(12943, "GBP"), Rate("41%"))
    ),
    Amount(19433, "GBP"),
    Amount(18433, "GBP"),
    Amount(20322, "GBP"),
    List(
      AdjustmentRow("less_tax_adjustment_previous_year", Amount.gbp(350)),
      AdjustmentRow("marriage_allowance_received_amount", Amount.gbp(200)),
      AdjustmentRow("married_couples_allowance_adjustment", Amount.gbp(400)),
      AdjustmentRow("tax_underpaid_previous_year", Amount.gbp(450))
    ),
    Amount(70, "GBP"),
    Amount(90, "GBP"),
    Amount(431, "GBP"),
    Amount(2550, "GBP")
  )

  val payeUKIncomeTaxAndNicsViewModel: PayeIncomeTaxAndNics = PayeIncomeTaxAndNics(
    taxYear,
    List.empty,
    List(
      TaxBand("ordinary_rate", Amount(19430, "GBP"), Amount(4080, "GBP"), Rate("19%")),
      TaxBand("higher_rate_income_tax", Amount(10150, "GBP"), Amount(2030, "GBP"), Rate("20%")),
      TaxBand("basic_rate_income_tax", Amount(2000, "GBP"), Amount(380, "GBP"), Rate("21%")),
      TaxBand("upper_rate", Amount(31570, "GBP"), Amount(12943, "GBP"), Rate("41%"))
    ),
    Amount.empty,
    Amount.empty,
    Amount(20322, "GBP"),
    List(
      AdjustmentRow("less_tax_adjustment_previous_year", Amount.gbp(350)),
      AdjustmentRow("marriage_allowance_received_amount", Amount.gbp(200)),
      AdjustmentRow("married_couples_allowance_adjustment", Amount.gbp(400)),
      AdjustmentRow("tax_underpaid_previous_year", Amount.gbp(450))
    ),
    Amount(70, "GBP"),
    Amount(90, "GBP"),
    Amount(431, "GBP"),
    Amount(2550, "GBP")
  )

  val payeEmployeeContributionNicsViewModel: PayeIncomeTaxAndNics = PayeIncomeTaxAndNics(
    taxYear,
    List.empty,
    List.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    List.empty,
    Amount(70, "GBP"),
    Amount(90, "GBP"),
    Amount(431, "GBP"),
    Amount(2550, "GBP")
  )

  val payeEmptyNicsViewModel: PayeIncomeTaxAndNics = PayeIncomeTaxAndNics(
    taxYear,
    List.empty,
    List.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    List.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty
  )

  val payeYourTaxableIncomeViewModel: PayeYourTaxableIncome = PayeYourTaxableIncome(
    taxYear,
    List(
      IncomeTaxRow("self_employment_income", Amount(450, "GBP")),
      IncomeTaxRow("income_from_employment", Amount(550, "GBP")),
      IncomeTaxRow("state_pension", Amount(652, "GBP")),
      IncomeTaxRow("taxable_state_benefits", Amount(751, "GBP")),
      IncomeTaxRow("other_income", Amount(851, "GBP")),
      IncomeTaxRow("benefits_from_employment", Amount(251, "GBP")),
      IncomeTaxRow("total_income_before_tax", Amount(351, "GBP"))
    ),
    Amount(1000, "GBP")
  )

  val incomeData: PayeAtsData = PayeAtsData(
    taxYear,
    None,
    None,
    Some(
      DataHolder(
        Some(
          Map(
            "income_from_employment" -> Amount(4500, "GBP"),
            "state_pension"          -> Amount(1000, "GBP"),
            "other_pension_income"   -> Amount(1000, "GBP"),
            "other_income"           -> Amount(15000, "GBP"),
            "scottish_income_tax"    -> Amount(2500, "GBP")
          )
        ),
        None,
        None
      )
    ),
    None,
    None
  )

  val incomeDataWithoutScottishIncomeTax: PayeAtsData = PayeAtsData(
    taxYear,
    None,
    None,
    Some(
      DataHolder(
        Some(
          Map(
            "income_from_employment" -> Amount(4500, "GBP"),
            "state_pension"          -> Amount(1000, "GBP"),
            "other_pension_income"   -> Amount(1000, "GBP"),
            "other_income"           -> Amount(15000, "GBP")
          )
        ),
        None,
        None
      )
    ),
    None,
    None
  )

  val govSpendingDataFor2022: PayeAtsData = PayeAtsData(
    2022,
    None,
    None,
    None,
    None,
    Some(
      GovernmentSpendingOutputWrapper(
        2022,
        Some(
          Map(
            "UkContributionToEuBudget" -> SpendData(Amount(19.00, "GBP"), 1.00),
            "Welfare"                  -> SpendData(Amount(451.00, "GBP"), 23.5),
            "GovernmentAdministration" -> SpendData(Amount(40.00, "GBP"), 2.10),
            "Education"                -> SpendData(Amount(226.00, "GBP"), 11.80),
            "StatePensions"            -> SpendData(Amount(246.00, "GBP"), 12.80),
            "NationalDebtInterest"     -> SpendData(Amount(102.00, "GBP"), 5.30),
            "Defence"                  -> SpendData(Amount(102.00, "GBP"), 5.30),
            "PublicOrderAndSafety"     -> SpendData(Amount(83.00, "GBP"), 4.30),
            "Transport"                -> SpendData(Amount(83.00, "GBP"), 4.30),
            "BusinessAndIndustry"      -> SpendData(Amount(69.00, "GBP"), 3.60),
            "Culture"                  -> SpendData(Amount(29.00, "GBP"), 1.50),
            "HousingAndUtilities"      -> SpendData(Amount(31.00, "GBP"), 1.60),
            "Environment"              -> SpendData(Amount(29.00, "GBP"), 1.50),
            "OverseasAid"              -> SpendData(Amount(23.00, "GBP"), 1.20),
            "Health"                   -> SpendData(Amount(388.00, "GBP"), 20.2)
          )
        ),
        Amount(200, "GBP"),
        None
      )
    )
  )

  val govSpendingDataFor2020: PayeAtsData = PayeAtsData(
    2020,
    None,
    None,
    None,
    None,
    Some(
      GovernmentSpendingOutputWrapper(
        taxYear,
        Some(
          Map(
            "UkContributionToEuBudget" -> SpendData(Amount(19.00, "GBP"), 0.80),
            "Welfare"                  -> SpendData(Amount(451.00, "GBP"), 22.1),
            "GovernmentAdministration" -> SpendData(Amount(40.00, "GBP"), 2.10),
            "Education"                -> SpendData(Amount(226.00, "GBP"), 11.60),
            "StatePensions"            -> SpendData(Amount(246.00, "GBP"), 12.40),
            "NationalDebtInterest"     -> SpendData(Amount(102.00, "GBP"), 6.90),
            "Defence"                  -> SpendData(Amount(102.00, "GBP"), 5.30),
            "PublicOrderAndSafety"     -> SpendData(Amount(83.00, "GBP"), 4.30),
            "Transport"                -> SpendData(Amount(83.00, "GBP"), 4.30),
            "BusinessAndIndustry"      -> SpendData(Amount(69.00, "GBP"), 3.80),
            "Culture"                  -> SpendData(Amount(29.00, "GBP"), 1.50),
            "HousingAndUtilities"      -> SpendData(Amount(31.00, "GBP"), 1.80),
            "Environment"              -> SpendData(Amount(29.00, "GBP"), 1.50),
            "OverseasAid"              -> SpendData(Amount(23.00, "GBP"), 1.10),
            "Health"                   -> SpendData(Amount(388.00, "GBP"), 20.5)
          )
        ),
        Amount(200, "GBP"),
        None
      )
    )
  )
}
