/*
 * Copyright 2022 HM Revenue & Customs
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

import config.ApplicationConfig
import controllers.auth.PayeAuthenticatedRequest
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api.Configuration
import play.api.i18n.Messages
import play.api.test.FakeRequest
import services.atsData.PayeAtsTestData
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.TestConstants
import views.ViewSpecBase
import views.html.paye.PayeGovernmentSpendingView

class PayeGovernmentSpendViewSpec extends ViewSpecBase with TestConstants {

  implicit val request =
    PayeAuthenticatedRequest(
      testNino,
      false,
      fakeCredentials,
      FakeRequest("GET", "/annual-tax-summary/paye/treasury-spending"))

  lazy val payeAtsTestData = inject[PayeAtsTestData]
  lazy val payeGovernmentSpendingView: PayeGovernmentSpendingView = inject[PayeGovernmentSpendingView]

  "view" must {
    "have correct data and heading for given taxYear" in {

      val view = payeGovernmentSpendingView(payeAtsTestData.payeGovernmentSpendViewModel, isWelshTaxPayer = false).body
      val document = Jsoup.parse(view)

      document.getElementById("Welfare").text() mustBe "Welfare (23.5%)"
      document.select("#Welfare + dd").text() mustBe "£451"

      document.getElementById("Health").text() mustBe "Health (20.2%)"
      document.select("#Health + dd").text() mustBe "£388"

      document.getElementById("StatePensions").text() mustBe "State Pensions (12.8%)"
      document.select("#StatePensions + dd").text() mustBe "£246"

      document.getElementById("Education").text() mustBe "Education (11.8%)"
      document.select("#Education + dd").text() mustBe "£226"

      document.getElementById("Defence").text() mustBe "Defence (5.3%)"
      document.select("#Defence + dd").text() mustBe "£102"

      document.getElementById("NationalDebtInterest").text() mustBe "National Debt Interest (5.3%)"
      document.select("#NationalDebtInterest + dd").text() mustBe "£102"

      document.getElementById("Transport").text() mustBe "Transport (4.3%)"
      document.select("#Transport + dd").text() mustBe "£83"

      document.getElementById("PublicOrderAndSafety").text() mustBe "Public Order and Safety (4.3%)"
      document.select("#PublicOrderAndSafety + dd").text() mustBe "£83"

      document.getElementById("BusinessAndIndustry").text() mustBe "Business and Industry (3.6%)"
      document.select("#BusinessAndIndustry + dd").text() mustBe "£69"

      document.getElementById("GovernmentAdministration").text() mustBe "Government Administration (2.1%)"
      document.select("#GovernmentAdministration + dd").text() mustBe "£40"

      document
        .getElementById("HousingAndUtilities")
        .text() mustBe "Housing and Utilities, like street lighting (1.6%)"
      document.select("#HousingAndUtilities + dd").text() mustBe "£31"

      document.getElementById("Environment").text() mustBe "Environment (1.5%)"
      document.select("#Environment + dd").text() mustBe "£29"

      document.getElementById("Culture").text() mustBe "Culture, like sports, libraries and museums (1.5%)"
      document.select("#Culture + dd").text() mustBe "£29"

      document.getElementById("OverseasAid").text() mustBe "Overseas Aid (1.2%)"
      document.select("#OverseasAid + dd").text() mustBe "£23"

      document.getElementById("UkContributionToEuBudget").text() mustBe "UK Contribution to the EU Budget (1%)"
      document.select("#UkContributionToEuBudget + dd").text() mustBe "£19"

      document.select("#gov-spend-total + dd").text() mustBe "£200"

      document
        .select("h1")
        .text mustBe "How your tax was spent"
      document
        .select("h2.govuk-heading-m")
        .text mustBe s"6 April ${taxYear - 1} to 5 April $taxYear"
    }

    "link to Scottish government spending page for Scottish users for tax year 2021" in {

      class FakeAppConfig extends ApplicationConfig(inject[ServicesConfig], inject[Configuration]) {
        override lazy val taxYear: Int = 2021
      }

      implicit lazy val appConfig: FakeAppConfig = new FakeAppConfig

      val view =
        payeGovernmentSpendingView(
          payeAtsTestData.payeGovernmentSpendViewModel.copy(isScottish = true, taxYear = appConfig.taxYear),
          isWelshTaxPayer = false
        ).body
      val document = Jsoup.parse(view)

      document
        .select("#scottish-spending-link a")
        .attr("href") mustBe "https://www.gov.scot/publications/scottish-income-tax-2020-2021/"
    }

    "link to Scottish government spending page for Scottish users for tax year 2020" in {

      class FakeAppConfig extends ApplicationConfig(inject[ServicesConfig], inject[Configuration]) {
        override lazy val taxYear: Int = 2020
      }

      implicit lazy val appConfig: FakeAppConfig = new FakeAppConfig

      val view =
        payeGovernmentSpendingView(
          payeAtsTestData.payeGovernmentSpendViewModel.copy(isScottish = true, taxYear = appConfig.taxYear),
          isWelshTaxPayer = false
        ).body
      val document = Jsoup.parse(view)

      document
        .select("#scottish-spending-link a")
        .attr("href") mustBe "https://www.gov.scot/publications/scottish-income-tax-2019-2020/"
    }

    "not link to Scottish government spending page for non-Scottish users" in {
      val view =
        payeGovernmentSpendingView(
          payeAtsTestData.payeGovernmentSpendViewModel.copy(isScottish = false),
          isWelshTaxPayer = false).body
      val document = Jsoup.parse(view)

      document.select("#scottish-spending-link") mustBe empty
    }

    "have text relevant to non welsh users on government spending page for non welsh tax payer" in {
      val view = payeGovernmentSpendingView(payeAtsTestData.payeGovernmentSpendViewModel, isWelshTaxPayer = false).body
      val document = Jsoup.parse(view)

      document
        .getElementById("paragraph-1")
        .text() mustBe "These figures show how government spent money across the whole of the UK, including spending by the devolved administrations."
      document.getElementById("paragraph-2").text() mustBe "All figures are rounded to the nearest pound."
      document
        .getElementById("paragraph-3")
        .text() mustBe "The figures in the table above are intended as an illustration of how taxes are spent and not as a direct link between your Income Tax, National Insurance contributions and any specific expenditure."
      document.getElementById("paragraph-4").text() mustBe "Spending information is published by HM Treasury."

    }

    "have text relevant to welsh users on government spending page for welsh tax payer" in {
      val view = payeGovernmentSpendingView(payeAtsTestData.payeGovernmentSpendViewModel, isWelshTaxPayer = true).body
      val document = Jsoup.parse(view)

      document
        .getElementById("welsh-tax-payer-paragraph-1")
        .text() mustBe "The figures in the table above are intended as an illustration of how taxes are spent and not as a direct link between your Income Tax, National Insurance contributions and any specific expenditure."
      document
        .getElementById("welsh-tax-payer-paragraph-2")
        .text() mustBe "All figures are rounded to the nearest pound."
      document
        .getElementById("welsh-tax-payer-paragraph-3")
        .text() mustBe "Spending information is published by HM Treasury."
      document
        .getElementById("welsh-tax-payer-paragraph-4")
        .text() mustBe "The policy and funding for most of the key public services delivered in Wales are the responsibility of the Welsh Government. For more information on Welsh Government expenditure please visit https://www.gov.wales/calculate-welsh-income-tax-spend"

    }
  }
}
