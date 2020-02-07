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
import controllers.auth.AuthenticatedRequest
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest.MustMatchers._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services._
import services.paye.PayeSummaryService
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestConstants._
import view_models.paye.PayeSummary
import view_models.{Amount, NoATSViewModel, Rate}

import scala.concurrent.Future

class PayeAtsMainControllerSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar with I18nSupport {

  override def messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]

  val taxYear = 2014
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
  val request = AuthenticatedRequest("userId", None, None, Some(Nino(testNino)), None, None, None, FakeRequest("GET", s"?taxYear=$taxYear"))
  val badRequest = AuthenticatedRequest("userId", None, None, Some(Nino(testNino)), None, None, None, FakeRequest("GET","?taxYear=20145"))

  trait TestController extends PayeAtsMainController {
    override lazy val summaryService = mock[PayeSummaryService]
    override lazy val auditService = mock[AuditService]
    implicit lazy val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever
    override val authAction: PayeAuthAction = PayeFakeAuthAction
    when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(baseModel))

  }

  "Calling Index Page" should {

    "return a successful response for a valid request" in new TestController {
      val result =  Future.successful(show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.title should include(Messages("ats.index.html.title")+ Messages("generic.to_from", (taxYear-1).toString, taxYear.toString))
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
      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe 200
      document.getElementById("tax-calc-link").text shouldBe "Your income and taxes"
      document.getElementById("tax-services-link").text shouldBe "How your tax was spent"
      document.getElementById("index-page-header").text shouldBe "Your Annual Tax Summary 6 April 2013 to 5 April 2014"
      document.getElementById("index-page-description").text shouldBe "This shows you how your Income Tax and National Insurance contributions were calculated and how your money is spent by the government."
      document.getElementById("tax-calc-link").tagName shouldBe "a"
      document.getElementById("tax-services-link").tagName shouldBe "a"
    }

    "display the right years" in new TestController {

      val model = baseModel.copy(
        year = 2015
      )

      when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model))

      val result = Future.successful(show(request))
      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe 200
      document.getElementById("index-page-header").text should include("2015")
    }

    "show 'Landing page' page with a correct breadcrumb" in new TestController {

      val result = Future.successful(show(request))
      val document = Jsoup.parse(contentAsString(result))

      document.select("#global-breadcrumb li:nth-child(1) a").attr("href") should include("/account")
      document.select("#global-breadcrumb li:nth-child(1) a").text should include("Home")

      document.select("#global-breadcrumb li:nth-child(2)").toString should include("Your Annual Tax Summary")
    }
  }
}
