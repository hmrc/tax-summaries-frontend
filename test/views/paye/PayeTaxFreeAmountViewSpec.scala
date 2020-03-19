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

package views.paye

import config.AppFormPartialRetriever
import controllers.auth.PayeAuthenticatedRequest
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.test.FakeRequest
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestConstants
import view_models.Amount
import view_models.paye.{AmountRow, PayeTaxFreeAmount}

class PayeTaxFreeAmountViewSpec extends UnitSpec with OneAppPerSuite with TestConstants {

  implicit val messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]
  implicit val messages: Messages = Messages(Lang("en"), messagesApi)
  implicit val request = PayeAuthenticatedRequest(testNino, FakeRequest("GET", "/annual-tax-summary/paye/tax-free-amount"))
  implicit val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever

  "PayeTaxFreeAmountView" should {
    "display correct heading for given taxYear" in {
      val viewModel = PayeTaxFreeAmount(
        2018,
        List.empty,
        Amount.empty,
        List.empty,
        Amount.empty
      )

      val view = views.html.paye.paye_tax_free_amount(viewModel).body
      val document = Jsoup.parse(view)

      document
        .select("h1")
        .text shouldBe "Tax-free amount 6 April 2018 to 5 April 2019"
    }

    "display the table of adjustments when there is more than one row" in {
      val viewModel = PayeTaxFreeAmount(
        2018,
        List(
          AmountRow("personal_allowance", Amount.gbp(1)),
          AmountRow("marriage_allowance_transferred_amount", Amount.gbp(1)),
          AmountRow("other_allowances_amount", Amount.gbp(1))
        ),
        Amount.empty,
        List.empty,
        Amount.empty
      )

      val view = views.html.paye.paye_tax_free_amount(viewModel).body
      val document = Jsoup.parse(view)

      document.select("#adjustmentRows") should not be empty
      document.select("#personal_allowance") should not be empty
      document.select("#marriage_allowance_transferred_amount") should not be empty
      document.select("#other_allowances_amount") should not be empty
    }

    "display the table of adjustments without a total when there is just personal allowance" in {
      val viewModel = PayeTaxFreeAmount(
        2018,
        List(
          AmountRow("personal_allowance", Amount.gbp(1))
        ),
        Amount.empty,
        List.empty,
        Amount.empty
      )

      val view = views.html.paye.paye_tax_free_amount(viewModel).body
      val document = Jsoup.parse(view)

      document.select("#adjustmentRows") should not be empty
      document.select("#totalTaxFreeAmount") shouldBe empty
    }

    "not display the table of adjustments when there are no rows" in {
      val viewModel = PayeTaxFreeAmount(
        2018,
        List.empty,
        Amount.empty,
        List.empty,
        Amount.empty
      )

      val view = views.html.paye.paye_tax_free_amount(viewModel).body
      val document = Jsoup.parse(view)

      document.select("#adjustmentRows") shouldBe empty
    }

    "display the summary table" in {
      val viewModel = PayeTaxFreeAmount(
        2018,
        List.empty,
        Amount.empty,
        List(
          AmountRow("income_before_tax", Amount.gbp(1)),
          AmountRow("tax_free_amount", Amount.gbp(1))
        ),
        Amount.empty
      )

      val view = views.html.paye.paye_tax_free_amount(viewModel).body
      val document = Jsoup.parse(view)

      document.select("#summaryRows") should not be empty
      document.select("#income_before_tax") should not be empty
      document.select("#tax_free_amount") should not be empty
    }
  }
}
