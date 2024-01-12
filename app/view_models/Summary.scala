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

package view_models

import utils.ViewUtils
import utils.{GenericViewModel, TextGenerator}

case class Summary(
  year: Int,
  utr: String,
  employeeNicAmount: Amount,
  totalIncomeTaxAndNics: Amount,
  yourTotalTax: Amount,
  totalTaxFree: Amount,
  totalTaxFreeAllowance: Amount,
  yourIncomeBeforeTax: Amount,
  totalIncomeTaxAmount: Amount,
  totalCapitalGainsTax: Amount,
  taxableGains: Amount,
  cgTaxPerCurrencyUnit: Amount,
  nicsAndTaxPerCurrencyUnit: Amount,
  totalCgTaxRate: Rate,
  nicsAndTaxRate: Rate,
  title: String,
  forename: String,
  surname: String
) extends GenericViewModel {

  def taxYearInterval: String   = taxYearFrom + "-" + taxYearTo.substring(2)
  def taxYearIntervalTo: String = taxYearFrom + " to " + taxYearTo

  def hasTotalIncomeTaxAmount: Boolean = !totalIncomeTaxAmount.isZeroOrLess
  def hasTotalCapitalGains: Boolean    = !totalCapitalGainsTax.isZero
  def hasEmployeeNicAmount: Boolean    = !employeeNicAmount.isZero

  def nonNegativeTotalIncomeTaxAndNics: Amount = {
    val viewUtils     = new ViewUtils
    val nonNegativeIT = viewUtils.positiveOrZero(totalIncomeTaxAmount)
    val total         = nonNegativeIT.amount + employeeNicAmount.amount
    Amount(total, totalIncomeTaxAndNics.currency)
  }

  def yourTotalTaxTextKey: String =
    if (hasTotalIncomeTaxAmount && hasEmployeeNicAmount) {
      "ats.summary.tax_and_nics.title"
    } else if (!hasTotalIncomeTaxAmount && hasEmployeeNicAmount) {
      "ats.summary.nics.title"
    } else {
      "ats.summary.tax.title"
    }

  def yourTotalTaxTextKeys: (String, List[String]) =
    TextGenerator.createOnScreenText(hasTotalIncomeTaxAmount, hasTotalCapitalGains, hasEmployeeNicAmount)

  def hasTaxableGains: Boolean = !taxableGains.isZero
  def taxYearTo: String        = year.toString
  def taxYearFrom: String      = (year - 1).toString
}
