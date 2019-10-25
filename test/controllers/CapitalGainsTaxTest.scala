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
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation}
import services.{AuditService, CapitalGainsService}
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import utils.AuthorityUtils
import utils.TestConstants._
import view_models.{Amount, CapitalGains, NoATSViewModel, Rate}

import scala.concurrent.Future

class CapitalGainsTaxTest extends UnitSpec with FakeTaxsPlayApplication with MockitoSugar {

  val user = User(AuthorityUtils.saAuthority(testOid, testUtr))
  val taxYear = 2014
  val request = FakeRequest("GET", s"?taxYear=$taxYear")
  val badRequest = FakeRequest("GET", "?taxYear=20145")
  val baseModel = CapitalGains(
    taxYear = 2014,
    utr = testUtr,
    taxableGains = Amount(20000, "GBP"),
    lessTaxFreeAmount = Amount(10600, "GBP"),
    payCgTaxOn = Amount(9400, "GBP"),
    entrepreneursReliefRateBefore = Amount(1111, "GBP"),
    entrepreneursReliefRateAmount = Amount(1000, "GBP"),
    ordinaryRateBefore = Amount(2222, "GBP"),
    ordinaryRateAmount = Amount(2000, "GBP"),
    upperRateBefore = Amount(3333, "GBP"),
    upperRateAmount = Amount(3000, "GBP"),
    adjustmentsAmount = Amount(500, "GBP"),
    totalCapitalGainsTaxAmount = Amount(5500, "GBP"),
    cgTaxPerCurrencyUnit = Amount(0.1234, "GBP"),
    entrepreneursReliefRateRate = Rate("10%"),
    ordinaryRateRate = Rate("18%"),
    upperRateRate = Rate("28%"),
    totalCgTaxRate = Rate("12.34%"),
    title = "Mr",
    forename = "forename",
    surname = "surname"
  )

  class TestController extends CapitalGainsTaxController {

    override lazy val capitalGainsService = mock[CapitalGainsService]
    override lazy val auditService = mock[AuditService]
    implicit lazy val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever

    when(
      capitalGainsService
        .getCapitalGains(Matchers.eq(taxYear))(Matchers.eq(user), Matchers.any(), Matchers.eq(request)))
      .thenReturn(Future.successful(baseModel))

  }

  "Calling Capital Gains with no session" should {

    "return a 303 response" in new TestController {

      val result = Future.successful(authorisedCapitalGains(request))
      status(result) shouldBe 303
    }
  }

  "Calling Capital Gains with session" should {

    "return a successful response for a valid request" in new TestController {
      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.title should include(
        Messages("ats.capital_gains_tax.html.title") + Messages(
          "generic.to_from",
          (taxYear - 1).toString,
          taxYear.toString))
    }

    "display an error page for an invalid request " in new TestController {
      val result = Future.successful(show(user, badRequest))
      status(result) shouldBe 400
      val document = Jsoup.parse(contentAsString(result))
      document.title should include(Messages("generic.error.html.title"))
    }

    "redirect to the no ATS page when there is no annual tax summary data returned" in new TestController {
      when(
        capitalGainsService
          .getCapitalGains(Matchers.eq(taxYear))(Matchers.eq(user), Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(new NoATSViewModel))
      val result = Future.successful(show(user, request))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.ErrorController.authorisedNoAts().url
    }

    "show Your Capital Gains section with the right user data" in new TestController {

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("taxable-gains").text() shouldBe "£20,000"
      document.getElementById("less-taxable-gains").text() shouldBe "minus £10,600 -£10,600"
      document.getElementById("cg-pay-tax-on").text() shouldBe "£9,400"
      document.getElementById("tax-period").text() shouldBe "2013 to 2014"
      document.getElementById("total-cg-tax-rate").text() shouldBe "12.34%"
      document.getElementById("user-info").text() should include("forename surname")
      document.getElementById("user-info").text() should include("Unique Taxpayer Reference: " + testUtr)
      document.select(".page-header h1").text shouldBe "Tax year: April 6 2013 to April 5 2014 Capital Gains Tax"
    }

    "show Capital Gains Tax section if total amount of capital gains to pay tax on is not 0.00" in new TestController {

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("cg-pay-tax-on").text() shouldBe "£9,400"
      document.getElementById("capital-gains-tax-section") should not be null
    }

    "hide Capital Gains Tax section if total amount of capital gains to pay tax on is 0.00" in new TestController {

      val model2 = baseModel.copy(
        payCgTaxOn = Amount(0, "GBP"),
        taxableGains = Amount(0, "GBP")
      )

      when(
        capitalGainsService
          .getCapitalGains(Matchers.eq(taxYear))(Matchers.eq(user), Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(model2))

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("cg-pay-tax-on").text() shouldBe "£0"
      document.getElementById("capital-gains-tax-section") should be(null)
    }

    "show Capital Gains Tax section with correct user data" in new TestController {

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("entrepreneurs-relief-rate-before").text() should equal("£1,111")
      document.getElementById("entrepreneurs-relief-rate").text() should equal("10%")
      document.getElementById("entrepreneurs-relief-rate-amount").text() should equal("£1,000")

      document.getElementById("ordinary-rate-before").text() should equal("£2,222")
      document.getElementById("ordinary-rate").text() should equal("18%")
      document.getElementById("ordinary-rate-amount").text() should equal("£2,000")

      document.getElementById("upper-rate-before").text() should equal("£3,333")
      document.getElementById("upper-rate").text() should equal("28%")
      document.getElementById("upper-rate-amount").text() should equal("£3,000")
    }

    "hide Entrepreneurs' Relief Rate field if the amount on the left side is 0.00" in new TestController {

      val model3 = baseModel.copy(
        entrepreneursReliefRateBefore = Amount(0, "GBP")
      )

      when(
        capitalGainsService
          .getCapitalGains(Matchers.eq(taxYear))(Matchers.eq(user), Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(model3))

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString should not include ("Technical Difficulties")
      document.getElementById("entrepreneurs-relief-rate-section") should be(null)
    }

    "hide Ordinary Rate field if the amount on the left side is 0.00" in new TestController {

      val model4 = baseModel.copy(
        ordinaryRateBefore = Amount(0, "GBP")
      )

      when(
        capitalGainsService
          .getCapitalGains(Matchers.eq(taxYear))(Matchers.eq(user), Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(model4))

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString should not include ("Technical Difficulties")
      document.getElementById("ordinary-rate-section") should be(null)
    }

    "hide Upper Rate field if the amount on the left side is 0.00" in new TestController {

      val model5 = baseModel.copy(
        upperRateBefore = Amount(0, "GBP")
      )

      when(
        capitalGainsService
          .getCapitalGains(Matchers.eq(taxYear))(Matchers.eq(user), Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(model5))

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString should not include ("Technical Difficulties")
      document.getElementById("upper-rate-section") should be(null)
    }

    "show Adjustments section with correct user data" in new TestController {

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("adjustment-to-capital-gains-tax-amount").text() should equal("minus £500 -£500")
    }

    "show Total Capital Gains Tax with correct user data" in new TestController {

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-capital-gains-tax-amount").text() should equal("£5,500")
    }

    "hide Adjustments section if the Adjustments amount is 0.00" in new TestController {

      val model6 = baseModel.copy(
        adjustmentsAmount = Amount(0, "GBP")
      )

      when(
        capitalGainsService
          .getCapitalGains(Matchers.eq(taxYear))(Matchers.eq(user), Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(model6))

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString should not include ("Technical Difficulties")
      document.getElementById("adjustments-section") should be(null)
    }

    "show capital gains description if total capital gains tax is not 0" in new TestController {

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-cg-description") should not be null
      document.getElementById("total-cg-tax-rate").text() should equal("12.34%")
    }

    "hide capital gains description if total capital gains tax is 0" in new TestController {

      val model7 = baseModel.copy(
        totalCapitalGainsTaxAmount = Amount(0, "GBP")
      )

      when(
        capitalGainsService
          .getCapitalGains(Matchers.eq(taxYear))(Matchers.eq(user), Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(model7))

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.toString should not include ("Technical Difficulties")
      document.getElementById("total-cg-description") should be(null)
    }

    "show 'Capital Gains tax' page with a correct breadcrumb" in new TestController {

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.select("#global-breadcrumb li:nth-child(1) a").attr("href") should include("/account")
      document.select("#global-breadcrumb li:nth-child(1) a").text should include("Home")

      document.select("#global-breadcrumb li:nth-child(2) a").attr("href") should include("/annual-tax-summary")
      document.select("#global-breadcrumb li:nth-child(2) a").text shouldBe "Select the tax year"

      document.select("#global-breadcrumb li:nth-child(3) a").attr("href") should include(
        "annual-tax-summary/main?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(3) a").text shouldBe "Your annual tax summary"

      document.select("#global-breadcrumb li:nth-child(4) a").attr("href") should include(
        "/annual-tax-summary/summary?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(4) a").text shouldBe "Your income and taxes"

      document.select("#global-breadcrumb li:nth-child(5)").toString should include(
        "<strong>Capital Gains Tax</strong>")
    }
  }
}
