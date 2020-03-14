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

package services.atsData

import models._
import view_models.paye.{AdjustmentRow, PayeGovernmentSpend, PayeIncomeTaxAndNics, PayeYourIncomeAndTaxes, SpendRow}
import view_models.{Amount, Rate}

object PayeAtsTestData {

  val govSpendingData = PayeAtsData(
    2018,
    None,
    None,
    None,
    None,
    Some(
      GovernmentSpendingOutputWrapper(
        2018,
        Some(
          Map(
            "UkContributionToEuBudget" -> SpendData(Amount(19.00, "GBP"), 1.00),
            "Welfare" -> SpendData(Amount(451.00, "GBP"), 23.5),
            "GovernmentAdministration" -> SpendData(Amount(40.00, "GBP"), 2.10),
            "Education" -> SpendData(Amount(226.00, "GBP"), 11.80),
            "StatePensions" -> SpendData(Amount(246.00, "GBP"), 12.80),
            "NationalDebtInterest" -> SpendData(Amount(98.00, "GBP"), 5.10),
            "Defence" -> SpendData(Amount(102.00, "GBP"), 5.30),
            "PublicOrderAndSafety" -> SpendData(Amount(83.00, "GBP"), 4.30),
            "Transport" -> SpendData(Amount(83.00, "GBP"), 4.30),
            "BusinessAndIndustry" -> SpendData(Amount(69.00, "GBP"), 3.60),
            "Culture" -> SpendData(Amount(29.00, "GBP"), 1.50),
            "HousingAndUtilities" -> SpendData(Amount(31.00, "GBP"), 1.60),
            "Environment" -> SpendData(Amount(29.00, "GBP"), 1.50),
            "OverseasAid" ->  SpendData(Amount(23.00, "GBP"), 1.20),
            "Health" -> SpendData(Amount(388.00, "GBP"), 20.2)
          )
        ),
        Amount(200,"GBP"),
        None
      )
    )
  )

  val yourIncomeAndTaxesData = PayeAtsData(
    2018,
    None,
    Some(
      DataHolder(
        Some(
          Map(
            "employee_nic_amount" -> Amount(100, "GBP"),
            "employer_nic_amount" -> Amount(150, "GBP"),
            "total_income_tax_and_nics" -> Amount(200, "GBP"),
            "your_total_tax" -> Amount(300, "GBP"),
            "personal_tax_free_amount" -> Amount(400, "GBP"),
            "total_tax_free_amount" -> Amount(400, "GBP"),
            "total_income_before_tax" -> Amount(500, "GBP"),
            "total_income_tax" -> Amount(600, "GBP"),
            "total_cg_tax" -> Amount(700, "GBP"),
            "taxable_gains" -> Amount(800, "GBP"),
            "cg_tax_per_currency_unit" -> Amount(900, "GBP"),
            "nics_and_tax_per_currency_unit" -> Amount(1000, "GBP"),
            "income_after_tax_and_nics" -> Amount(1100, "GBP"),
            "nics_and_tax_rate_amount" -> Amount(20, "PERCENT")
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
            "other_allowances_amount" -> Amount(300, "GBP"),
            "total_tax_free_amount" -> Amount(9740, "GBP")
          )
        ),
        None,
        None
      )
    ),
    Some(
      GovernmentSpendingOutputWrapper(
        2018,
        None,
        Amount(200,"GBP"),
        None
      )
    )
  )

  val malformedYourIncomeAndTaxesData = PayeAtsData(
    2018,
    None,
    Some(
      DataHolder(
        Some(
          Map(
            "employee_nic_amount" -> Amount(100, "GBP"),
            "employer_nic_amount" -> Amount(150, "GBP"),
            "total_income_tax_and_nics" -> Amount(200, "GBP"),
            "your_total_tax" -> Amount(300, "GBP"),
            "personal_tax_free_amount" -> Amount(400, "GBP"),
            "total_tax_free_amount" -> Amount(400, "GBP"),
            "total_income_tax" -> Amount(600, "GBP"),
            "total_cg_tax" -> Amount(700, "GBP"),
            "taxable_gains" -> Amount(800, "GBP"),
            "cg_tax_per_currency_unit" -> Amount(900, "GBP"),
            "nics_and_tax_per_currency_unit" -> Amount(1000, "GBP"),
            "income_after_tax_and_nics" -> Amount(1100, "GBP"),
            "nics_and_tax_rate_amount" -> Amount(20, "PERCENT")
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
            "other_allowances_amount" -> Amount(300, "GBP"),
            "total_tax_free_amount" -> Amount(9740, "GBP")
          )
        ),
        None,
        None
      )
    ),
    Some(
      GovernmentSpendingOutputWrapper(
        2018,
        None,
        Amount(200,"GBP"),
        None
      )
    )
  )

  val missingYourIncomeAndTaxesData = PayeAtsData(
    2018,
    None,
    None,
    None,
    Some(
      DataHolder(
        Some(
          Map(
            "personal_tax_free_amount" -> Amount(9440, "GBP"),
            "other_allowances_amount" -> Amount(300, "GBP"),
            "total_tax_free_amount" -> Amount(9740, "GBP")
          )
        ),
        None,
        None
      )
    ),
    Some(
      GovernmentSpendingOutputWrapper(
        2018,
        None,
        Amount(200,"GBP"),
        None
      )
    )
  )

  val YourIncomeAndTaxesDataWithMissingTotalTaxFreeAmount = PayeAtsData(
    2018,
    None,
    Some(
      DataHolder(
        Some(
          Map(
            "employee_nic_amount" -> Amount(100, "GBP"),
            "employer_nic_amount" -> Amount(150, "GBP"),
            "total_income_tax_and_nics" -> Amount(200, "GBP"),
            "your_total_tax" -> Amount(300, "GBP"),
            "personal_tax_free_amount" -> Amount(800, "GBP"),
            "total_income_before_tax" -> Amount(500, "GBP"),
            "total_income_tax" -> Amount(600, "GBP"),
            "total_cg_tax" -> Amount(700, "GBP"),
            "taxable_gains" -> Amount(800, "GBP"),
            "cg_tax_per_currency_unit" -> Amount(900, "GBP"),
            "nics_and_tax_per_currency_unit" -> Amount(1000, "GBP"),
            "income_after_tax_and_nics" -> Amount(1100, "GBP"),
            "nics_and_tax_rate_amount" -> Amount(20, "PERCENT")
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
            "other_allowances_amount" -> Amount(300, "GBP"),
            "total_tax_free_amount" -> Amount(9740, "GBP")
          )
        ),
        None,
        None
      )
    ),
    Some(
      GovernmentSpendingOutputWrapper(
        2018,
        None,
        Amount(200,"GBP"),
        None
      )
    )
  )

  val YourIncomeAndTaxesDataWithMissingEmployeeNicAmount = PayeAtsData(
    2018,
    None,
    Some(
      DataHolder(
        Some(
          Map(
            "employer_nic_amount" -> Amount(150, "GBP"),
            "total_income_tax_and_nics" -> Amount(200, "GBP"),
            "your_total_tax" -> Amount(300, "GBP"),
            "personal_tax_free_amount" -> Amount(800, "GBP"),
            "total_income_before_tax" -> Amount(500, "GBP"),
            "total_income_tax" -> Amount(600, "GBP"),
            "total_cg_tax" -> Amount(700, "GBP"),
            "taxable_gains" -> Amount(800, "GBP"),
            "cg_tax_per_currency_unit" -> Amount(900, "GBP"),
            "nics_and_tax_per_currency_unit" -> Amount(1000, "GBP"),
            "income_after_tax_and_nics" -> Amount(1100, "GBP"),
            "nics_and_tax_rate_amount" -> Amount(20, "PERCENT")
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
            "other_allowances_amount" -> Amount(300, "GBP"),
            "total_tax_free_amount" -> Amount(9740, "GBP")
          )
        ),
        None,
        None
      )
    ),
    Some(
      GovernmentSpendingOutputWrapper(
        2018,
        None,
        Amount(200,"GBP"),
        None
      )
    )
  )

  val totalIncomeTaxData = PayeAtsData(
    2018,
    Some(
      DataHolder(
        Some(
          Map(
            "scottish_starter_rate_amount" -> Amount.gbp(380),
            "scottish_starter_rate" -> Amount.gbp(2000),
            "scottish_basic_rate_amount" -> Amount.gbp(2030),
            "scottish_basic_rate" -> Amount.gbp(10150),
            "scottish_intermediate_rate_amount" -> Amount.gbp(4080),
            "scottish_intermediate_rate" -> Amount.gbp(19430),
            "scottish_higher_rate_amount" -> Amount.gbp(12943),
            "scottish_higher_rate" -> Amount.gbp(31570),
            "scottish_total_tax" -> Amount.gbp(19433),

            "basic_rate_income_tax_amount" -> Amount.gbp(480),
            "basic_rate_income_tax" -> Amount.gbp(3000),
            "higher_rate_income_tax_amount" -> Amount.gbp(3030),
            "higher_rate_income_tax" -> Amount.gbp(20150),
            "ordinary_rate_amount" -> Amount.gbp(5080),
            "ordinary_rate" -> Amount.gbp(29430),
            "upper_rate_amount" -> Amount.gbp(22943),
            "upper_rate" -> Amount.gbp(41570),
            "total_UK_income_tax" -> Amount.gbp(20224),
            "total_income_tax_2" -> Amount.gbp(10477),

            "less_tax_adjustment_previous_year" -> Amount.gbp(350),
            "marriage_allowance_received_amount" -> Amount.gbp(200),
            "married_couples_allowance_adjustment" -> Amount.gbp(400),
            "tax_underpaid_previous_year" -> Amount.gbp(450)
          )
        ),
        Some(
          Map(
            "paye_scottish_starter_rate" -> Rate("19%"),
            "paye_scottish_basic_rate" -> Rate("20%"),
            "paye_scottish_intermediate_rate" -> Rate("21%"),
            "paye_scottish_higher_rate" -> Rate("41%"),

            "paye_ordinary_rate" -> Rate("19%"),
            "paye_higher_rate_income_tax" -> Rate("20%"),
            "paye_basic_rate_income_tax" -> Rate("21%"),
            "paye_upper_rate" -> Rate("41%")
          )
        ), None
      )
    ),
    None, None, None, None
  )


  val payeYourIncomeAndTaxesViewModel = PayeYourIncomeAndTaxes(2018,Amount(1000,"GBP"),Amount(800,"GBP"),Amount(200,"GBP"),Amount(100,"GBP"),"20")


  val payeGovernmentSpendViewModel =  PayeGovernmentSpend(2018, List(
    SpendRow("Welfare", SpendData(Amount(451, "GBP"),23.5)),
    SpendRow("Health", SpendData(Amount(388, "GBP"),20.2)),
    SpendRow("StatePensions", SpendData(Amount(246, "GBP"),12.8)),
    SpendRow("Education", SpendData(Amount(226, "GBP"),11.8)),
    SpendRow("NationalDebtInterest", SpendData(Amount(98, "GBP"),5.1)),
    SpendRow("Defence", SpendData(Amount(102, "GBP"),5.3)),
    SpendRow("Transport", SpendData(Amount(83, "GBP"),4.3)),
    SpendRow("PublicOrderAndSafety", SpendData(Amount(83, "GBP"),4.3)),
    SpendRow("BusinessAndIndustry", SpendData(Amount(69, "GBP"),3.6)),
    SpendRow("GovernmentAdministration", SpendData(Amount(40, "GBP"),2.1)),
    SpendRow("HousingAndUtilities", SpendData(Amount(31, "GBP"),1.6)),
    SpendRow("Environment", SpendData(Amount(29, "GBP"),1.5)),
    SpendRow("Culture", SpendData(Amount(29, "GBP"),1.5)),
    SpendRow("OverseasAid", SpendData(Amount(23, "GBP"),1.2)),
    SpendRow("UkContributionToEuBudget", SpendData(Amount(19, "GBP"),1)))
    , totalAmount = Amount(200,"GBP"), isScottish = false)

  val payeIncomeTaxAndNicsViewModel = PayeIncomeTaxAndNics(2018,
    List(TaxBand("scottish_starter_rate", Amount(2000, "GBP"),
      Amount(380, "GBP"), Rate("19%")),
      TaxBand("scottish_basic_rate", Amount(10150, "GBP"),
        Amount(2030, "GBP"), Rate("20%")),
      TaxBand("scottish_intermediate_rate", Amount(19430, "GBP"),
        Amount(4080, "GBP"), Rate("21%")),
      TaxBand("scottish_higher_rate", Amount(31570, "GBP"),
        Amount(12943, "GBP"), Rate("41%"))),
    List(TaxBand("ordinary_rate", Amount(19430, "GBP"),
      Amount(4080, "GBP"), Rate("19%")),
      TaxBand("higher_rate_income_tax", Amount(10150, "GBP"),
        Amount(2030, "GBP"), Rate("20%")),
      TaxBand("basic_rate_income_tax", Amount(2000, "GBP"),
        Amount(380, "GBP"), Rate("21%")),
      TaxBand("upper_rate", Amount(31570, "GBP"),
        Amount(12943, "GBP"), Rate("41%"))),
    Amount(19433, "GBP"),  Amount(18433, "GBP"),Amount(20322, "GBP"),
    List(AdjustmentRow("less_tax_adjustment_previous_year", Amount.gbp(350)),
      AdjustmentRow("marriage_allowance_received_amount", Amount.gbp(200)),
      AdjustmentRow("married_couples_allowance_adjustment", Amount.gbp(400)),
      AdjustmentRow("tax_underpaid_previous_year", Amount.gbp(450)))
  )

  val payeUKIncomeTaxAndNicsViewModel = PayeIncomeTaxAndNics(2018,
    List.empty,
    List(TaxBand("ordinary_rate", Amount(19430, "GBP"),
      Amount(4080, "GBP"), Rate("19%")),
      TaxBand("higher_rate_income_tax", Amount(10150, "GBP"),
        Amount(2030, "GBP"), Rate("20%")),
      TaxBand("basic_rate_income_tax", Amount(2000, "GBP"),
        Amount(380, "GBP"), Rate("21%")),
      TaxBand("upper_rate", Amount(31570, "GBP"),
        Amount(12943, "GBP"), Rate("41%"))),
    Amount.empty,  Amount.empty,Amount(20322, "GBP"),
    List(AdjustmentRow("less_tax_adjustment_previous_year", Amount.gbp(350)),
      AdjustmentRow("marriage_allowance_received_amount", Amount.gbp(200)),
      AdjustmentRow("married_couples_allowance_adjustment", Amount.gbp(400)),
      AdjustmentRow("tax_underpaid_previous_year", Amount.gbp(450)))
  )
}

