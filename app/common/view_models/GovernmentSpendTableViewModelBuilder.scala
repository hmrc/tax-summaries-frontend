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

package common.view_models

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{HeadCell, HtmlContent, TableRow, Text}

object GovernmentSpendTableViewModelBuilder {

  def apply(viewModel: GovernmentSpend)(implicit messages: Messages): GovernmentSpendTableViewModel = {
    val caption =
      Messages("ats.treasury_spending.html.title") +
        Messages("generic.to_from", viewModel.taxYearFrom, viewModel.taxYearTo)

    val head = Seq(
      HeadCell(
        content = Text(Messages("generic.description")),
        classes = "govuk-!-font-size-24 govuk-!-padding-top-5 govuk-!-padding-bottom-0"
      ),
      HeadCell(
        content = Text(Messages("generic.amount_pounds")),
        classes = "govuk-table__header--numeric govuk-!-font-size-24 govuk-!-padding-top-5 govuk-!-padding-bottom-0"
      )
    )

    val rows =
      viewModel.govSpendAmountData.flatMap { case (key, value) =>
        Seq(
          Seq(
            TableRow(
              content = HtmlContent(
                s"""<span class="govuk-!-font-weight-bold">
                   |${Messages(s"ats.treasury_spending.table.$key")} (${value.percentage}%)
                   |</span>""".stripMargin
              ),
              classes = "govuk-!-padding-top-5 govuk-!-padding-bottom-1",
              attributes = Map(
                "id"    -> key,
                "style" -> "border-bottom: 0;"
              )
            ),
            TableRow(
              content = HtmlContent(value.amount.renderCurrencyValueAsHtml(poundsOnly = false).body),
              classes = "govuk-table__cell--numeric govuk-!-padding-top-5 govuk-!-padding-bottom-1",
              attributes = Map(
                "style" -> "border-bottom: 0;"
              )
            )
          ),
          Seq(
            TableRow(
              content = HtmlContent(
                s"""<div>
                   |  <meter value="${value.percentage}" min="0" max="100">(${value.percentage}%)</meter>
                   |</div>""".stripMargin
              ),
              classes = "govuk-!-padding-top-0 govuk-!-padding-bottom-2"
            ),
            TableRow(
              content = HtmlContent(""),
              classes = "govuk-!-padding-top-0 govuk-!-padding-bottom-2"
            )
          )
        )
      }.toSeq ++ Seq(
        Seq(
          TableRow(
            content = HtmlContent(Messages("ats.treasury_spending.total")),
            classes = "govuk-!-font-weight-bold",
            attributes = Map("id" -> "gov-spend-total")
          ),
          TableRow(
            content = HtmlContent(viewModel.totalAmount.renderCurrencyValueAsHtml(poundsOnly = false).body),
            classes = "govuk-table__cell--numeric govuk-!-font-weight-bold"
          )
        )
      )

    GovernmentSpendTableViewModel(
      caption = caption,
      head = head,
      rows = rows
    )
  }
}
