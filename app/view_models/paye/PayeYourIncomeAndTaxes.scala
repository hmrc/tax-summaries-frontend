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

import models.PayeAtsData
import view_models.{Amount, Rate}

case class PayeYourIncomeAndTaxes(
  taxYear: Int,
  employeeContributions: Boolean,
  incomeBeforeTax: Amount,
  taxableIncome: Option[Amount],
  totalIncomeTax: Amount,
  totalIncomeTaxNics: Amount,
  incomeAfterTaxNics: Amount,
  averageTaxRate: Rate,
  taxFreeAmount: Amount) extends TaxYearFormatting

object PayeYourIncomeAndTaxes {

  def buildViewModel(payeAtsData: PayeAtsData): PayeYourIncomeAndTaxes = {
    val model = payeAtsData.summary_data.flatMap {
      summaryData => {

        val averageTaxRate: Rate = summaryData.rates.map(
          rates =>
            rates("nics_and_tax_rate")
        ).get

        summaryData.payload.map(
          payload => {
            PayeYourIncomeAndTaxes(
              2019,
              payload.get("employee_nic_amount").isDefined,
              payload("total_income_before_tax"),
              Some(payload("total_tax_free_amount")),
              payload("total_income_tax"),
                payload("total_income_tax_and_nics"),
              payload("income_after_tax_and_nics"),
              averageTaxRate,
              payload("total_tax_free_amount"))
          }
        )
      }
    }
    model.getOrElse(PayeYourIncomeAndTaxes(1, false, Amount(123, ""), None, Amount(123, ""), Amount(123, ""), Amount(123, ""), Rate(""), Amount(123, "")))
  }
}
