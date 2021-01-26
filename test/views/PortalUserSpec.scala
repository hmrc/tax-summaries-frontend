/*
 * Copyright 2021 HM Revenue & Customs
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

package views

import controllers.ControllerBaseSpec
import controllers.auth.AuthenticatedRequest
import models.SpendData
import org.jsoup.Jsoup
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.HtmlUnitFactory
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import uk.gov.hmrc.domain.SaUtr
import utils.TestConstants._
import view_models._

class PortalUserSpec extends HtmlUnitFactory with MockitoSugar with ControllerBaseSpec {

  val utr = testUtr
  implicit val messages: Messages = MessagesImpl(Lang("en"), inject[MessagesApi])
  lazy val requestWithSession = AuthenticatedRequest(
    "userId",
    None,
    Some(SaUtr(utr)),
    None,
    None,
    None,
    None,
    FakeRequest().withSession("TAXS_USER_TYPE" -> "PORTAL"))
  val language = Lang("en")
  val amount = new Amount(0.00, "GBP")
  val rate = new Rate("5")

  "Logging in as a portal user" should {

    "show the 'exit tax summaries' link on the capital gains page" in {

      val fakeViewModel = new CapitalGains(
        2014,
        utr,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        rate,
        rate,
        rate,
        rate,
        rate,
        rate,
        "",
        "",
        ""
      )
      val result =
        capitalGainsView(fakeViewModel)(requestWithSession, messages, formPartialRetriever, templateRenderer, appConfig)
      val document = Jsoup.parse(contentAsString(result))

      document.select("#global-breadcrumb li:nth-child(1) a").attr("href") should include("/annual-tax-summary")
      document.select("#global-breadcrumb li:nth-child(1) a").text shouldBe "Select the tax year"

      document.select("#global-breadcrumb li:nth-child(2) a").attr("href") should include(
        "/annual-tax-summary/main?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(2) a").text shouldBe "Your Annual Tax Summary"

      document.select("#global-breadcrumb li:nth-child(3) a").attr("href") should include(
        "/annual-tax-summary/summary?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(3) a").text shouldBe "Your income and taxes"

      document.select("#global-breadcrumb li:nth-child(4)").toString should include(
        "<strong>Capital Gains Tax</strong>")
    }

    "show the 'exit tax summaries' link on the income before tax page" in {

      val fakeViewModel =
        new IncomeBeforeTax(2014, utr, amount, amount, amount, amount, amount, amount, amount, amount, "", "", "")
      val result = incomeBeforeTaxView(fakeViewModel)(
        requestWithSession,
        messages,
        formPartialRetriever,
        templateRenderer,
        appConfig)
      val document = Jsoup.parse(contentAsString(result))

      document.select("#global-breadcrumb li:nth-child(1) a").attr("href") should include("/annual-tax-summary")
      document.select("#global-breadcrumb li:nth-child(1) a").text shouldBe "Select the tax year"

      document.select("#global-breadcrumb li:nth-child(2) a").attr("href") should include(
        "/annual-tax-summary/main?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(2) a").text shouldBe "Your Annual Tax Summary"

      document.select("#global-breadcrumb li:nth-child(3) a").attr("href") should include(
        "/annual-tax-summary/summary?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(3) a").text shouldBe "Your income and taxes"

      document.select("#global-breadcrumb li:nth-child(4)").toString should include(
        "<strong>Your total income</strong>")
    }

    "show the 'exit tax summaries' link on the nics page" in {

      val fakeViewModel = new Summary(
        2014,
        utr,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        rate,
        rate,
        "",
        "",
        "")
      val result =
        nicsView(fakeViewModel)(requestWithSession, messages, formPartialRetriever, templateRenderer, appConfig)
      val document = Jsoup.parse(contentAsString(result))

      document.select("#global-breadcrumb li:nth-child(1) a").attr("href") should include("/annual-tax-summary")
      document.select("#global-breadcrumb li:nth-child(1) a").text shouldBe "Select the tax year"

      document.select("#global-breadcrumb li:nth-child(2) a").attr("href") should include(
        "/annual-tax-summary/main?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(2) a").text shouldBe "Your Annual Tax Summary"

      document.select("#global-breadcrumb li:nth-child(3) a").attr("href") should include(
        "/annual-tax-summary/summary?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(3) a").text should include("Your income and taxes")

      document.select("#global-breadcrumb li:nth-child(4)").toString should include(
        "<strong>Your Income Tax and National Insurance</strong>")
    }

    "show the 'exit tax summaries' link on the summaries page" in {

      val fakeViewModel = new Summary(
        2014,
        utr,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        rate,
        rate,
        "",
        "",
        "")
      val result = summaryView(fakeViewModel)(
        language,
        requestWithSession,
        messages,
        formPartialRetriever,
        templateRenderer,
        appConfig)
      val document = Jsoup.parse(contentAsString(result))

      document.select("#global-breadcrumb li:nth-child(1) a").attr("href") should include("/annual-tax-summary")
      document.select("#global-breadcrumb li:nth-child(1) a").text shouldBe "Select the tax year"

      document.select("#global-breadcrumb li:nth-child(2) a").attr("href") should include(
        "/annual-tax-summary/main?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(2) a").text shouldBe "Your Annual Tax Summary"

      document.select("#global-breadcrumb li:nth-child(3)").toString should include(
        "<strong>Your income and taxes</strong>")
    }

    "show the 'exit tax summaries' link on the tax free amount page" in {

      val fakeViewModel = new Allowances(2014, utr, amount, amount, amount, amount, "", "", "")
      val result = taxFreeAmountView(fakeViewModel)(
        requestWithSession,
        messages,
        formPartialRetriever,
        templateRenderer,
        appConfig)
      val document = Jsoup.parse(contentAsString(result))

      document.select("#global-breadcrumb li:nth-child(1) a").attr("href") should include("/annual-tax-summary")
      document.select("#global-breadcrumb li:nth-child(1) a").text shouldBe "Select the tax year"

      document.select("#global-breadcrumb li:nth-child(2) a").attr("href") should include(
        "/annual-tax-summary/main?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(2) a").text shouldBe "Your Annual Tax Summary"

      document.select("#global-breadcrumb li:nth-child(3) a").attr("href") should include(
        "/annual-tax-summary/summary?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(3) a").text shouldBe "Your income and taxes"

      document.select("#global-breadcrumb li:nth-child(4)").toString should include(
        "<strong>Your tax-free amount</strong>")
    }

    "show the 'exit tax summaries' link on the total income tax page" in {

      val fakeViewModel = new TotalIncomeTax(
        2014,
        utr,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        ScottishTax.empty,
        amount,
        amount,
        amount,
        SavingsTax.empty,
        "",
        rate,
        rate,
        rate,
        rate,
        rate,
        rate,
        rate,
        ScottishRates.empty,
        SavingsRates.empty,
        "",
        "",
        ""
      )

      val result = totalIncomeTaxView(fakeViewModel)(
        requestWithSession,
        messages,
        formPartialRetriever,
        templateRenderer,
        appConfig)
      val document = Jsoup.parse(contentAsString(result))

      document.select("#global-breadcrumb li:nth-child(1) a").attr("href") should include("/annual-tax-summary")
      document.select("#global-breadcrumb li:nth-child(1) a").text shouldBe "Select the tax year"

      document.select("#global-breadcrumb li:nth-child(2) a").attr("href") should include(
        "/annual-tax-summary/main?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(2) a").text shouldBe "Your Annual Tax Summary"

      document.select("#global-breadcrumb li:nth-child(3) a").attr("href") should include(
        "/annual-tax-summary/summary?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(3) a").text should include("Your income and taxes")

      document.select("#global-breadcrumb li:nth-child(4) a").attr("href") should include(
        "/annual-tax-summary/nics?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(4) a").text should include(
        "Your Income Tax and National Insurance")

      document.select("#global-breadcrumb li:nth-child(5)").toString should include("<strong>Income Tax</strong>")
    }

    "show the 'exit tax summaries' link on the treasury spending page" in {

      val spendData = new SpendData(amount, 20)
      val scottishIncomeTax = new Amount(0.00, "GBP")
      val fakeViewModel = new GovernmentSpend(
        2014,
        utr,
        List(
          ("welfare", spendData),
          ("health", spendData),
          ("education", spendData),
          ("pension", spendData),
          ("national_debt_interest", spendData),
          ("defence", spendData),
          ("criminal_justice", spendData),
          ("transport", spendData),
          ("business_and_industry", spendData),
          ("government_administration", spendData),
          ("culture", spendData),
          ("environment", spendData),
          ("housing_and_utilities", spendData),
          ("overseas_aid", spendData),
          ("uk_contribution_to_eu_budget", spendData),
          ("gov_spend_total", spendData)
        ),
        "",
        "",
        "",
        amount,
        "",
        scottishIncomeTax
      )
      val result = governmentSpendingView(fakeViewModel, (20.0, 20.0, 20.0))(
        requestWithSession,
        messages,
        formPartialRetriever,
        templateRenderer,
        appConfig)
      val document = Jsoup.parse(contentAsString(result))
      document.select("#global-breadcrumb li:nth-child(1) a").attr("href") should include("/annual-tax-summary")
      document.select("#global-breadcrumb li:nth-child(1) a").text shouldBe "Select the tax year"

      document.select("#global-breadcrumb li:nth-child(2) a").attr("href") should include(
        "/annual-tax-summary/main?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(2) a").text shouldBe "Your Annual Tax Summary"

      document.select("#global-breadcrumb li:nth-child(3)").toString should include(
        "<strong>Your taxes and public spending</strong>")
    }
  }
}
