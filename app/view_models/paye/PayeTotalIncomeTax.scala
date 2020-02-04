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

import utils.GenericViewModel
import view_models.{Amount, Rate}

case class PayeTotalIncomeTax(
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
                               scottishTax: PayeScottishTax,
                               totalIncomeTax: Amount,
                               scottishIncomeTax: Amount,
                               savingsTax: PayeSavingsTax,
                               incomeTaxStatus: String,
                               startingRateForSavingsRateRate: Rate,
                               basicRateIncomeTaxRateRate: Rate,
                               higherRateIncomeTaxRateRate: Rate,
                               additionalRateIncomeTaxRateRate: Rate,
                               ordinaryRateTaxRateRate: Rate,
                               upperRateRateRate: Rate,
                               additionalRateRateRate: Rate,
                               scottishRates: PayeScottishRates,
                               savingsRates: PayeSavingsRates,
                               lessTaxAdjustmentPreviousYear: Amount,
                               taxUnderpaidPreviousYear: Amount,
                               title: String,
                               forename: String,
                               surname: String)
    extends GenericViewModel {
  def isPaye = utr.isEmpty
  def taxYear = year.toString

  def startingRateForSavingsRate = startingRateForSavingsRateRate.percent
  def basicRateIncomeTaxRate = basicRateIncomeTaxRateRate.percent
  def higherRateIncomeTaxRate = higherRateIncomeTaxRateRate.percent
  def additionalRateIncomeTaxRate = additionalRateIncomeTaxRateRate.percent
  def ordinaryRateTaxRate = ordinaryRateTaxRateRate.percent
  def upperRateRate = upperRateRateRate.percent
  def additionalRateRate = additionalRateRateRate.percent
  def taxYearFrom = (year - 1).toString

  def showIncomeTaxTable = startingRateForSavings.nonZero ||
    basicRateIncomeTaxAmount.nonZero ||
    higherRateIncomeTaxAmount.nonZero ||
    additionalRateIncomeTaxAmount.nonZero

  def showDividendsTable = ordinaryRate.nonZero || upperRate.nonZero || additionalRate.nonZero

  def showAdjustmentsTable = otherAdjustmentsIncreasing.nonZero || otherAdjustmentsReducing.nonZero || marriageAllowanceReceivedAmount.nonZero

  def restOfUkTotal: Amount = {
    Amount(savingsTax.savingsLowerRateTax.amount
      + savingsTax.savingsHigherRateTax.amount
      + savingsTax.savingsAdditionalRateTax.amount
      + ordinaryRateAmount.amount
      + upperRateAmount.amount
      + additionalRateAmount.amount, savingsTax.savingsLowerRateTaxAmount.currency)
  }
}

case class PayeScottishTax(
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

object PayeScottishTax {

  val empty: PayeScottishTax =
    PayeScottishTax(
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

case class PayeSavingsTax(
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

object PayeSavingsTax {

  val empty: PayeSavingsTax =
    PayeSavingsTax(
      Amount.empty,
      Amount.empty,
      Amount.empty,
      Amount.empty,
      Amount.empty,
      Amount.empty
    )
}

case class PayeScottishRates(
  scottishStarterRate: Rate,
  scottishBasicRate: Rate,
  scottishIntermediateRate: Rate,
  scottishHigherRate: Rate,
  scottishAdditionalRate: Rate
)

object PayeScottishRates {

  val empty: PayeScottishRates =
    PayeScottishRates(
      Rate.empty,
      Rate.empty,
      Rate.empty,
      Rate.empty,
      Rate.empty
    )
}

case class PayeSavingsRates(
  savingsLowerRate: Rate,
  savingsHigherRate: Rate,
  savingsAdditionalRate: Rate
)

object PayeSavingsRates {

  val empty: PayeSavingsRates =
    PayeSavingsRates(
      Rate.empty,
      Rate.empty,
      Rate.empty
    )
}