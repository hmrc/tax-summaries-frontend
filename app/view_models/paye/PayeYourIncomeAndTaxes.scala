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
import view_models.Amount

case class PayeYourIncomeAndTaxes(
  taxYear: Int,
  incomeBeforeTax: Amount,
  taxableIncome: Amount,
  totalIncomeTax: Amount,
  incomeAfterTaxNics: Amount,
  averageTaxRate: String) extends TaxYearFormatting

object PayeYourIncomeAndTaxes {

  val taxYear: Int = ApplicationConfig.payeYear

  def buildViewModelEither(payeAtsData: PayeAtsData): Either[String, PayeYourIncomeAndTaxes] = {
    val taxableIncome = for {
      allowanceData <- payeAtsData.allowance_data.toRight("Missing allowance_data in payeAtsData").right
      payload <- allowanceData.payload.toRight("Missing payload in allowance_data").right
      income <- if (payload.getOrElse("total_tax_free_amount", Amount.empty) == Amount.empty) {
        payload.get("personal_tax_free_amount").toRight("Missing personal_tax_free_amount in payload").right
      } else {
        payload.get("total_tax_free_amount").toRight("Missing total_tax_free_amount in payload").right
      }
    } yield {
      income
    }
    val totalIncomeTax: Either[String, Amount] = payeAtsData.gov_spending.toRight("Missing gov_spending in payeAtsData").right.map { govSpending =>
      govSpending.totalAmount
    }
    for {
      taxableIncome <- taxableIncome.right
      totalIncomeTax <- totalIncomeTax.right
      summaryData <- payeAtsData.summary_data.toRight("Missing summary_data in payeAtsData").right
      rates <- summaryData.rates.toRight("Missing rates in summary_data").right
      averageTaxRate <- rates.get("nics_and_tax_rate").toRight("Missing nics_and_tax_rate in rates").right
      payload <- summaryData.payload.toRight("Missing payload in summary_data").right
      incomeBeforeTax <- payload.get("total_income_before_tax").toRight("Missing total_income_before_tax in payload").right
      incomeAfterTaxNics <- payload.get("income_after_tax_and_nics").toRight("Missing income_after_tax_and_nics in payload").right
    } yield {
      PayeYourIncomeAndTaxes(
        taxYear,
        incomeBeforeTax,
        taxableIncome,
        totalIncomeTax,
        incomeAfterTaxNics,
        averageTaxRate.percent.replaceAll("%", "")
      )
    }
  }
}
