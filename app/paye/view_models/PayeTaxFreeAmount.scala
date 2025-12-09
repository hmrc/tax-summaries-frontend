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

package paye.view_models

import common.view_models.{Amount, TaxYearFormatting}
import paye.models.PayeAtsData

case class PayeTaxFreeAmount(
  taxYear: Int,
  adjustmentRows: List[AmountRow],
  totalTaxFreeAmount: Amount,
  summaryRows: List[AmountRow],
  liableTaxAmount: Amount
) extends TaxYearFormatting

object PayeTaxFreeAmount {

  private val adjustments = List(
    "personal_tax_free_amount",
    "marriage_allowance_transferred_amount",
    "other_allowances_amount"
  )

  def apply(payeAtsData: PayeAtsData): PayeTaxFreeAmount = {
    val totalTaxFreeAmount    =
      payeAtsData.summary_data.flatMap(_.payload).flatMap(_.get("total_tax_free_amount")).getOrElse(Amount.empty)
    val liableTaxAmount       =
      payeAtsData.summary_data.flatMap(_.payload).flatMap(_.get("liable_tax_amount")).getOrElse(Amount.empty)
    val totalIncomeBeforeTax  =
      payeAtsData.summary_data.flatMap(_.payload).flatMap(_.get("total_income_before_tax")).getOrElse(Amount.empty)
    val personalTaxFreeAmount =
      payeAtsData.allowance_data.flatMap(_.payload).flatMap(_.get("personal_tax_free_amount")).getOrElse(Amount.empty)

    val adjustmentRows = (for {
      allowanceData <- payeAtsData.allowance_data
      payload       <- allowanceData.payload
    } yield adjustments
      .map { adjustment =>
        AmountRow(adjustment, payload.getOrElse(adjustment, Amount.empty))
      }
      .filter(_.amount.isValueNotEqual(Amount.empty))).getOrElse(List.empty)

    val summaryRows = List(
      AmountRow("income_before_tax", totalIncomeBeforeTax),
      AmountRow(
        "tax_free_amount",
        if (totalTaxFreeAmount.isValueEqual(Amount.empty)) personalTaxFreeAmount else totalTaxFreeAmount
      )
    )

    PayeTaxFreeAmount(payeAtsData.taxYear, adjustmentRows, totalTaxFreeAmount, summaryRows, liableTaxAmount)
  }
}

case class AmountRow(label: String, amount: Amount)
