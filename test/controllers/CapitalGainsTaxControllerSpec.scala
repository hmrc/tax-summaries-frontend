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

import controllers.auth.{AuthenticatedRequest, FakeAuthAction}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.MustMatchers._
import play.api.http.Status.SEE_OTHER
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation}
import services.{AuditService, CapitalGainsService}
import uk.gov.hmrc.domain.SaUtr
import utils.TestConstants
import view_models.{Amount, NoATSViewModel}

import scala.concurrent.Future

class CapitalGainsTaxControllerSpec extends ControllerBaseSpec with TestConstants with BeforeAndAfterEach {

  val taxYear = 2014
  val request = AuthenticatedRequest(
    "userId",
    None,
    Some(SaUtr(testUtr)),
    None,
    None,
    None,
    None,
    FakeRequest("GET", s"?taxYear=$taxYear"))
  val badRequest = AuthenticatedRequest(
    "userId",
    None,
    Some(SaUtr(testUtr)),
    None,
    None,
    None,
    None,
    FakeRequest("GET", "?taxYear=20145"))
  val baseModel = capitalGains

  val mockCapitalGainsService = mock[CapitalGainsService]
  val mockAuditService = mock[AuditService]

  def sut =
    new CapitalGainsTaxController(
      mockCapitalGainsService,
      mockAuditService,
      FakeAuthAction,
      mcc,
      capitalGainsView,
      genericErrorView,
      tokenErrorView)

  override def beforeEach(): Unit =
    when(mockCapitalGainsService.getCapitalGains(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
      .thenReturn(Future.successful(baseModel))

  "Calling Capital Gains" should {

    "return a successful response for a valid request" in {
      val result = Future.successful(sut.show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.title should include(
        Messages("ats.capital_gains_tax.html.title") + Messages(
          "generic.to_from",
          (taxYear - 1).toString,
          taxYear.toString))
    }

    "display an error page for an invalid request " in {
      val result = Future.successful(sut.show(badRequest))
      status(result) shouldBe 400
      val document = Jsoup.parse(contentAsString(result))
      document.title should include(Messages("generic.error.html.title"))
    }

    "redirect to the no ATS page when there is no Annual Tax Summary data returned" in {
      when(mockCapitalGainsService.getCapitalGains(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(new NoATSViewModel))
      val result = Future.successful(sut.show(request))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.ErrorController.authorisedNoAts().url
    }

    "show Your Capital Gains section with the right user data" in {

      val result = Future.successful(sut.show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("taxable-gains").text() shouldBe "£20,000"
      document.getElementById("less-taxable-gains").text() shouldBe "minus £10,600 -£10,600"
      document.getElementById("cg-pay-tax-on").text() shouldBe "£9,400"
      document.getElementById("tax-period").text() shouldBe "2013 to 2014"
      document.getElementById("total-cg-tax-rate").text() shouldBe "12.34%"
      document.getElementById("user-info").text() should include("forename surname")
      document.getElementById("user-info").text() should include("Unique Taxpayer Reference: " + testUtr)
      document.select("h1").text shouldBe "Tax year: April 6 2013 to April 5 2014 Capital Gains Tax"
    }

    "show Capital Gains Tax section if total amount of capital gains to pay tax on is not 0.00" in {

      val result = Future.successful(sut.show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("cg-pay-tax-on").text() shouldBe "£9,400"
      document.getElementById("capital-gains-tax-section") should not be null
    }

    "hide Capital Gains Tax section if total amount of capital gains to pay tax on is 0.00" in {

      val model2 = baseModel.copy(
        payCgTaxOn = Amount(0, "GBP"),
        taxableGains = Amount(0, "GBP")
      )

      when(mockCapitalGainsService.getCapitalGains(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(model2))

      val result = Future.successful(sut.show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("cg-pay-tax-on").text() shouldBe "£0"
      document.getElementById("capital-gains-tax-section") should be(null)
    }

    "show Capital Gains Tax section with correct user data" in {

      val result = Future.successful(sut.show(request))
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

    "hide Entrepreneurs' Relief Rate field if the amount on the left side is 0.00" in {

      val model3 = baseModel.copy(
        entrepreneursReliefRateBefore = Amount(0, "GBP")
      )

      when(mockCapitalGainsService.getCapitalGains(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(model3))

      val result = Future.successful(sut.show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString should not include ("Technical Difficulties")
      document.getElementById("entrepreneurs-relief-rate-section") should be(null)
    }

    "hide Ordinary Rate field if the amount on the left side is 0.00" in {

      val model4 = baseModel.copy(
        ordinaryRateBefore = Amount(0, "GBP")
      )

      when(mockCapitalGainsService.getCapitalGains(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(model4))

      val result = Future.successful(sut.show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString should not include ("Technical Difficulties")
      document.getElementById("ordinary-rate-section") should be(null)
    }

    "hide Upper Rate field if the amount on the left side is 0.00" in {

      val model5 = baseModel.copy(
        upperRateBefore = Amount(0, "GBP")
      )

      when(mockCapitalGainsService.getCapitalGains(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(model5))

      val result = Future.successful(sut.show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString should not include ("Technical Difficulties")
      document.getElementById("upper-rate-section") should be(null)
    }

    "show Adjustments section with correct user data" in {

      val result = Future.successful(sut.show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("adjustment-to-capital-gains-tax-amount").text() should equal("minus £500 -£500")
    }

    "show Total Capital Gains Tax with correct user data" in {

      val result = Future.successful(sut.show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-capital-gains-tax-amount").text() should equal("£5,500")
    }

    "hide Adjustments section if the Adjustments amount is 0.00" in {

      val model6 = baseModel.copy(
        adjustmentsAmount = Amount(0, "GBP")
      )

      when(mockCapitalGainsService.getCapitalGains(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(model6))

      val result = Future.successful(sut.show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString should not include ("Technical Difficulties")
      document.getElementById("adjustments-section") should be(null)
    }

    "show capital gains description if total capital gains tax is not 0" in {

      val result = Future.successful(sut.show(request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-cg-description") should not be null
      document.getElementById("total-cg-tax-rate").text() should equal("12.34%")
    }

    "hide capital gains description if total capital gains tax is 0" in {

      val model7 = baseModel.copy(
        totalCapitalGainsTaxAmount = Amount(0, "GBP")
      )

      when(mockCapitalGainsService.getCapitalGains(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(model7))

      val result = Future.successful(sut.show(request))
      val document = Jsoup.parse(contentAsString(result))

      document.toString should not include ("Technical Difficulties")
      document.getElementById("total-cg-description") should be(null)
    }

    "show 'Capital Gains tax' page with a correct breadcrumb" in {

      val result = Future.successful(sut.show(request))
      val document = Jsoup.parse(contentAsString(result))

      document.select("#global-breadcrumb li:nth-child(1) a").attr("href") should include("/account")
      document.select("#global-breadcrumb li:nth-child(1) a").text should include("Home")

      document.select("#global-breadcrumb li:nth-child(2) a").attr("href") should include("/annual-tax-summary")
      document.select("#global-breadcrumb li:nth-child(2) a").text shouldBe "Select the tax year"

      document.select("#global-breadcrumb li:nth-child(3) a").attr("href") should include(
        "annual-tax-summary/main?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(3) a").text shouldBe "Your Annual Tax Summary"

      document.select("#global-breadcrumb li:nth-child(4) a").attr("href") should include(
        "/annual-tax-summary/summary?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(4) a").text shouldBe "Your income and taxes"

      document.select("#global-breadcrumb li:nth-child(5)").toString should include(
        "<strong>Capital Gains Tax</strong>")
    }
  }
}
