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

package view_models.paye

import config.ApplicationConfig
import models.PayeAtsData
import view_models.{Amount, Rate}

case class PayeYourIncomeAndTaxes(
  taxYear: Int,
  incomeBeforeTax: Amount,
  taxableIncome: Amount,
  totalIncomeTax: Amount,
  incomeAfterTaxNics: Amount,
  averageTaxRate: String) extends TaxYearFormatting

object PayeYourIncomeAndTaxes {

  val taxYear: Int = ApplicationConfig.payeYear

  def buildViewModel(payeAtsData: PayeAtsData): Option[PayeYourIncomeAndTaxes] = {

   val taxableIncome = payeAtsData.allowance_data.flatMap{ allowanceData =>
      allowanceData.payload.map(payload=>
        if (payload("total_tax_free_amount") == Amount.empty) payload("personal_tax_free_amount") else payload("total_tax_free_amount")
      )
    }.get

    val totalIncomeTax = payeAtsData.gov_spending.map(govSpendingData => govSpendingData.totalAmount).get

    payeAtsData.summary_data.flatMap {
      summaryData => {
        val averageTaxRate: Rate = summaryData.rates.map(rates => rates("nics_and_tax_rate")).get
        summaryData.payload.map(
          payload => {
            PayeYourIncomeAndTaxes(
              taxYear,
              payload("total_income_before_tax"),
              taxableIncome ,
              totalIncomeTax,
              payload("income_after_tax_and_nics"),
              averageTaxRate.percent.replaceAll("%", ""))
          }
        )
      }
    }
  }
}
