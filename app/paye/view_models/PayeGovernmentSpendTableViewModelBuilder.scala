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

package paye.view_models

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{HeadCell, HtmlContent, TableRow, Text}

final case class PayeGovernmentSpendTableViewModel(
  caption: String,
  head: Seq[HeadCell],
  rows: Seq[Seq[TableRow]]
)

object PayeGovernmentSpendTableViewModelBuilder {

  def apply(viewModel: PayeGovernmentSpend)(implicit messages: Messages): PayeGovernmentSpendTableViewModel = {
    val caption =
      Messages("paye.ats.treasury_spending.title") +
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
      viewModel.orderedSpendRows.flatMap { rowModel =>
        Seq(
          Seq(
            TableRow(
              content = HtmlContent(
                s"""<span class="govuk-!-font-weight-bold">
                   |${Messages(
                    "paye.ats.treasury_spending.table." + rowModel.category
                  )} (${rowModel.spendData.percentage}%)
                   |</span>""".stripMargin
              ),
              classes = "govuk-!-padding-top-5 govuk-!-padding-bottom-1",
              attributes = Map(
                "id"    -> rowModel.category,
                "style" -> "border-bottom: 0;"
              )
            ),
            TableRow(
              content = HtmlContent(s"£${rowModel.spendData.amount.toHalfRoundedUpAmount}"),
              classes = "govuk-table__cell--numeric govuk-!-padding-top-5 govuk-!-padding-bottom-1",
              attributes = Map("style" -> "border-bottom: 0;")
            )
          ),
          Seq(
            TableRow(
              content = HtmlContent(
                s"""<div>
                   |  <meter value="${rowModel.spendData.percentage}" min="0" max="100">(${rowModel.spendData.percentage}%)</meter>
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
      } ++ Seq(
        Seq(
          TableRow(
            content = Text(Messages("paye.ats.treasury_spending.total")),
            classes = "govuk-!-font-size-24 govuk-!-padding-top-7 govuk-!-font-weight-bold",
            attributes = Map("id" -> "gov-spend-total")
          ),
          TableRow(
            content = HtmlContent(s"&pound;${viewModel.totalAmount}"),
            classes = "govuk-table__cell--numeric govuk-!-padding-top-7"
          )
        )
      )

    PayeGovernmentSpendTableViewModel(
      caption = caption,
      head = head,
      rows = rows
    )
  }
}
