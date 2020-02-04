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

package controllers

import config.AppFormPartialRetriever
import controllers.auth.{AuthenticatedRequest, FakeAuthAction}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest.MustMatchers._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Play.current
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{AuditService, SummaryService}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestConstants._
import view_models.{Amount, NoATSViewModel, Rate, Summary}

import scala.concurrent.Future

class NicsSummaryControllerSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar with I18nSupport {

  override def messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]

  val taxYear = 2014
  val request = AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest("GET", s"?taxYear=$taxYear"))
  val badRequest = AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest("GET","?taxYear=20145"))
  val dataPath = "/summary_json_test.json"

  trait TestController extends NicsController {

    override lazy val auditService = mock[AuditService]
    override lazy val summaryService = mock[SummaryService]
    implicit lazy val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever
    override val authAction = FakeAuthAction
    val model = Summary(
      year = 2014,
      utr = testUtr,
      employeeNicAmount = Amount(1200, "GBP"),
      employerNicAmount = Amount(1300, "GBP"),
      totalIncomeTaxAndNics = Amount(1400, "GBP"),
      yourTotalTax = Amount(1800, "GBP"),
      totalTaxFree = Amount(9440, "GBP"),
      totalTaxFreeAllowance = Amount(9740, "GBP"),
      yourIncomeBeforeTax = Amount(11600, "GBP"),
      totalIncomeTaxAmount = Amount(372, "GBP"),
      totalCapitalGainsTax = Amount(5500, "GBP"),
      taxableGains = Amount(20000, "GBP"),
      cgTaxPerCurrencyUnit = Amount(0.1234, "GBP"),
      nicsAndTaxPerCurrencyUnit = Amount(0.5678, "GBP"),
      nicsAndTaxRateAmount = Amount(56.78, "PERCENT"),
      totalCgTaxRate = Rate("12.34%"),
      nicsAndTaxRate = Rate("56.78%"),
      incomeAfterTaxAndNics = Amount(0.5678, "GBP"),
      title = "Mr",
      forename = "forename",
      surname = "surname"
    )

    when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model))
  }

  "Calling NICs" should {

    "return a successful response for a valid request" in new TestController {
      val result =  Future.successful(show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.title should include(Messages("ats.nics.tax_and_nics.title")+ Messages("generic.to_from", (taxYear-1).toString, taxYear.toString))
    }

    "display an error page for an invalid request" in new TestController {
      val result = Future.successful(show(badRequest))
      status(result) shouldBe 400
      val document = Jsoup.parse(contentAsString(result))
      document.title should include(Messages("generic.error.html.title"))
    }

    "redirect to the no ATS page when there is no Annual Tax Summary data returned" in new TestController {

      when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(new NoATSViewModel))

      val result = Future.successful(show(request))
      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe routes.ErrorController.authorisedNoAts().url

    }

    "have the right user data in the view" in new TestController {

      val result = Future.successful(show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-income-tax-amt").text() shouldBe "£372"
   //   document.getElementById("total-cg-tax-rate").text() shouldBe "56.78%"
      document.getElementById("employee-nic-amount").text() shouldBe "£1,200"
      document.getElementById("total-income-tax-and-nics").text() shouldBe "£1,400"
      document.getElementById("user-info").text should include("forename surname")
      document.getElementById("user-info").text should include("Unique Taxpayer Reference: "+testUtr)
    }

    "show 'Income Tax and NICs' page with a correct breadcrumb" in new TestController {

      val result = Future.successful(show(request))
      val document = Jsoup.parse(contentAsString(result))

      document.select("#global-breadcrumb li:nth-child(1) a").attr("href") should include("/account")
      document.select("#global-breadcrumb li:nth-child(1) a").text should include("Home")

      document.select("#global-breadcrumb li:nth-child(2) a").attr("href") should include("/annual-tax-summary")
      document.select("#global-breadcrumb li:nth-child(2) a").text shouldBe "Select the tax year"

      document.select("#global-breadcrumb li:nth-child(3) a").attr("href") should include("annual-tax-summary/main?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(3) a").text shouldBe "Your Annual Tax Summary"

      document.select("#global-breadcrumb li:nth-child(4) a").attr("href") should include("/annual-tax-summary/summary?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(4) a").text shouldBe "Your income and taxes"

      document.select("#global-breadcrumb li:nth-child(5)").toString should include("<strong>Your Income Tax and National Insurance</strong>")
    }
  }
}
