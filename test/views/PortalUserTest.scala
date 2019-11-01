/*
 * Copyright 2019 HM Revenue & Customs
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

import config.AppFormPartialRetriever
import models.SpendData
import org.jsoup.Jsoup
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{HtmlUnitFactory, OneBrowserPerSuite, OneServerPerSuite}
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.api.test.FakeRequest
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import utils.AuthorityUtils
import view_models._
import utils.TestConstants._

class PortalUserTest
    extends UnitSpec with OneServerPerSuite with OneBrowserPerSuite with HtmlUnitFactory with MockitoSugar {

  val request = FakeRequest()
  val language = Lang("en")
  val user = User(AuthorityUtils.saAuthority(testOid, testUtr))
  val amount = new Amount(0.00, "GBP")
  val rate = new Rate("5")
  val utr = testUtr

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val messages = Messages(language, messagesApi)
  implicit val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever

  "Logging in as a portal user" should {

    "show the 'exit tax summaries' link on the landing page" in {

      val fakeViewModel = Summary(
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
      val result = views.html.taxs_main(fakeViewModel)(
        request.withSession("TAXS_USER_TYPE" -> "PORTAL"),
        messages,
        language,
        formPartialRetriever)
      val document = Jsoup.parse(contentAsString(result))

      document.select("#proposition-links a").text should include("Back to HMRC Online Services")
      val href = document.select("#proposition-links a").first().attr("href")
      href should be("https://online.hmrc.gov.uk/self-assessment/ind/" + utr)
    }

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
        rate,
        rate,
        rate,
        rate,
        "",
        "",
        "")
      val result = views.html.capital_gains(fakeViewModel)(
        language,
        request.withSession("TAXS_USER_TYPE" -> "PORTAL"),
        messages,
        formPartialRetriever)
      val document = Jsoup.parse(contentAsString(result))

      document.select("#proposition-links a").text should include("Back to HMRC Online Services")
      val href = document.select("#proposition-links a").first().attr("href")
      href should be("https://online.hmrc.gov.uk/self-assessment/ind/" + utr)

      document.select("#global-breadcrumb li:nth-child(1) a").attr("href") should include("/annual-tax-summary")
      document.select("#global-breadcrumb li:nth-child(1) a").text shouldBe "Select the tax year"

      document.select("#global-breadcrumb li:nth-child(2) a").attr("href") should include(
        "/annual-tax-summary/main?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(2) a").text shouldBe "Your annual tax summary"

      document.select("#global-breadcrumb li:nth-child(3) a").attr("href") should include(
        "/annual-tax-summary/summary?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(3) a").text shouldBe "Your income and taxes"

      document.select("#global-breadcrumb li:nth-child(4)").toString should include(
        "<strong>Capital Gains Tax</strong>")
    }

    "show the 'exit tax summaries' link on the income before tax page" in {

      val fakeViewModel =
        new IncomeBeforeTax(2014, utr, amount, amount, amount, amount, amount, amount, amount, amount, "", "", "")
      val result = views.html.income_before_tax(fakeViewModel)(
        language,
        request.withSession("TAXS_USER_TYPE" -> "PORTAL"),
        messages,
        formPartialRetriever)
      val document = Jsoup.parse(contentAsString(result))

      document.select("#proposition-links a").text should include("Back to HMRC Online Services")
      val href = document.select("#proposition-links a").first().attr("href")
      href should be("https://online.hmrc.gov.uk/self-assessment/ind/" + utr)

      document.select("#global-breadcrumb li:nth-child(1) a").attr("href") should include("/annual-tax-summary")
      document.select("#global-breadcrumb li:nth-child(1) a").text shouldBe "Select the tax year"

      document.select("#global-breadcrumb li:nth-child(2) a").attr("href") should include(
        "/annual-tax-summary/main?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(2) a").text shouldBe "Your annual tax summary"

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
      val result = views.html.nics(fakeViewModel)(
        language,
        request.withSession("TAXS_USER_TYPE" -> "PORTAL"),
        messages,
        formPartialRetriever)
      val document = Jsoup.parse(contentAsString(result))

      document.select("#proposition-links a").text should include("Back to HMRC Online Services")
      val href = document.select("#proposition-links a").first().attr("href")
      href should be("https://online.hmrc.gov.uk/self-assessment/ind/" + utr)

      document.select("#global-breadcrumb li:nth-child(1) a").attr("href") should include("/annual-tax-summary")
      document.select("#global-breadcrumb li:nth-child(1) a").text shouldBe "Select the tax year"

      document.select("#global-breadcrumb li:nth-child(2) a").attr("href") should include(
        "/annual-tax-summary/main?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(2) a").text shouldBe "Your annual tax summary"

      document.select("#global-breadcrumb li:nth-child(3) a").attr("href") should include(
        "/annual-tax-summary/summary?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(3) a").text should include("Your income and taxes")

      document.select("#global-breadcrumb li:nth-child(4)").toString should include(
        "<strong>Your Income Tax and National Insurance</strong>")
    }

    "show the 'exit tax summaries' link on the no ats page" in {

      val result = views.html.errors.no_ats_error()(
        language,
        request.withSession("TAXS_USER_TYPE" -> "PORTAL"),
        user,
        messages,
        formPartialRetriever)
      val document = Jsoup.parse(contentAsString(result))

      document.select("#proposition-links a").text should include("Back to HMRC Online Services")
      val href = document.select("#proposition-links a").first().attr("href")
      href should be("https://online.hmrc.gov.uk/self-assessment/ind/" + utr)

      document.select("#global-breadcrumb li:nth-child(1) a").attr("href") should include("/annual-tax-summary")
      document.select("#global-breadcrumb li:nth-child(1) a").text shouldBe "Select the tax year"

      document.select("#global-breadcrumb li:nth-child(2)").toString should include("<strong>No ATS available</strong>")
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
      val result = views.html.summary(fakeViewModel)(
        language,
        request.withSession("TAXS_USER_TYPE" -> "PORTAL"),
        messages,
        formPartialRetriever)
      val document = Jsoup.parse(contentAsString(result))

      document.select("#proposition-links a").text should include("Back to HMRC Online Services")

      val href = document.select("#proposition-links a").first().attr("href")
      href should be("https://online.hmrc.gov.uk/self-assessment/ind/" + utr)

      document.select("#global-breadcrumb li:nth-child(1) a").attr("href") should include("/annual-tax-summary")
      document.select("#global-breadcrumb li:nth-child(1) a").text shouldBe "Select the tax year"

      document.select("#global-breadcrumb li:nth-child(2) a").attr("href") should include(
        "/annual-tax-summary/main?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(2) a").text shouldBe "Your annual tax summary"

      document.select("#global-breadcrumb li:nth-child(3)").toString should include(
        "<strong>Your income and taxes</strong>")
    }

    "show the 'exit tax summaries' link on the tax free amount page" in {

      val fakeViewModel = new Allowances(2014, utr, amount, amount, amount, amount, "", "", "")
      val result = views.html.tax_free_amount(fakeViewModel)(
        language,
        request.withSession("TAXS_USER_TYPE" -> "PORTAL"),
        messages,
        formPartialRetriever)
      val document = Jsoup.parse(contentAsString(result))

      document.select("#proposition-links a").text should include("Back to HMRC Online Services")
      val href = document.select("#proposition-links a").first().attr("href")
      href should be("https://online.hmrc.gov.uk/self-assessment/ind/" + utr)

      document.select("#global-breadcrumb li:nth-child(1) a").attr("href") should include("/annual-tax-summary")
      document.select("#global-breadcrumb li:nth-child(1) a").text shouldBe "Select the tax year"

      document.select("#global-breadcrumb li:nth-child(2) a").attr("href") should include(
        "/annual-tax-summary/main?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(2) a").text shouldBe "Your annual tax summary"

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
        amount,
        amount,
        "",
        rate,
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
      val result = views.html.total_income_tax(fakeViewModel)(
        language,
        request.withSession("TAXS_USER_TYPE" -> "PORTAL"),
        messages,
        formPartialRetriever)
      val document = Jsoup.parse(contentAsString(result))

      document.select("#proposition-links a").text should include("Back to HMRC Online Services")
      val href = document.select("#proposition-links a").first().attr("href")
      href should be("https://online.hmrc.gov.uk/self-assessment/ind/" + utr)

      document.select("#global-breadcrumb li:nth-child(1) a").attr("href") should include("/annual-tax-summary")
      document.select("#global-breadcrumb li:nth-child(1) a").text shouldBe "Select the tax year"

      document.select("#global-breadcrumb li:nth-child(2) a").attr("href") should include(
        "/annual-tax-summary/main?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(2) a").text shouldBe "Your annual tax summary"

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
      val result = views.html.government_spending(fakeViewModel, (20.0, 20.0, 20.0))(
        language,
        request.withSession("TAXS_USER_TYPE" -> "PORTAL"),
        messages,
        formPartialRetriever)
      val document = Jsoup.parse(contentAsString(result))

      document.select("#proposition-links a").text should include("Back to HMRC Online Services")
      val href = document.select("#proposition-links a").first().attr("href")
      href should be("https://online.hmrc.gov.uk/self-assessment/ind/" + utr)

      document.select("#global-breadcrumb li:nth-child(1) a").attr("href") should include("/annual-tax-summary")
      document.select("#global-breadcrumb li:nth-child(1) a").text shouldBe "Select the tax year"

      document.select("#global-breadcrumb li:nth-child(2) a").attr("href") should include(
        "/annual-tax-summary/main?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(2) a").text shouldBe "Your annual tax summary"

      document.select("#global-breadcrumb li:nth-child(3)").toString should include(
        "<strong>Your taxes and public spending</strong>")
    }
    "show the menu link in the header if on a mobile device" in {

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
      val result = views.html.government_spending(fakeViewModel, (20.0, 20.0, 20.0))(
        language,
        request.withSession("TAXS_USER_TYPE" -> "PORTAL"),
        messages,
        formPartialRetriever)
      val document = Jsoup.parse(contentAsString(result))

      val menu_toggle = document.select(".js-header-toggle.menu")
      menu_toggle.text should include("Menu")

      val href = menu_toggle.attr("href")
      href should be("#proposition-links")
    }

    "contain GA event attribute on the landing page" in {

      val fakeViewModel = Summary(
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
      val result = views.html.taxs_main(fakeViewModel)(
        request.withSession("TAXS_USER_TYPE" -> "PORTAL"),
        messages,
        language,
        formPartialRetriever)
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("wrapper").attr("data-journey") should include("annual-tax-summary:portal-user:start")
    }
  }

  "Logging into the portal as an agent" should {

    "show the 'back to the Portal' link containing the client's UTR" in {

      val fakeViewModel = Summary(
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
      val result = views.html.taxs_main(fakeViewModel)(
        request.withSession("TAXS_USER_TYPE" -> "PORTAL"),
        messages,
        language,
        formPartialRetriever)
      val document = Jsoup.parse(contentAsString(result))

      document.select("#proposition-links a").text should include("Back to HMRC Online Services")
      val href = document.select("#proposition-links a").first().attr("href")
      href should be("https://online.hmrc.gov.uk/self-assessment/ind/" + utr)
    }
  }
}
