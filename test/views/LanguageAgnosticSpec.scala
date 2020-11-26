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

package views

import controllers.auth.AuthenticatedRequest
import models.SpendData
import org.jsoup.Jsoup
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{HtmlUnitFactory, OneBrowserPerSuite, OneServerPerSuite}
import play.api.i18n.{Lang, Messages, MessagesImpl}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.{SaUtr, Uar}
import utils.AttorneyUtils
import utils.TestConstants._
import view_models.AtsForms._
import view_models._
import views.html.TaxsMainView
import views.html.GovernmentSpendingView
import views.html.TaxsIndexView
import views.html.SummaryView
import views.html.errors.GenericErrorView

class LanguageAgnosticSpec extends ViewSpecBase with HtmlUnitFactory with MockitoSugar {

  val request = AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest())
  val utr = testUtr
  lazy val taxsMainView = inject[TaxsMainView]
  lazy val governmentSpendingView = inject[GovernmentSpendingView]
  lazy val summaryView = inject[SummaryView]
  lazy val genericErrorView = inject[GenericErrorView]

  "Logging in with English language settings" should {
    "show the correct contents of the generic error page in English with welsh language switch enabled" in {
      val result = genericErrorView()(request, messages, formPartialRetriever, templateRenderer, appConfig)
      val document = Jsoup.parse(contentAsString(result))
      document.select("#generic-error-page-heading").text should include("Sorry, there is a problem with the service")
      document.getElementById("switchToWelsh").text should include("Cymraeg")
    }
  }

  "Logging in with invalid language settings" should {
    "show the correct contents of the generic error page in English" in {
      val result = genericErrorView()(request, messages, formPartialRetriever, templateRenderer, appConfig)
      val document = Jsoup.parse(contentAsString(result))
      document.select("#generic-error-page-heading").text should include("Sorry, there is a problem with the service")
    }
  }

  "Logging in with Welsh language settings" should {
    "show the correct contents of the generic error page in Welsh with english language switch enabled" in {
      implicit val messages: Messages = MessagesImpl(Lang("cy"), messagesApi)
      val result = genericErrorView()(request, messages, formPartialRetriever, templateRenderer, appConfig)
      val document = Jsoup.parse(contentAsString(result))
      document.select("#generic-error-page-heading").text should include(
        "Mae’n ddrwg gennym, mae problem gyda’r gwasanaeth")
      document.getElementById("switchToEnglish").text should include("English")
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
      implicit val messages: Messages = MessagesImpl(Lang("cy"), messagesApi)
      val language = Lang("cy-GB")
      val requestWithSession = AuthenticatedRequest(
        "userId",
        None,
        Some(SaUtr(testUtr)),
        None,
        None,
        None,
        None,
        FakeRequest().withSession("TAXS_USER_TYPE" -> "PORTAL"))
      val result =
        taxsMainView(fakeViewModel)(requestWithSession, messages, formPartialRetriever, templateRenderer, appConfig)
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
      implicit val messages: Messages = MessagesImpl(Lang("cy"), messagesApi)
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
      val result =
        governmentSpendingView(fakeViewModel, (20.0, 20.0, 20.0))(
          request,
          messages,
          formPartialRetriever,
          templateRenderer,
          appConfig)
      val document = Jsoup.parse(contentAsString(result))
      document.select("#content h1").text should include("Eich trethi a gwariant cyhoeddus")
      document
        .select(".lede")
        .text shouldBe "Mae hwn yn dangos dadansoddiad o sut mae’r llywodraeth wedi gwario eich trethi, neu sut y byddant yn eu gwario."

    }

    "show the summary page in welsh language" in {
      val amount = new Amount(0.00, "GBP")
      val rate = new Rate("5")
      implicit val messages: Messages = MessagesImpl(Lang("cy"), messagesApi)
      val language = Lang("cy-GB")
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
      val agentRequestWithSession = AuthenticatedRequest(
        "userId",
        Some(Uar(testUar)),
        None,
        None,
        None,
        None,
        None,
        FakeRequest().withSession("TAXS_USER_TYPE" -> "PORTAL"))
      val actingAsAttorneyFor = AttorneyUtils.getActingAsAttorneyFor(
        agentRequestWithSession,
        fakeViewModel.forename,
        fakeViewModel.surname,
        fakeViewModel.utr)
      val result = summaryView(fakeViewModel, actingAsAttorneyFor)(
        language,
        agentRequestWithSession,
        messages,
        formPartialRetriever,
        templateRenderer,
        appConfig)
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
