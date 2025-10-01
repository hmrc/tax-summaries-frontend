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

import utils.{GenericViewModel, ViewUtils}

case class IncomeTaxAndNI(
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
  startingRateForSavings: Amount,
  startingRateForSavingsAmount: Amount,
  basicRateIncomeTax: Amount,
  basicRateIncomeTaxAmount: Amount,
  higherRateIncomeTax: Amount,
  higherRateIncomeTaxAmount: Amount,
  additionalRateIncomeTax: Amount,
  additionalRateIncomeTaxAmount: Amount,
  ordinaryRate: Amount,
  ordinaryRateAmount: Amount,
  upperRate: Amount,
  upperRateAmount: Amount,
  additionalRate: Amount,
  additionalRateAmount: Amount,
  otherAdjustmentsIncreasing: Amount,
  marriageAllowanceReceivedAmount: Amount,
  otherAdjustmentsReducing: Amount,
  scottishTax: ScottishTax,
  totalIncomeTax: Amount,
  scottishIncomeTax: Amount,
  welshIncomeTax: Amount,
  savingsTax: SavingsTax,
  incomeTaxStatus: String,
  startingRateForSavingsRateRate: Rate,
  basicRateIncomeTaxRateRate: Rate,
  higherRateIncomeTaxRateRate: Rate,
  additionalRateIncomeTaxRateRate: Rate,
  ordinaryRateTaxRateRate: Rate,
  upperRateRateRate: Rate,
  additionalRateRateRate: Rate,
  scottishRates: ScottishRates,
  savingsRates: SavingsRates,
  title: String,
  forename: String,
  surname: String
) extends GenericViewModel {

  def taxYearTo: String   = year.toString
  def taxYearFrom: String = (year - 1).toString

  val isScottishTaxPayer: Boolean = incomeTaxStatus == "0002"
  val isWelshTaxPayer: Boolean    = incomeTaxStatus == "0003"

  def startingRateForSavingsRate: String  = startingRateForSavingsRateRate.percent
  def basicRateIncomeTaxRate: String      = basicRateIncomeTaxRateRate.percent
  def higherRateIncomeTaxRate: String     = higherRateIncomeTaxRateRate.percent
  def additionalRateIncomeTaxRate: String = additionalRateIncomeTaxRateRate.percent
  def ordinaryRateTaxRate: String         = ordinaryRateTaxRateRate.percent
  def upperRateRate: String               = upperRateRateRate.percent
  def additionalRateRate: String          = additionalRateRateRate.percent

  def showIncomeTaxTable: Boolean =
    startingRateForSavings.nonZero ||
      basicRateIncomeTaxAmount.nonZero ||
      higherRateIncomeTaxAmount.nonZero ||
      additionalRateIncomeTaxAmount.nonZero

  def showDividendsTable: Boolean = ordinaryRate.nonZero || upperRate.nonZero || additionalRate.nonZero

  def showAdjustmentsTable: Boolean =
    otherAdjustmentsIncreasing.nonZero || otherAdjustmentsReducing.nonZero || marriageAllowanceReceivedAmount.nonZero

  def scottishAndRestOfUkTotal: Amount =
    Amount(
      savingsTax.savingsLowerRateTax.amount
        + savingsTax.savingsHigherRateTax.amount
        + savingsTax.savingsAdditionalRateTax.amount
        + ordinaryRateAmount.amount
        + upperRateAmount.amount
        + additionalRateAmount.amount,
      savingsTax.savingsLowerRateTaxAmount.currency
    )

  def nonNegativeTotalIncomeTaxAndNics: Amount = {
    val viewUtils     = new ViewUtils
    val nonNegativeIT = viewUtils.positiveOrZero(totalIncomeTaxAmount)
    val total         = nonNegativeIT.amount + employeeNicAmount.amount
    Amount(total, totalIncomeTaxAndNics.currency)
  }
}

case class ScottishTax(
  scottishStarterIncomeTax: Amount,
  scottishStarterIncomeTaxAmount: Amount,
  scottishBasicIncomeTax: Amount,
  scottishBasicIncomeTaxAmount: Amount,
  scottishIntermediateIncomeTax: Amount,
  scottishIntermediateIncomeTaxAmount: Amount,
  scottishHigherIncomeTax: Amount,
  scottishHigherIncomeTaxAmount: Amount,
  scottishAdvancedIncomeTax: Amount,
  scottishAdvancedIncomeTaxAmount: Amount,
  scottishAdditionalIncomeTax: Amount,
  scottishAdditionalIncomeTaxAmount: Amount,
  scottishTopIncomeTax: Amount,
  scottishTopIncomeTaxAmount: Amount,
  scottishTotalTax: Amount
) {

  def hasTax: Boolean =
    scottishStarterIncomeTax.nonZero ||
      scottishBasicIncomeTax.nonZero ||
      scottishIntermediateIncomeTax.nonZero ||
      scottishHigherIncomeTax.nonZero ||
      scottishAdvancedIncomeTaxAmount.nonZero ||
      scottishAdditionalIncomeTax.nonZero ||
      scottishTotalTax.nonZero
}

object ScottishTax {

  val empty: ScottishTax =
    ScottishTax(
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
      Amount.empty
    )
}

case class SavingsTax(
  savingsLowerRateTax: Amount,
  savingsLowerRateTaxAmount: Amount,
  savingsHigherRateTax: Amount,
  savingsHigherRateTaxAmount: Amount,
  savingsAdditionalRateTax: Amount,
  savingsAdditionalRateTaxAmount: Amount
) {

  val hasTax: Boolean =
    savingsLowerRateTax.nonZero ||
      savingsHigherRateTax.nonZero ||
      savingsAdditionalRateTax.nonZero
}

object SavingsTax {

  val empty: SavingsTax =
    SavingsTax(
      Amount.empty,
      Amount.empty,
      Amount.empty,
      Amount.empty,
      Amount.empty,
      Amount.empty
    )
}

case class ScottishRates(
  scottishStarterRate: Rate,
  scottishBasicRate: Rate,
  scottishIntermediateRate: Rate,
  scottishHigherRate: Rate,
  scottishAdditionalRate: Rate,
  scottishAdvancedRate: Rate,
  scottishTopRate: Rate
)

object ScottishRates {

  val empty: ScottishRates =
    ScottishRates(
      Rate.empty,
      Rate.empty,
      Rate.empty,
      Rate.empty,
      Rate.empty,
      Rate.empty,
      Rate.empty
    )
}

case class SavingsRates(
  savingsLowerRate: Rate,
  savingsHigherRate: Rate,
  savingsAdditionalRate: Rate
)

object SavingsRates {

  val empty: SavingsRates =
    SavingsRates(
      Rate.empty,
      Rate.empty,
      Rate.empty
    )
}
