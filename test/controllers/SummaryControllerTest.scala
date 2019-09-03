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

package controllers

import config.AppFormPartialRetriever
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest.MustMatchers._
import org.scalatest.mock.MockitoSugar
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services._
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import utils.AuthorityUtils
import utils.TestConstants._
import view_models.{Amount, NoATSViewModel, Rate, Summary}

import scala.concurrent.Future
import scala.math.BigDecimal.double2bigDecimal


object SummaryControllerTest {

  val baseModel = Summary(
    year = 2014,
    utr = testUtr,
    employeeNicAmount = Amount(1200, "GBP"),
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
    totalCgTaxRate = Rate("12.34%"),
    nicsAndTaxRate = Rate("56.78%"),
    title = "Mr",
    forename = "forename",
    surname = "surname"
  )
}

class SummaryControllerTest extends UnitSpec with FakeTaxsPlayApplication with MockitoSugar {

  val taxYear = 2014
  val request = FakeRequest("Get", s"?taxYear=$taxYear")
  val badRequest = FakeRequest("GET","?taxYear=20145")
  val user = User(AuthorityUtils.saAuthority(testOid, testUtr))
  val baseModel = SummaryControllerTest.baseModel

  trait TestController extends SummaryController {

    override lazy val summaryService = mock[SummaryService]
    override lazy val auditService = mock[AuditService]
    implicit lazy val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever

    when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.eq(user), Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(baseModel))

  }

  "Calling Summary with no session" should {

    "return a 303 response" in new TestController {

      val result = Future.successful(authorisedSummaries(request))
      status(result) shouldBe 303
    }
  }

  "Calling Summary with session" should {

    "return a successful response for a valid request" in new TestController {
      val result =  Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.title should include(Messages("ats.summary.title")+ Messages("generic.to_from", (taxYear-1).toString, taxYear.toString))
    }

    "display an error page for an invalid request" in new TestController {
      val result = Future.successful(show(user, badRequest))
      status(result) shouldBe 400
      val document = Jsoup.parse(contentAsString(result))
      document.title should include(Messages("generic.error.html.title"))
    }

    "redirect to the no ATS page when there is no annual tax summary data returned" in new TestController {
      when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.eq(user), Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(new NoATSViewModel))
      val result = Future.successful(show(user, request))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.ErrorController.authorisedNoAts().url
    }

    "have the right user data in the view" in new TestController {

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString contains "Tax calculation"
      document.getElementById("income-before-tax-amount").text() shouldBe "£11,600"
      document.getElementById("user-info").text should include("forename surname")
      document.getElementById("user-info").text should include("Unique Taxpayer Reference: "+testUtr)
    }

    "have the correct tax free amount" in new TestController {

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-tax-free-amount").text() shouldBe "£9,740"
    }

    "still show a 0 tax free amount" in new TestController {

      val model2 = baseModel.copy(
        totalTaxFreeAllowance = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.eq(user), Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model2))

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-tax-free-amount").text() shouldBe "£0"
    }

    "show total income tax and NICs value on the summary page" in new TestController {

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-income-tax-and-nics").text() shouldBe "£1,400"
    }

    "show capital gains (and description) on the summary if capital gains is not 0" in new TestController {

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("capital-gains") should not be null
    }

    "not show capital gains on the summary if capital gains is 0" in new TestController {

      val model3 = baseModel.copy(
        taxableGains = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.eq(user), Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model3))

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString should not include("Technical Difficulties")
      document.getElementById("capital-gains") should be(null)
    }

    "show Total Capital Gains Tax value" in new TestController {

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-capital-gains-tax").text() should equal("£5,500")
    }

    "show capital gains description on the summary if total capital gains tax is not 0" in new TestController {

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-cg-description") should not be null
    }

    "hide capital gains description on the summary if total capital gains tax is  0" in new TestController {

      val model4 = baseModel.copy(
        totalCapitalGainsTax = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.eq(user), Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model4))

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString should not include("Technical Difficulties")
      document.getElementById("total-cg-description") should be (null)
    }

    "show zero in Total Income Tax value" in new TestController {

      val model5 = baseModel.copy(
        totalIncomeTaxAndNics = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.eq(user), Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model5))

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-income-tax-and-nics").text() should equal("£0")
    }

    "show Tax and Nics description having (income tax and employee nics)" in new TestController {

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("tax-and-nics-title").text() should equal("Income Tax and National Insurance")
    }

    "show Tax and Nics description having only (total income tax)" in new TestController {

      val model6 = baseModel.copy(
        employeeNicAmount = Amount(0, "GBP"),
        totalCapitalGainsTax = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.eq(user), Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model6))

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("tax-and-nics-title").text() should equal("Your tax was calculated as")
    }

    "show Tax and Nics description having only (capital gains)" in new TestController {

      val model7 = baseModel.copy(
        totalIncomeTaxAmount = Amount(0, "GBP"),
        employeeNicAmount = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.eq(user), Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model7))

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("tax-and-nics-title").text() should equal("Your tax was calculated as")
    }

    "show Tax and Nics description having only (employee nics)" in new TestController {

      val model8 = baseModel.copy(
        totalIncomeTaxAmount = Amount(0, "GBP"),
        totalCapitalGainsTax = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.eq(user), Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model8))

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("tax-and-nics-title").text() should equal("Your NICs were calculated as")
    }

    "show Tax and Nics description having only (total income tax, employee nics)" in new TestController {

      val model9 = baseModel.copy(
        totalCapitalGainsTax = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.eq(user), Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model9))

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("tax-and-nics-title").text() should equal("Income Tax and National Insurance")
    }

    "show Tax and Nics description having only (capital gains, employee nics)" in new TestController {

      val model10 = baseModel.copy(
        totalIncomeTaxAmount = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.eq(user), Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model10))

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("tax-and-nics-title").text() should equal("Your NICs were calculated as")
    }

    "show Your Total Tax as sum of Income Tax, capital gains and employee nics)" in new TestController {

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-tax-amount").text() shouldBe "£1,800"
    }

    "show Your Total Tax description having (total income tax, capital gains, employee nics)" in new TestController {

      when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.eq(user), Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(baseModel))

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-tax-description").text() should equal("Your total Income Tax, National Insurance and Capital Gains Tax.")
    }

    "show Your Total Tax description having only (total income tax)" in new TestController {

      val model11 = baseModel.copy(
        employeeNicAmount = Amount(0, "GBP"),
        totalCapitalGainsTax = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.eq(user), Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model11))

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-tax-description").text() should equal("Your total Income Tax.")
    }

    "show Your Total Tax description having only (capital gains)" in new TestController {

       val model12 = baseModel.copy(
        totalIncomeTaxAmount = Amount(0, "GBP"),
        employeeNicAmount = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.eq(user), Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model12))


      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-tax-description").text() should equal("Your Capital Gains Tax.")
    }

    "show Your Total Tax description having only (employee nics)" in new TestController {

       val model13 = baseModel.copy(
        totalIncomeTaxAmount = Amount(0, "GBP"),
        totalCapitalGainsTax = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.eq(user), Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model13))

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-tax-description").text() should equal("Your National Insurance.")
    }

    "show Your Total Tax description having only (total income tax, capital gains)" in new TestController {

       val model14 = baseModel.copy(
        employeeNicAmount = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.eq(user), Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model14))


      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-tax-description").text() should equal("Your total Income Tax and Capital Gains Tax.")
    }

    "show Your Total Tax description having only (total income tax, employee nics)" in new TestController {

      val model15 = baseModel.copy(
        totalCapitalGainsTax = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.eq(user), Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model15))


      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-tax-description").text() should equal("Your total Income Tax and National Insurance.")
    }

    "show Your Total Tax description having only (capital gains, employee nics)" in new TestController {

       val model16 = baseModel.copy(
        totalIncomeTaxAmount = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.eq(user), Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model16))


      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-tax-description").text() should equal("Your National Insurance and Capital Gains Tax.")
    }

    "show 'Summary' page with a correct breadcrumb" in new TestController {

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.select("#global-breadcrumb li:nth-child(1) a").attr("href") should include("/account")
      document.select("#global-breadcrumb li:nth-child(1) a").text should include("Home")

      document.select("#global-breadcrumb li:nth-child(2) a").attr("href") should include("/annual-tax-summary")
      document.select("#global-breadcrumb li:nth-child(2) a").text shouldBe "Select the tax year"

      document.select("#global-breadcrumb li:nth-child(3) a").attr("href") should include("/annual-tax-summary/main?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(3) a").text shouldBe "Your annual tax summary"

      document.select("#global-breadcrumb li:nth-child(4)").toString should include("<strong>Your income and taxes</strong>")
    }

    "Redirect to 'No ATS' page" in new TestController {

      val model17 = new NoATSViewModel

      when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.eq(user), Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model17))

      val result = Future.successful(show(user, request))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/annual-tax-summary/no-ats")
    }
  }
}
