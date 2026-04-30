/*
 * Copyright 2026 HM Revenue & Customs
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

package sa.view_models

import play.api.i18n.Messages
import common.view_models.Amount
import uk.gov.hmrc.govukfrontend.views.Aliases.{HtmlContent, TableRow}

final case class IncomeTaxDetailsTableViewModel(
  incomeTaxRows: Seq[Seq[TableRow]],
  dividendRows: Seq[Seq[TableRow]],
  totalUkIncomeTaxRows: Seq[Seq[TableRow]],
  adjustmentRows: Seq[Seq[TableRow]]
)

object IncomeTaxDetailsTableViewModelBuilder {

  def apply(viewModel: IncomeTaxAndNI)(implicit messages: Messages): IncomeTaxDetailsTableViewModel =
    IncomeTaxDetailsTableViewModel(
      incomeTaxRows = incomeTaxRows(viewModel),
      dividendRows = dividendRows(viewModel),
      totalUkIncomeTaxRows = totalUkIncomeTaxRows(viewModel),
      adjustmentRows = adjustmentRows(viewModel)
    )

  private def incomeTaxRows(viewModel: IncomeTaxAndNI)(implicit messages: Messages): Seq[Seq[TableRow]] =
    Seq(
      Option.when(!viewModel.startingRateForSavings.isZero)(
        rateRow(
          labelKey = "ats.total_income_tax.table.savings",
          beforeId = "start-rate-for-savings-before",
          beforeAmount = viewModel.startingRateForSavings,
          rateId = "start-rate-for-savings-rate",
          rate = viewModel.startingRateForSavingsRate,
          rowId = "starting-rate-for-savings-row",
          amountId = "starting-rate-for-savings-amount",
          amount = viewModel.startingRateForSavingsAmount
        )
      ),
      Option.when(!viewModel.basicRateIncomeTax.isZero)(
        rateRow(
          labelKey = "ats.total_income_tax.table.basic",
          beforeId = "basic-rate-income-tax-before",
          beforeAmount = viewModel.basicRateIncomeTax,
          rateId = "basic-rate-income-tax-rate",
          rate = viewModel.basicRateIncomeTaxRate,
          rowId = "basic-rate-income-tax-row",
          amountId = "basic-rate-income-tax-amount",
          amount = viewModel.basicRateIncomeTaxAmount
        )
      ),
      Option.when(!viewModel.basicRateIncomeTax.isZero && !viewModel.higherRateIncomeTax.isZero)(
        rateRow(
          labelKey = "ats.total_income_tax.table.higher",
          beforeId = "higher-rate-income-tax-before",
          beforeAmount = viewModel.higherRateIncomeTax,
          rateId = "higher-rate-income-tax-rate",
          rate = viewModel.higherRateIncomeTaxRate,
          rowId = "higher-rate-income-tax-row",
          amountId = "higher-rate-income-tax-amount",
          amount = viewModel.higherRateIncomeTaxAmount
        )
      ),
      Option.when(!viewModel.basicRateIncomeTax.isZero && !viewModel.additionalRateIncomeTax.isZero)(
        rateRow(
          labelKey = "ats.total_income_tax.table.additional",
          beforeId = "additional-rate-income-tax-before",
          beforeAmount = viewModel.additionalRateIncomeTax,
          rateId = "additional-rate-income-tax-rate",
          rate = viewModel.additionalRateIncomeTaxRate,
          rowId = "additional-rate-income-tax-row",
          amountId = "additional-rate-income-tax-amount",
          amount = viewModel.additionalRateIncomeTaxAmount
        )
      )
    ).flatten

  private def dividendRows(viewModel: IncomeTaxAndNI)(implicit messages: Messages): Seq[Seq[TableRow]] =
    Seq(
      Option.when(!viewModel.ordinaryRate.isZero)(
        rateRow(
          labelKey = "ats.total_income_tax.table.basic",
          beforeId = "ordinary-rate-before",
          beforeAmount = viewModel.ordinaryRate,
          rateId = "ordinary-rate-rate",
          rate = viewModel.ordinaryRateTaxRate,
          rowId = "ordinary-rate-row",
          amountId = "ordinary-rate-amount",
          amount = viewModel.ordinaryRateAmount
        )
      ),
      Option.when(!viewModel.upperRate.isZero)(
        rateRow(
          labelKey = "ats.total_income_tax.table.higher",
          beforeId = "upper-rate-before",
          beforeAmount = viewModel.upperRate,
          rateId = "upper-rate-rate",
          rate = viewModel.upperRateRate,
          rowId = "upper-rate-row",
          amountId = "upper-rate-amount",
          amount = viewModel.upperRateAmount
        )
      ),
      Option.when(!viewModel.additionalRate.isZero)(
        rateRow(
          labelKey = "ats.total_income_tax.table.additional",
          beforeId = "additional-rate-before",
          beforeAmount = viewModel.additionalRate,
          rateId = "additional-rate-rate",
          rate = viewModel.additionalRateRate,
          rowId = "additional-rate-row",
          amountId = "additional-rate-amount",
          amount = viewModel.additionalRateAmount
        )
      )
    ).flatten

  private def totalUkIncomeTaxRows(viewModel: IncomeTaxAndNI)(implicit messages: Messages): Seq[Seq[TableRow]] =
    Seq(
      simpleRow(
        label = Messages("ats.total_income_tax.scottish_income_uk_income_tax.table.total"),
        amount = viewModel.scottishAndRestOfUkTotal,
        amountId = "total-uk-income-tax-amount"
      )
    )

  private def adjustmentRows(viewModel: IncomeTaxAndNI)(implicit messages: Messages): Seq[Seq[TableRow]] =
    Seq(
      Option.when(!viewModel.otherAdjustmentsIncreasing.isZero)(
        simpleRow(
          label = Messages("ats.total_income_tax.table.other.increasing"),
          amount = viewModel.otherAdjustmentsIncreasing,
          amountId = "other-adjustments-increasing-amount"
        )
      ),
      Option.when(!viewModel.marriageAllowanceReceivedAmount.isZero)(
        simpleRow(
          label = Messages("ats.total_income_tax.table.marriage_allowance_received"),
          amount = viewModel.marriageAllowanceReceivedAmount,
          amountId = "marriage-allowance-received-amount"
        )
      ),
      Option.when(!viewModel.otherAdjustmentsReducing.isZero)(
        Seq(
          TableRow(content = HtmlContent(Messages("ats.total_income_tax.table.other.reducing"))),
          TableRow(
            content = HtmlContent(
              s"""<span id="reducing-amount">${viewModel.otherAdjustmentsReducing.renderCurrencyValueAsHtml(poundsOnly =
                  true
                )}</span>"""
            ),
            classes = "govuk-table__cell--numeric",
            attributes = Map("id" -> "other-adjustments-reducing-amount")
          )
        )
      )
    ).flatten

  private def rateRow(
    labelKey: String,
    beforeId: String,
    beforeAmount: Amount,
    rateId: String,
    rate: String,
    rowId: String,
    amountId: String,
    amount: Amount
  )(implicit messages: Messages): Seq[TableRow] =
    Seq(
      TableRow(
        content = HtmlContent(
          Messages(
            labelKey,
            span(beforeId, beforeAmount.renderCurrencyValueAsHtml(poundsOnly = true).body),
            span(rateId, rate)
          )
        ),
        attributes = Map("id" -> rowId)
      ),
      amountCell(amount, amountId)
    )

  private def simpleRow(label: String, amount: Amount, amountId: String)(implicit messages: Messages): Seq[TableRow] =
    Seq(
      TableRow(content = HtmlContent(label)),
      amountCell(amount, amountId)
    )

  private def amountCell(amount: Amount, id: String)(implicit messages: Messages): TableRow =
    TableRow(
      content = HtmlContent(amount.renderCurrencyValueAsHtml(poundsOnly = true).body),
      classes = "govuk-table__cell--numeric",
      attributes = Map("id" -> id)
    )

  private def span(id: String, value: String): String =
    s"""<span id="$id">$value</span>"""
}
