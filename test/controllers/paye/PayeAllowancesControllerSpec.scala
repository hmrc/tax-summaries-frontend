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
import controllers.auth.AuthenticatedRequest
import controllers.auth.paye.{PayeAuthAction, PayeFakeAuthAction}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest.MustMatchers._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services._
import services.paye.PayeAllowanceService
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import utils.TaxsController
import utils.TestConstants._
import view_models._
import view_models.paye.PayeAllowances

import scala.concurrent.Future

class PayeAllowancesControllerSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar with I18nSupport {

  override def messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]

  val taxYear = 2014

  val baseModel = PayeAllowances(
    taxYear = 2014,
    taxFreeAllowance = Amount(9440, "GBP"),
    marriageAllowanceTransferred = Amount(0, "GBP"),
    otherAllowances = Amount(300, "GBP"),
    youPayTaxOn = Amount(5000, "GBP"),
    totalTaxFree = Amount(9740, "GBP"),
    totalIncomeBeforeTax =  Amount(9740, "GBP")
  )

  val noATSViewModel: NoATSViewModel = new NoATSViewModel()

  lazy val taxsController = mock[TaxsController]

  trait TestController extends PayeAllowancesController {
    override lazy val allowanceService = mock[PayeAllowanceService]
    override lazy val auditService = mock[AuditService]
    implicit lazy val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever
    override val authAction: PayeAuthAction = PayeFakeAuthAction
    val model = baseModel
    val taxYear = 2014
    val request = AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest("GET", s"?taxYear=$taxYear"))
    val badRequest = AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest("GET","?taxYear=20155"))
    implicit val hc = new HeaderCarrier
    when(allowanceService.getAllowances(Matchers.eq(taxYear))(Matchers.eq(request),Matchers.any())).thenReturn(Future.successful(model))
  }

  "Calling allowances" should {

    "have the right user data in the view when a valid request is sent" in new TestController {

      val result = Future.successful(show(request))

      status(result) shouldBe 200

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("tax-free-total").text() shouldBe "£9,740"
      document.getElementById("tax-free-allowance-amount").text() shouldBe "£9,440"
      document.getElementById("other-allowances").text() shouldBe "£300"
      document.toString should include("tax-free-allowance")
      document.select("h1").text shouldBe "Tax free amount 6 April 2013 to 5 April 2014"
    }

    "have zero-value fields hidden in the view" in new TestController {

      override val model = baseModel.copy(
        taxFreeAllowance = Amount(0, "GBP"),
        otherAllowances = Amount(0, "GBP")
      )

      val result:Future[Result] = Future.successful(show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString should not include "tax-free-allowance-amount"
      document.toString should not include "other-allowances"
    }

    "show 'Allowances' page with a correct breadcrumb" in new TestController {

      val result = Future.successful(show(request))
      val document = Jsoup.parse(contentAsString(result))
      document.select("#global-breadcrumb li:nth-child(1) a").attr("href") should include("/account")
      document.select("#global-breadcrumb li:nth-child(1) a").text should include("Home")

      document.select("#global-breadcrumb li:nth-child(2) a").attr("href") should include("/annual-tax-summary/main?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(2) a").text shouldBe "Your Annual Tax Summary"

      document.select("#global-breadcrumb li:nth-child(3) a").attr("href") should include("/annual-tax-summary/summary?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(3) a").text shouldBe "Your income and taxes"

      document.select("#global-breadcrumb li:nth-child(4)").toString should include("<strong>Your tax-free amount</strong>")
    }

    "return a successful response for a valid request" in new TestController {
      val result =  Future.successful(show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.title should include(Messages("ats.tax_free_amount.html.title")+ Messages("generic.to_from", (taxYear-1).toString, taxYear.toString))
    }

    "display an error page for an invalid request" in new TestController {
      val result = Future.successful(show(badRequest))
      status(result) shouldBe 400
      val document = Jsoup.parse(contentAsString(result))
      document.title should include(Messages("generic.error.html.title"))
    }

    "redirect to the no ATS page when there is no annual tax summary data returned" in new TestController {
      when(allowanceService.getAllowances(Matchers.eq(taxYear))(Matchers.eq(request),Matchers.any())).thenReturn(Future.successful(new NoATSViewModel))
      val result = Future.successful(show(request))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.ErrorController.authorisedNoAts().url
    }

  }
}
