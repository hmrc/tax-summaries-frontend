package sa.view_models

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{HeadCell, HtmlContent, TableRow}

final case class IncomeTaxDetailsTableViewModel(
                                                 incomeTaxRows: Seq[Seq[TableRow]],
                                                 dividendRows: Seq[Seq[TableRow]],
                                                 totalUkIncomeTaxRows: Seq[Seq[TableRow]],
                                                 adjustmentRows: Seq[Seq[TableRow]]
                                               )

object IncomeTaxDetailsTableViewModelBuilder {

  def apply(viewModel: IncomeTaxAndNI)(implicit messages: Messages): IncomeTaxDetailsTableViewModel = {

    val incomeTaxRows = Seq(
      Option.when(!viewModel.startingRateForSavings.isZero) {
        Seq(
          TableRow(
            content = HtmlContent(Messages(
              "ats.total_income_tax.table.savings",
              s"""<span id="start-rate-for-savings-before">${viewModel.startingRateForSavings.renderCurrencyValueAsHtml(poundsOnly = true)}</span>""",
              s"""<span id="start-rate-for-savings-rate">${viewModel.startingRateForSavingsRate}</span>"""
            )),
            attributes = Map("id" -> "starting-rate-for-savings-row")
          ),
          TableRow(
            content = HtmlContent(viewModel.startingRateForSavingsAmount.renderCurrencyValueAsHtml(poundsOnly = true).body),
            classes = "govuk-table__cell--numeric",
            attributes = Map("id" -> "starting-rate-for-savings-amount")
          )
        )
      },
      Option.when(!viewModel.basicRateIncomeTax.isZero) {
        Seq(
          TableRow(
            content = HtmlContent(Messages(
              "ats.total_income_tax.table.basic",
              s"""<span id="basic-rate-income-tax-before">${viewModel.basicRateIncomeTax.renderCurrencyValueAsHtml(poundsOnly = true)}</span>""",
              s"""<span id="basic-rate-income-tax-rate">${viewModel.basicRateIncomeTaxRate}</span>"""
            )),
            attributes = Map("id" -> "basic-rate-income-tax-row")
          ),
          TableRow(
            content = HtmlContent(viewModel.basicRateIncomeTaxAmount.renderCurrencyValueAsHtml(poundsOnly = true).body),
            classes = "govuk-table__cell--numeric",
            attributes = Map("id" -> "basic-rate-income-tax-amount")
          )
        )
      },
      Option.when(!viewModel.basicRateIncomeTax.isZero && !viewModel.higherRateIncomeTax.isZero) {
        Seq(
          TableRow(
            content = HtmlContent(Messages(
              "ats.total_income_tax.table.higher",
              s"""<span id="higher-rate-income-tax-before">${viewModel.higherRateIncomeTax.renderCurrencyValueAsHtml(poundsOnly = true)}</span>""",
              s"""<span id="higher-rate-income-tax-rate">${viewModel.higherRateIncomeTaxRate}</span>"""
            )),
            attributes = Map("id" -> "higher-rate-income-tax-row")
          ),
          TableRow(
            content = HtmlContent(viewModel.higherRateIncomeTaxAmount.renderCurrencyValueAsHtml(poundsOnly = true).body),
            classes = "govuk-table__cell--numeric",
            attributes = Map("id" -> "higher-rate-income-tax-amount")
          )
        )
      },
      Option.when(!viewModel.basicRateIncomeTax.isZero && !viewModel.additionalRateIncomeTax.isZero) {
        Seq(
          TableRow(
            content = HtmlContent(Messages(
              "ats.total_income_tax.table.additional",
              s"""<span id="additional-rate-income-tax-before">${viewModel.additionalRateIncomeTax.renderCurrencyValueAsHtml(poundsOnly = true)}</span>""",
              s"""<span id="additional-rate-income-tax-rate">${viewModel.additionalRateIncomeTaxRate}</span>"""
            )),
            attributes = Map("id" -> "additional-rate-income-tax-row")
          ),
          TableRow(
            content = HtmlContent(viewModel.additionalRateIncomeTaxAmount.renderCurrencyValueAsHtml(poundsOnly = true).body),
            classes = "govuk-table__cell--numeric",
            attributes = Map("id" -> "additional-rate-income-tax-amount")
          )
        )
      }
    ).flatten

    val dividendRows = Seq(
      Option.when(!viewModel.ordinaryRate.isZero) {
        Seq(
          TableRow(
            content = HtmlContent(Messages(
              "ats.total_income_tax.table.basic",
              s"""<span id="ordinary-rate-before">${viewModel.ordinaryRate.renderCurrencyValueAsHtml(poundsOnly = true)}</span>""",
              s"""<span id="ordinary-rate-rate">${viewModel.ordinaryRateTaxRate}</span>"""
            )),
            attributes = Map("id" -> "ordinary-rate-row")
          ),
          TableRow(
            content = HtmlContent(viewModel.ordinaryRateAmount.renderCurrencyValueAsHtml(poundsOnly = true).body),
            classes = "govuk-table__cell--numeric",
            attributes = Map("id" -> "ordinary-rate-amount")
          )
        )
      },
      Option.when(!viewModel.upperRate.isZero) {
        Seq(
          TableRow(
            content = HtmlContent(Messages(
              "ats.total_income_tax.table.higher",
              s"""<span id="upper-rate-before">${viewModel.upperRate.renderCurrencyValueAsHtml(poundsOnly = true)}</span>""",
              s"""<span id="upper-rate-rate">${viewModel.upperRateRate}</span>"""
            )),
            attributes = Map("id" -> "upper-rate-row")
          ),
          TableRow(
            content = HtmlContent(viewModel.upperRateAmount.renderCurrencyValueAsHtml(poundsOnly = true).body),
            classes = "govuk-table__cell--numeric",
            attributes = Map("id" -> "upper-rate-amount")
          )
        )
      },
      Option.when(!viewModel.additionalRate.isZero) {
        Seq(
          TableRow(
            content = HtmlContent(Messages(
              "ats.total_income_tax.table.additional",
              s"""<span id="additional-rate-before">${viewModel.additionalRate.renderCurrencyValueAsHtml(poundsOnly = true)}</span>""",
              s"""<span id="additional-rate-rate">${viewModel.additionalRateRate}</span>"""
            )),
            attributes = Map("id" -> "additional-rate-row")
          ),
          TableRow(
            content = HtmlContent(viewModel.additionalRateAmount.renderCurrencyValueAsHtml(poundsOnly = true).body),
            classes = "govuk-table__cell--numeric",
            attributes = Map("id" -> "additional-rate-amount")
          )
        )
      }
    ).flatten

    val totalUkIncomeTaxRows = Seq(
      Seq(
        TableRow(
          content = HtmlContent(Messages("ats.total_income_tax.scottish_income_uk_income_tax.table.total"))
        ),
        TableRow(
          content = HtmlContent(viewModel.scottishAndRestOfUkTotal.renderCurrencyValueAsHtml(poundsOnly = true).body),
          classes = "govuk-table__cell--numeric",
          attributes = Map("id" -> "total-uk-income-tax-amount")
        )
      )
    )

    val adjustmentRows = Seq(
      Option.when(!viewModel.otherAdjustmentsIncreasing.isZero) {
        Seq(
          TableRow(content = HtmlContent(Messages("ats.total_income_tax.table.other.increasing"))),
          TableRow(
            content = HtmlContent(viewModel.otherAdjustmentsIncreasing.renderCurrencyValueAsHtml(poundsOnly = true).body),
            classes = "govuk-table__cell--numeric",
            attributes = Map("id" -> "other-adjustments-increasing-amount")
          )
        )
      },
      Option.when(!viewModel.marriageAllowanceReceivedAmount.isZero) {
        Seq(
          TableRow(content = HtmlContent(Messages("ats.total_income_tax.table.marriage_allowance_received"))),
          TableRow(
            content = HtmlContent(viewModel.marriageAllowanceReceivedAmount.renderCurrencyValueAsHtml(poundsOnly = true).body),
            classes = "govuk-table__cell--numeric",
            attributes = Map("id" -> "marriage-allowance-received-amount")
          )
        )
      },
      Option.when(!viewModel.otherAdjustmentsReducing.isZero) {
        Seq(
          TableRow(content = HtmlContent(Messages("ats.total_income_tax.table.other.reducing"))),
          TableRow(
            content = HtmlContent(
              s"""<span id="reducing-amount">${viewModel.otherAdjustmentsReducing.renderCurrencyValueAsHtml(poundsOnly = true)}</span>"""
            ),
            classes = "govuk-table__cell--numeric",
            attributes = Map("id" -> "other-adjustments-reducing-amount")
          )
        )
      }
    ).flatten

    IncomeTaxDetailsTableViewModel(
      incomeTaxRows = incomeTaxRows,
      dividendRows = dividendRows,
      totalUkIncomeTaxRows = totalUkIncomeTaxRows,
      adjustmentRows = adjustmentRows
    )
  }
}