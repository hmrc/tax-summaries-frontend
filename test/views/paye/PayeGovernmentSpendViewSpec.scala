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

import controllers.auth.PayeAuthenticatedRequest
import org.jsoup.Jsoup
import play.api.test.FakeRequest
import services.atsData.PayeAtsTestData
import utils.TestConstants
import views.ViewSpecBase
import views.html.paye.PayeGovernmentSpendingView

class PayeGovernmentSpendViewSpec extends TestConstants with ViewSpecBase {

  implicit val request =
    PayeAuthenticatedRequest(testNino, FakeRequest("GET", "/annual-tax-summary/paye/treasury-spending"))
  lazy val payeGovernmentSpendingView: PayeGovernmentSpendingView = inject[PayeGovernmentSpendingView]

  "view" should {
    "have correct data and heading for given taxYear" in {

      val view = payeGovernmentSpendingView(PayeAtsTestData.payeGovernmentSpendViewModel, isWelshTaxPayer = false).body
      val document = Jsoup.parse(view)

      document.getElementById("Welfare").text() shouldBe "Welfare (23.5%)"
      document.select("#Welfare + dd").text() shouldBe "£451"

      document.getElementById("Health").text() shouldBe "Health (20.2%)"
      document.select("#Health + dd").text() shouldBe "£388"

      document.getElementById("StatePensions").text() shouldBe "State Pensions (12.8%)"
      document.select("#StatePensions + dd").text() shouldBe "£246"

      document.getElementById("Education").text() shouldBe "Education (11.8%)"
      document.select("#Education + dd").text() shouldBe "£226"

      document.getElementById("Defence").text() shouldBe "Defence (5.3%)"
      document.select("#Defence + dd").text() shouldBe "£102"

      document.getElementById("NationalDebtInterest").text() shouldBe "National debt interest (5.1%)"
      document.select("#NationalDebtInterest + dd").text() shouldBe "£98"

      document.getElementById("Transport").text() shouldBe "Transport (4.3%)"
      document.select("#Transport + dd").text() shouldBe "£83"

      document.getElementById("PublicOrderAndSafety").text() shouldBe "Public order and safety (4.3%)"
      document.select("#PublicOrderAndSafety + dd").text() shouldBe "£83"

      document.getElementById("BusinessAndIndustry").text() shouldBe "Business and industry (3.6%)"
      document.select("#BusinessAndIndustry + dd").text() shouldBe "£69"

      document.getElementById("GovernmentAdministration").text() shouldBe "Government administration (2.1%)"
      document.select("#GovernmentAdministration + dd").text() shouldBe "£40"

      document
        .getElementById("HousingAndUtilities")
        .text() shouldBe "Housing and utilities, like street lighting (1.6%)"
      document.select("#HousingAndUtilities + dd").text() shouldBe "£31"

      document.getElementById("Environment").text() shouldBe "Environment (1.5%)"
      document.select("#Environment + dd").text() shouldBe "£29"

      document.getElementById("Culture").text() shouldBe "Culture, like sports, libraries and museums (1.5%)"
      document.select("#Culture + dd").text() shouldBe "£29"

      document.getElementById("OverseasAid").text() shouldBe "Overseas aid (1.2%)"
      document.select("#OverseasAid + dd").text() shouldBe "£23"

      document.getElementById("UkContributionToEuBudget").text() shouldBe "UK contribution to the EU budget (1%)"
      document.select("#UkContributionToEuBudget + dd").text() shouldBe "£19"

      document.select("#TotalAmount + dd").text() shouldBe "£200"

      document
        .select("h1")
        .text shouldBe "How your tax was spent"
      document
        .select("h2.heading-xlarge")
        .text shouldBe "6 April 2019 to 5 April 2020"
    }

    "link to Scottish government spending page for Scottish users" in {
      val view =
        payeGovernmentSpendingView(
          PayeAtsTestData.payeGovernmentSpendViewModel.copy(isScottish = true),
          isWelshTaxPayer = false).body
      val document = Jsoup.parse(view)

      document
        .select("#scottish-spending-link a")
        .attr("href") shouldBe "https://www.gov.scot/publications/scottish-income-tax-2019-2020/"
    }

    "not link to Scottish government spending page for non-Scottish users" in {
      val view =
        payeGovernmentSpendingView(
          PayeAtsTestData.payeGovernmentSpendViewModel.copy(isScottish = false),
          isWelshTaxPayer = false).body
      val document = Jsoup.parse(view)

      document.select("#scottish-spending-link") shouldBe empty
    }

    "have text relevant to non welsh users on government spending page for non welsh tax payer" in {
      val view = payeGovernmentSpendingView(PayeAtsTestData.payeGovernmentSpendViewModel, isWelshTaxPayer = false).body
      val document = Jsoup.parse(view)

      document
        .getElementById("paragraph-1")
        .text() shouldBe "These figures show how government spent money across the whole of the UK, including spending by the devolved administrations."
      document.getElementById("paragraph-2").text() shouldBe "All figures are rounded to the nearest pound."
      document
        .getElementById("paragraph-3")
        .text() shouldBe "The figures in the table above are intended as an illustration of how taxes are spent and not as a direct link between your Income Tax, National Insurance contributions and any specific expenditure."
      document.getElementById("paragraph-4").text() shouldBe "Spending information is published by HM Treasury."

    }

    "have text relevant to welsh users on government spending page for welsh tax payer" in {
      val view = payeGovernmentSpendingView(PayeAtsTestData.payeGovernmentSpendViewModel, isWelshTaxPayer = true).body
      val document = Jsoup.parse(view)

      document
        .getElementById("welsh-tax-payer-paragraph-1")
        .text() shouldBe "The figures in the table above are intended as an illustration of how taxes are spent and not as a direct link between your Income Tax, National Insurance contributions and any specific expenditure."
      document
        .getElementById("welsh-tax-payer-paragraph-2")
        .text() shouldBe "All figures are rounded to the nearest pound."
      document
        .getElementById("welsh-tax-payer-paragraph-3")
        .text() shouldBe "Spending information is published by HM Treasury."
      document
        .getElementById("welsh-tax-payer-paragraph-4")
        .text() shouldBe "The policy and funding for most of the key public services delivered in Wales are the responsibility of the Welsh Government. For more information on Welsh Government expenditure please visit www.gov.wales/calculate-welsh-income-tax-spend"

    }
  }
}
