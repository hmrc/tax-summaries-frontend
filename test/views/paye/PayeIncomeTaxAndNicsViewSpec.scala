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
import services.atsData.PayeAtsTestData
import utils.TestConstants
import views.ViewSpecBase
import views.behaviours.ViewBehaviours
import views.html.paye.PayeIncomeTaxAndNicsView

class PayeIncomeTaxAndNicsViewSpec extends ViewSpecBase with TestConstants with ViewBehaviours {

  implicit val request: PayeAuthenticatedRequest[AnyContentAsEmpty.type] =
    requests.PayeAuthenticatedRequest(
      testNino,
      fakeCredentials,
      FakeRequest("GET", "/annual-tax-summary/paye/total-income-tax")
    )

  lazy val payeAtsTestData: PayeAtsTestData                   = inject[PayeAtsTestData]
  lazy val payeIncomeTaxAndNicsView: PayeIncomeTaxAndNicsView = inject[PayeIncomeTaxAndNicsView]

  def createView: () => Html =
    () =>
      payeIncomeTaxAndNicsView(
        payeAtsTestData.payeIncomeTaxAndNicsViewModel,
        isWelshTaxPayer = false,
        includeBRDMessage = false
      )(messages, request)

  "PayeIncomeTaxAndNicsView when rendered" must {
    behave like pageWithBackLink(createView)
  }

  "PayeIncomeTaxAndNicsView" must {

    "have correct data for scottish and rUK tax payer with all adjustments" in {

      val view     = payeIncomeTaxAndNicsView(
        payeAtsTestData.payeIncomeTaxAndNicsViewModel,
        isWelshTaxPayer = false,
        includeBRDMessage = false
      ).body
      val document = Jsoup.parse(view)

      document.getElementById("scottish_starter_rate").text() mustBe "Starter rate (£2,000 at 19%) £380.00"

      document.getElementById("scottish_basic_rate").text() mustBe "Basic rate (£10,150 at 20%) £2,030.00"

      document
        .getElementById("scottish_intermediate_rate")
        .text() mustBe "Intermediate rate (£19,430 at 21%) £4,080.00"

      document.getElementById("scottish_higher_rate").text() mustBe "Higher rate (£31,570 at 41%) £12,943.00"

      document.getElementById("totalScottishIncomeTax").text() mustBe "Total £19,433.00"

      document.getElementById("ordinary_rate").text() mustBe "Basic rate Dividend Tax (£19,430 at 19%) £4,080.00"

      document
        .getElementById("higher_rate_income_tax")
        .text() mustBe "Higher rate Income Tax (£10,150 at 20%) £2,030.00"

      document.getElementById("basic_rate_income_tax").text() mustBe "Basic rate Income Tax (£2,000 at 21%) £380.00"

      document.getElementById("upper_rate").text() mustBe "Higher rate Dividend Tax (£31,570 at 41%) £12,943.00"

      document.getElementById("totalRestOfUkIncomeTax").text() mustBe "Total £18,433.00"

      document
        .getElementById("less_tax_adjustment_previous_year")
        .text() mustBe "Less tax adjustment from a previous year £350.00"

      document
        .getElementById("marriage_allowance_received_amount")
        .text() mustBe "Less Marriage allowance received £200.00"

      document
        .getElementById("married_couples_allowance_adjustment")
        .text() mustBe "Less Married Couples Allowance £400.00"

      document.getElementById("tax_underpaid_previous_year").text() mustBe "Tax underpaid in a previous year £450.00"
    }

    "have correct data for UK tax payer" in {

      val view     = payeIncomeTaxAndNicsView(
        payeAtsTestData.payeUKIncomeTaxAndNicsViewModel,
        isWelshTaxPayer = false,
        includeBRDMessage = false
      ).body
      val document = Jsoup.parse(view)

      document.getElementById("ordinary_rate").text() mustBe "Basic rate Dividend Tax (£19,430 at 19%) £4,080.00"

      document
        .getElementById("higher_rate_income_tax")
        .text() mustBe "Higher rate Income Tax (£10,150 at 20%) £2,030.00"

      document.getElementById("basic_rate_income_tax").text() mustBe "Basic rate Income Tax (£2,000 at 21%) £380.00"

      document.getElementById("upper_rate").text() mustBe "Higher rate Dividend Tax (£31,570 at 41%) £12,943.00"

      document.getElementById("totalUkIncomeTax").text() mustBe "Total £20,322.00"

    }

    "have correct data for Welsh tax payer" in {

      val view     =
        payeIncomeTaxAndNicsView(
          payeAtsTestData.payeEmployeeContributionNicsViewModel,
          isWelshTaxPayer = true,
          includeBRDMessage = false
        ).body
      val document = Jsoup.parse(view)

      document
        .getElementById("welshIncomeTax")
        .text() mustBe "This includes Welsh Income Tax of £2,550.00"

    }

    "not display adjustments table when they have no adjustments" in {

      val view     =
        payeIncomeTaxAndNicsView(
          payeAtsTestData.payeUKIncomeTaxAndNicsViewModel.copy(adjustments = List.empty),
          isWelshTaxPayer = false,
          includeBRDMessage = false
        ).body
      val document = Jsoup.parse(view)

      document.select("#adjustments-table") mustBe empty

    }

    "have correct data for national insurance contributions" in {

      val view     =
        payeIncomeTaxAndNicsView(
          payeAtsTestData.payeEmployeeContributionNicsViewModel,
          isWelshTaxPayer = false,
          includeBRDMessage = false
        ).body
      val document = Jsoup.parse(view)

      document.getElementById("employeeContributions").text() mustBe "National Insurance contributions £70.00"

      document
        .getElementById("totalIncomeTaxAndNic")
        .text() mustBe "Total Income Tax and National Insurance contributions £431.00"

      document
        .getElementById("employerContributions")
        .text() mustBe "In addition to this, your employers paid £90.00 in National Insurance contributions."

      document
        .getElementById("incomeTaxBreakdownText")
        .text() mustBe "Your total income tax and NIC’s on your summary page does not reflect any refunds or repayments and therefore may be different to the calculated tax shown in this breakdown."

    }

    "have no data for national insurance contributions" in {

      val view     = payeIncomeTaxAndNicsView(
        payeAtsTestData.payeEmptyNicsViewModel,
        isWelshTaxPayer = false,
        includeBRDMessage = false
      ).body
      val document = Jsoup.parse(view)

      document.getElementById("employeeContributions").text() mustBe "National Insurance contributions £0.00"

      document
        .getElementById("totalIncomeTaxAndNic")
        .text() mustBe "Total Income Tax and National Insurance contributions £0.00"

    }
  }
}
