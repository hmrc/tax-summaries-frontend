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

package controllers.paye

import config.AppFormPartialRetriever
import controllers.auth.paye.{PayeAuthAction, PayeFakeAuthAction}
import controllers.auth.{AuthAction, AuthenticatedRequest, FakeAuthAction}
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
import services._
import services.paye.PayeSummaryService
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestConstants._
import view_models.paye.PayeSummary
import view_models.{Amount, NoATSViewModel, Rate, Summary}

import scala.concurrent.Future
import scala.math.BigDecimal.double2bigDecimal


object PayeSummaryControllerSpec {

  val baseModel = PayeSummary(
    year = 2014,
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
    nicsAndTaxRateAmount = Amount(0.5678, "GBP"),
    totalCgTaxRate = Rate("12.34%"),
    nicsAndTaxRate = Rate("56.78%"),
    incomeAfterTaxAndNics = Amount(0.5678, "GBP")
  )
}

class PayeSummaryControllerSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar with I18nSupport {

  override def messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]

  val taxYear = 2014
  val request = AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest("GET", s"?taxYear=$taxYear"))
  val badRequest = AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest("GET","?taxYear=20145"))
  val baseModel = PayeSummaryControllerSpec.baseModel

  trait TestController extends PayeSummaryController {

    override lazy val summaryService = mock[PayeSummaryService]
    override lazy val auditService = mock[AuditService]
    implicit lazy val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever
    override val authAction: PayeAuthAction = PayeFakeAuthAction

    when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(baseModel))

  }

  "Calling Summary" should {
      // TODO Commented out tests will pass when PAYE messages keys are used
    "return a successful response for a valid request" in new TestController {
      val result =  Future.successful(show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.title should include(Messages("ats.summary.title")+ Messages("generic.to_from", (taxYear-1).toString, taxYear.toString))
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
      redirectLocation(result).get mustBe controllers.routes.ErrorController.authorisedNoAts().url
    }

    "have the right user data in the view" in new TestController {

      val result = Future.successful(show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString contains "Tax calculation"
      document.getElementById("income-before-tax-amount").text() shouldBe "£11,600"
    }

    "have the correct tax free amount" in new TestController {

      val result = Future.successful(show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-tax-free-amount").text() shouldBe "£9,740"
    }

    "still show a 0 tax free amount" in new TestController {

      val model2 = baseModel.copy(
        totalTaxFreeAllowance = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model2))

      val result = Future.successful(show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-tax-free-amount").text() shouldBe "£0"
    }

    "show total income tax and NICs value on the summary page" in new TestController {

      val result = Future.successful(show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-income-tax-and-nics").text() shouldBe "£1,400"
    }


    "not show capital gains on the summary if capital gains is 0" in new TestController {

      val model3 = baseModel.copy(
        taxableGains = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model3))

      val result = Future.successful(show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString should not include("Technical Difficulties")
      document.getElementById("capital-gains") should be(null)
    }
    
    "show zero in Total Income Tax value" in new TestController {

      val model5 = baseModel.copy(
        totalIncomeTaxAndNics = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model5))

      val result = Future.successful(show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-income-tax-and-nics").text() should equal("£0")
    }

    "show Tax and Nics description having (income tax and employee nics)" in new TestController {

      val result = Future.successful(show(request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("tax-and-nics-title").text() should equal("Income Tax and National Insurance")
    }



    "show Tax and Nics description having only (total income tax, employee nics)" in new TestController {

      val model9 = baseModel.copy(
        totalCapitalGainsTax = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model9))

      val result = Future.successful(show(request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("tax-and-nics-title").text() should equal("Income Tax and National Insurance")
    }

    "show 'Summary' page with a correct breadcrumb" in new TestController {

      val result = Future.successful(show(request))
      val document = Jsoup.parse(contentAsString(result))

      document.select("#global-breadcrumb li:nth-child(1) a").attr("href") should include("/account")
      document.select("#global-breadcrumb li:nth-child(1) a").text should include("Home")

      document.select("#global-breadcrumb li:nth-child(2) a").attr("href") should include("/annual-tax-summary/main?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(2) a").text shouldBe "Your Annual Tax Summary"

      document.select("#global-breadcrumb li:nth-child(3)").toString should include("<strong>Your income and taxes</strong>")
    }

    "Redirect to 'No ATS' page" in new TestController {

      val model17 = new NoATSViewModel

      when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model17))

      val result = Future.successful(show(request))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/annual-tax-summary/no-ats")
    }
  }
}
