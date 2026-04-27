package common.view_models

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{HeadCell, HtmlContent, TableRow, Text}

object HowYourTaxWasSpentTableViewModelBuilder {

  def apply(spendData: Seq[(String, Double)])(implicit messages: Messages): HowYourTaxWasSpentTableViewModel = {
    val head = Seq(
      HeadCell(
        content = Text(messages("generic.description")),
        classes = "govuk-!-font-size-24 govuk-!-padding-top-5 govuk-!-padding-bottom-0"
      ),
      HeadCell(
        content = Text(messages("ats.howYourTaxWasSpent.table.percentage.title")),
        classes = "govuk-table__header--numeric govuk-!-font-size-24 govuk-!-padding-top-5 govuk-!-padding-bottom-0"
      )
    )

    val rows = spendData.flatMap { case (name, percentage) =>
      Seq(
        Seq(
          TableRow(
            content = HtmlContent(Messages(s"ats.treasury_spending.table.$name")),
            classes = "govuk-!-padding-top-5 govuk-!-padding-bottom-1",
            attributes = Map(
              "id" -> name,
              "style" -> "border-bottom: 0;"
            )
          ),
          TableRow(
            content = HtmlContent(s"$percentage%"),
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
                 |  <meter value="$percentage" min="0" max="100">$percentage%</meter>
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
    }

    HowYourTaxWasSpentTableViewModel(
      caption = Messages("ats.howYourTaxWasSpent.heading"),
      head = head,
      rows = rows
    )
  }
}