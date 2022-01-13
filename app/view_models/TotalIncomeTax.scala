/*
 * Copyright 2022 HM Revenue & Customs
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

import utils.GenericViewModel

case class TotalIncomeTax(
  year: Int,
  utr: String,
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
  surname: String)
    extends GenericViewModel {

  val isScottishTaxPayer: Boolean = (incomeTaxStatus == "0002")
  val isWelshTaxPayer: Boolean = (incomeTaxStatus == "0003")

  def taxYear = year.toString

  def startingRateForSavingsRate = startingRateForSavingsRateRate.percent
  def basicRateIncomeTaxRate = basicRateIncomeTaxRateRate.percent
  def higherRateIncomeTaxRate = higherRateIncomeTaxRateRate.percent
  def additionalRateIncomeTaxRate = additionalRateIncomeTaxRateRate.percent
  def ordinaryRateTaxRate = ordinaryRateTaxRateRate.percent
  def upperRateRate = upperRateRateRate.percent
  def additionalRateRate = additionalRateRateRate.percent
  def taxYearFrom = (year - 1).toString

  def showIncomeTaxTable =
    startingRateForSavings.nonZero ||
      basicRateIncomeTaxAmount.nonZero ||
      higherRateIncomeTaxAmount.nonZero ||
      additionalRateIncomeTaxAmount.nonZero

  def showDividendsTable = ordinaryRate.nonZero || upperRate.nonZero || additionalRate.nonZero

  def showAdjustmentsTable =
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
  scottishAdditionalIncomeTax: Amount,
  scottishAdditionalIncomeTaxAmount: Amount,
  scottishTotalTax: Amount
) {

  def hasTax: Boolean =
    scottishStarterIncomeTax.nonZero ||
      scottishBasicIncomeTax.nonZero ||
      scottishIntermediateIncomeTax.nonZero ||
      scottishHigherIncomeTax.nonZero ||
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
  scottishAdditionalRate: Rate
)

object ScottishRates {

  val empty: ScottishRates =
    ScottishRates(
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
