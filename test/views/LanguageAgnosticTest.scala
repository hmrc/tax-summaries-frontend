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
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{HtmlUnitFactory, OneBrowserPerSuite, OneServerPerSuite}
import utils.{AttorneyUtils, AuthorityUtils}
import models.SpendData
import org.jsoup.Jsoup
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.test.Helpers._
import play.api.test.FakeRequest
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import view_models.AtsForms._
import view_models._
import utils.TestConstants._

class LanguageAgnosticTest
    extends UnitSpec with OneServerPerSuite with OneBrowserPerSuite with HtmlUnitFactory with MockitoSugar {

  val request = FakeRequest()
  val language = Lang("en")
  val utr = testUtr
  val user = User(AuthorityUtils.saAuthority(testOid, utr))

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever

  "Logging in with English language settings" should {
    "show the correct contents of the generic error page in English" in {
      val language = Lang("en")
      implicit val messages = Messages(language, messagesApi)
      val result = views.html.errors.generic_error()(language, request, user, messages, formPartialRetriever)
      val document = Jsoup.parse(contentAsString(result))
      document.select("#generic-error-page-heading").text should include("Sorry, there is a problem with the service")
    }
  }

  "Logging in with invalid language settings" should {
    "show the correct contents of the generic error page in English" in {
      val language = Lang("xy")
      implicit val messages = Messages(language, messagesApi)
      val result = views.html.errors.generic_error()(language, request, user, messages, formPartialRetriever)
      val document = Jsoup.parse(contentAsString(result))
      document.select("#generic-error-page-heading").text should include("Sorry, there is a problem with the service")
    }
  }

  "Logging in with Welsh language settings" should {
    "show the correct contents of the generic error page in Welsh" in {
      val language = Lang("cy")
      implicit val messages = Messages(language, messagesApi)
      val result = views.html.errors.generic_error()(language, request, user, messages, formPartialRetriever)
      val document = Jsoup.parse(contentAsString(result))
      document.select("#generic-error-page-heading").text should include(
        "Mae’n ddrwg gennym, mae problem gyda’r gwasanaeth")
    }

    "show the English language switch" in {
      val language = Lang("cy-GB")
      implicit val messages = Messages(language, messagesApi)
      val atsList = AtsList("", "", "", List(TaxYearEnd(Some("2014")), TaxYearEnd(Some("2015"))))
      val result = views.html.taxs_index(atsList, atsYearFormMapping)(request, messages, formPartialRetriever, language)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("english-switch").text should include("English")
    }

    "show the index page in welsh language" in {
      val amount = new Amount(0.00, "GBP")
      val rate = new Rate("5")
      val fakeViewModel = Summary(
        2014,
        "1123",
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
      val language = Lang("cy-GB")
      implicit val messages = Messages(language, messagesApi)
      val result = views.html.taxs_main(fakeViewModel)(
        request.withSession("TAXS_USER_TYPE" -> "PORTAL"),
        messages,
        language,
        formPartialRetriever)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("index-page-header").text() should include("Eich crynodeb treth blynyddol")
      document
        .getElementById("index-page-description")
        .text() shouldBe "Mae hwn yn crynhoi eich treth bersonol a’ch Yswiriant Gwladol, " +
        "a sut mae’r llywodraeth yn eu gwario. Daw’r wybodaeth hon oddi wrthych chi, eich cyflogwr/cyflogwyr neu eich darparwr/darparwyr pensiwn."
    }

    "show the treasury spending page in welsh language" in {
      val amount = new Amount(0.00, "GBP")
      val totalAmount = new Amount(0.00, "GBP")
      val scottishIncomeTax = new Amount(0.00, "GBP")
      val spendData = new SpendData(amount, 20)
      val language = Lang("cy-GB")
      implicit val messages = Messages(language, messagesApi)
      val fakeViewModel = GovernmentSpend(
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
        totalAmount,
        "",
        scottishIncomeTax
      )
      val result = views.html
        .government_spending(fakeViewModel, (20.0, 20.0, 20.0))(language, request, messages, formPartialRetriever)
      val document = Jsoup.parse(contentAsString(result))

      document.select("#content h1").text should include("Eich trethi a gwariant cyhoeddus")
      document
        .select(".lede")
        .text shouldBe "Mae hwn yn dangos dadansoddiad o sut mae’r llywodraeth wedi gwario eich trethi, neu sut y byddant yn eu gwario."
    }

    "show the summary page in welsh language" in {
      val agentUser = User(AuthorityUtils.taxsAgentAuthority(testOid, testUar))
      val amount = new Amount(0.00, "GBP")
      val rate = new Rate("5")
      val language = Lang("cy-GB")
      implicit val messages = Messages(language, messagesApi)
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
        "Forename",
        "Surname")
      val actingAsAttorneyFor = AttorneyUtils
        .getActingAsAttorneyFor(agentUser, fakeViewModel.forename, fakeViewModel.surname, fakeViewModel.utr)
      val result = views.html.summary(fakeViewModel, actingAsAttorneyFor)(
        language,
        request.withSession("TAXS_USER_TYPE" -> "PORTAL"),
        messages,
        formPartialRetriever)
      val document = Jsoup.parse(contentAsString(result))

      document.select("h1").text should include("Eich incwm a’ch trethi")
      document.select(".link-back").text shouldBe "Yn ôl"
      document
        .select("#agent-banner")
        .text shouldBe "Rydych yn gweithredu ar ran Forename Surname (UTR: " + testUtr + ")."
      document
        .select("#total-tax-description")
        .text shouldBe "Cyfanswm eich Treth Incwm, Yswiriant Gwladol a, lle’n briodol, Treth Enillion Cyfalaf"
    }
  }
}
