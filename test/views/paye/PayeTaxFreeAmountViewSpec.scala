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

package views.paye

import controllers.auth.requests
import controllers.auth.requests.PayeAuthenticatedRequest
import org.jsoup.Jsoup
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.Html
import utils.TestConstants
import view_models.Amount
import view_models.paye.{AmountRow, PayeTaxFreeAmount}
import views.ViewSpecBase
import views.behaviours.ViewBehaviours
import views.html.paye.PayeTaxFreeAmountView

class PayeTaxFreeAmountViewSpec extends ViewSpecBase with TestConstants with ViewBehaviours {

  implicit val request: PayeAuthenticatedRequest[AnyContentAsEmpty.type] =
    requests.PayeAuthenticatedRequest(
      testNino,
      fakeCredentials,
      FakeRequest("GET", "/annual-tax-summary/paye/tax-free-income")
    )
  lazy val payeTaxFreeAmountView: PayeTaxFreeAmountView                  = inject[PayeTaxFreeAmountView]

  val payeTaxFreeAmountViewModel: PayeTaxFreeAmount = PayeTaxFreeAmount(
    2022,
    List.empty,
    Amount.empty,
    List.empty,
    Amount.empty
  )

  def createView: () => Html =
    () => payeTaxFreeAmountView(payeTaxFreeAmountViewModel)(messages, request)

  "PayeTaxFreeAmountView when rendered" must {
    behave like pageWithBackLink(createView)
  }

  "PayeTaxFreeAmountView" must {
    "display correct heading for given taxYear" in {
      val viewModel = PayeTaxFreeAmount(
        2023,
        List.empty,
        Amount.empty,
        List.empty,
        Amount.empty
      )

      val view     = payeTaxFreeAmountView(viewModel).body
      val document = Jsoup.parse(view)

      document
        .select("h1")
        .text mustBe "Tax-free income 6 April 2022 to 5 April 2023"
    }

    "display the table of adjustments when there is more than one row" in {
      val viewModel = PayeTaxFreeAmount(
        2022,
        List(
          AmountRow("personal_allowance", Amount.gbp(1)),
          AmountRow("marriage_allowance_transferred_amount", Amount.gbp(1)),
          AmountRow("other_allowances_amount", Amount.gbp(1))
        ),
        Amount.empty,
        List.empty,
        Amount.empty
      )

      val view     = payeTaxFreeAmountView(viewModel).body
      val document = Jsoup.parse(view)

      document.select("#adjustmentRows")                        must not be empty
      document.select("#personal_allowance")                    must not be empty
      document.select("#marriage_allowance_transferred_amount") must not be empty
      document.select("#other_allowances_amount")               must not be empty
    }

    "display the table of adjustments without a total when there is just personal allowance" in {
      val viewModel = PayeTaxFreeAmount(
        2022,
        List(
          AmountRow("personal_allowance", Amount.gbp(1))
        ),
        Amount.empty,
        List.empty,
        Amount.empty
      )

      val view     = payeTaxFreeAmountView(viewModel).body
      val document = Jsoup.parse(view)

      document.select("#adjustmentRows") must not be empty
      document.select("#totalTaxFreeAmount") mustBe empty
    }

    "not display the table of adjustments when there are no rows" in {
      val viewModel = PayeTaxFreeAmount(
        2022,
        List.empty,
        Amount.empty,
        List.empty,
        Amount.empty
      )

      val view     = payeTaxFreeAmountView(viewModel).body
      val document = Jsoup.parse(view)

      document.select("#adjustmentRows") mustBe empty
    }

    "display the summary table" in {
      val viewModel = PayeTaxFreeAmount(
        2022,
        List.empty,
        Amount.empty,
        List(
          AmountRow("income_before_tax", Amount.gbp(1)),
          AmountRow("tax_free_amount", Amount.gbp(1))
        ),
        Amount.empty
      )

      val view     = payeTaxFreeAmountView(viewModel).body
      val document = Jsoup.parse(view)

      document.select("#summaryRows")       must not be empty
      document.select("#income_before_tax") must not be empty
      document.select("#tax_free_amount")   must not be empty
    }
  }
}
