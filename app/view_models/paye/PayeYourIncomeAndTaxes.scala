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

package view_models.paye

import models.PayeAtsData
import view_models.{Amount, Rate}

case class PayeYourIncomeAndTaxes(
  taxYear: Int,
  incomeBeforeTax: Amount,
  taxableIncome: Amount,
  totalIncomeTax: Amount,
  incomeAfterTaxNics: Amount,
  averageTaxRate: String
) extends TaxYearFormatting

object PayeYourIncomeAndTaxes {

  def buildViewModel(payeAtsData: PayeAtsData, taxYear: Int): Option[PayeYourIncomeAndTaxes] = {

    val taxableIncome = payeAtsData.allowance_data.flatMap { allowanceData =>
      allowanceData.payload.flatMap { payload =>
        payload.get("total_tax_free_amount") match {
          case Some(amount) if amount == Amount.empty => payload.get("personal_tax_free_amount")
          case _                                      => payload.get("total_tax_free_amount")
        }
      }
    }

    val totalIncomeTax = payeAtsData.gov_spending.map(govSpendingData => govSpendingData.totalAmount)

    payeAtsData.summary_data.flatMap { summaryData =>
      val averageTaxRate = summaryData.rates.map(rates => rates("nics_and_tax_rate"))
      summaryData.payload.map { payload =>
        PayeYourIncomeAndTaxes(
          taxYear,
          Amount(payload.get("total_income_before_tax")),
          Amount(taxableIncome),
          Amount(totalIncomeTax),
          Amount(payload.get("income_after_tax_and_nics")),
          averageTaxRate.getOrElse(Rate.empty).percent.replaceAll("%", "")
        )
      }
    }
  }
}
