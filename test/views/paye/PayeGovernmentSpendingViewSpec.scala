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

import config.ApplicationConfig
import controllers.auth.PayeAuthenticatedRequest
import org.jsoup.Jsoup
import play.api.Configuration
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.Html
import services.atsData.PayeAtsTestData
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.TestConstants
import views.ViewSpecBase
import views.behaviours.ViewBehaviours
import views.html.paye.PayeGovernmentSpendingView

class PayeGovernmentSpendingViewSpec extends ViewSpecBase with TestConstants with ViewBehaviours {

  implicit val request: PayeAuthenticatedRequest[AnyContentAsEmpty.type] =
    PayeAuthenticatedRequest(
      testNino,
      isSa = false,
      fakeCredentials,
      FakeRequest("GET", "/annual-tax-summary/paye/treasury-spending")
    )

  lazy val payeAtsTestData: PayeAtsTestData                       = inject[PayeAtsTestData]
  lazy val payeGovernmentSpendingView: PayeGovernmentSpendingView = inject[PayeGovernmentSpendingView]

  def createView: () => Html =
    () =>
      payeGovernmentSpendingView(
        payeAtsTestData.payeGovernmentSpendViewModel,
        isWelshTaxPayer = false
      )(messages, request, appConfig)

  "PayeIncomeTaxAndNicsView when rendered" must {
    behave like pageWithBackLink(createView)
  }

  "view" must {
    "have correct data and heading for given taxYear" in {

      val view     = payeGovernmentSpendingView(payeAtsTestData.payeGovernmentSpendViewModel, isWelshTaxPayer = false).body
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
        .text mustBe s"How your tax was spent for the tax year 6 April ${taxYear - 1} to 5 April $taxYear"
    }

    "link to Scottish government spending page for Scottish users for tax year 2021" in {

      class FakeAppConfig extends ApplicationConfig(inject[ServicesConfig], inject[Configuration]) {
        override lazy val taxYear: Int = 2021
      }

      implicit lazy val appConfig: FakeAppConfig = new FakeAppConfig

      val view     =
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

      val view     =
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
      val view     =
        payeGovernmentSpendingView(
          payeAtsTestData.payeGovernmentSpendViewModel.copy(isScottish = false),
          isWelshTaxPayer = false
        ).body
      val document = Jsoup.parse(view)

      document.select("#scottish-spending-link") mustBe empty
    }

    "have text relevant to non welsh users on government spending page for non welsh tax payer" in {
      val view     = payeGovernmentSpendingView(payeAtsTestData.payeGovernmentSpendViewModel, isWelshTaxPayer = false).body
      val document = Jsoup.parse(view)
      document
        .getElementById("paragraph-1")
        .text() mustBe "These figures include spending by devolved administrations, such as the Scottish or Welsh Governments. They do not include indirect taxes, such as VAT and other duties."
      document
        .getElementById("generic-spending-link")
        .text() mustBe "You can find out more about how public spending was calculated in your tax summary on GOV.UK."
      document
        .getElementById("paragraph-4")
        .text() mustBe "Spending information is published by HM Treasury. All figures are rounded to the nearest pound."

    }

    "have text relevant to welsh users on government spending page for welsh tax payer" in {
      val view     = payeGovernmentSpendingView(payeAtsTestData.payeGovernmentSpendViewModel, isWelshTaxPayer = true).body
      val document = Jsoup.parse(view)
      document
        .getElementById("paragraph-1")
        .text() mustBe "These figures include spending by devolved administrations, such as the Scottish or Welsh Governments. They do not include indirect taxes, such as VAT and other duties."
      document
        .getElementById("welsh-spending-link")
        .text() mustBe "You can find out more about how public spending was calculated in your tax summary on the Welsh Government website."
      document
        .getElementById("paragraph-4")
        .text() mustBe "Spending information is published by HM Treasury. All figures are rounded to the nearest pound."

    }
  }
}
