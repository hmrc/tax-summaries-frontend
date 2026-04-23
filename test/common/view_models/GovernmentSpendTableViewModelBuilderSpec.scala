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

import common.models.SpendData
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.test.Helpers.stubMessagesApi
import uk.gov.hmrc.govukfrontend.views.Aliases.{HtmlContent, Text}

class GovernmentSpendTableViewModelBuilderSpec extends AnyWordSpec with Matchers {

  private val messagesApi: MessagesApi = stubMessagesApi(
    Map(
      "default" -> Map(
        "ats.treasury_spending.html.title"    -> "Your taxes and public spending",
        "generic.to_from"                     -> " from 2024 to 2025",
        "generic.description"                 -> "Description",
        "generic.amount_pounds"               -> "Amount in pounds",
        "ats.treasury_spending.table.welfare" -> "Welfare",
        "ats.treasury_spending.table.health"  -> "Health",
        "ats.treasury_spending.total"         -> "Total"
      )
    )
  )

  implicit private val messages: Messages =
    messagesApi.preferred(Seq(Lang.defaultLang))

  "GovernmentSpendTableViewModelBuilder" should {

    "build the table view model correctly" in {

      val viewModel = GovernmentSpend(
        taxYear = 2025,
        userUtr = "1234567890",
        govSpendAmountData = List(
          "welfare" -> SpendData(
            amount = Amount(BigDecimal(400.00), "GBP"),
            percentage = BigDecimal(40.0)
          ),
          "health" -> SpendData(
            amount = Amount(BigDecimal(600.00), "GBP"),
            percentage = BigDecimal(60.0)
          )
        ),
        userTitle = "Ms",
        userForename = "Jane",
        userSurname = "Doe",
        totalAmount = Amount(BigDecimal(1000.00), "GBP"),
        incomeTaxStatus = "0001",
        scottishIncomeTax = Amount(BigDecimal(0), "GBP")
      )

      val result = GovernmentSpendTableViewModelBuilder(viewModel)

      result.caption mustBe "Your taxes and public spending from 2024 to 2025"

      result.head must have size 2
      result.head.head.content mustBe Text("Description")
      result.head(1).content mustBe Text("Amount in pounds")

      result.rows must have size 5

      val firstRow = result.rows.head
      firstRow.head.attributes must contain("id" -> "welfare")
      firstRow.head.attributes must contain("style" -> "border-bottom: 0;")

      val labelHtml = firstRow.head.content.asInstanceOf[HtmlContent].value.body
      labelHtml must include("Welfare")
      labelHtml must include("(40.0%)")

      val amountHtml = firstRow(1).content.asInstanceOf[HtmlContent].value.body
      amountHtml must include("400")

      val meterRow = result.rows(1)
      meterRow.head.content.asInstanceOf[HtmlContent].value.body must include("""<meter value="40.0"""")
      meterRow(1).content.asInstanceOf[HtmlContent].value.body mustBe ""

      val totalRow = result.rows.last
      totalRow.head.attributes must contain("id" -> "gov-spend-total")

      val totalHtml = totalRow(1).content.asInstanceOf[HtmlContent].value.body
      totalHtml must include("1,000")
    }
  }
}