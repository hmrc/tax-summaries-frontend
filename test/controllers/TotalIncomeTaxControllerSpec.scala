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

import controllers.auth.{AuthAction, AuthenticatedRequest, FakeAuthAction}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.MustMatchers._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services._
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestConstants._
import view_models._

import scala.concurrent.Future

class TotalIncomeTaxControllerSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar with I18nSupport with BeforeAndAfterEach {

  override def messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]

  val taxYear = 2014
  val request = AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest("GET", s"?taxYear=$taxYear"))
  val badRequest = AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest("GET","?taxYear=20145"))
  val baseModel = TotalIncomeTax(
    year = 2014,
    utr = testUtr,
    startingRateForSavings = Amount(110, "GBP"),
    startingRateForSavingsAmount = Amount(140, "GBP"),
    basicRateIncomeTax = Amount(1860, "GBP"),
    basicRateIncomeTaxAmount = Amount(372, "GBP"),
    higherRateIncomeTax = Amount(130, "GBP"),
    higherRateIncomeTaxAmount = Amount(70, "GBP"),
    additionalRateIncomeTax = Amount(80, "GBP"),
    additionalRateIncomeTaxAmount = Amount(60, "GBP"),
    ordinaryRate = Amount(100, "GBP"),
    ordinaryRateAmount = Amount(50, "GBP"),
    upperRate = Amount(30, "GBP"),
    upperRateAmount = Amount(120, "GBP"),
    additionalRate = Amount(10, "GBP"),
    additionalRateAmount = Amount(40, "GBP"),
    otherAdjustmentsIncreasing = Amount(90, "GBP"),
    marriageAllowanceReceivedAmount = Amount(0, "GBP"),
    otherAdjustmentsReducing = Amount(20, "GBP"),
    ScottishTax.empty,
    totalIncomeTax = Amount(372, "GBP"),
    scottishIncomeTax = Amount(100, "GBP"),
    SavingsTax.empty,
    incomeTaxStatus = "0002",
    startingRateForSavingsRateRate = Rate("10%"),
    basicRateIncomeTaxRateRate = Rate("20%"),
    higherRateIncomeTaxRateRate = Rate("40%"),
    additionalRateIncomeTaxRateRate = Rate("45%"),
    ordinaryRateTaxRateRate = Rate("10%"),
    upperRateRateRate = Rate("32.5%"),
    additionalRateRateRate = Rate("37.5%"),
    ScottishRates.empty,
    SavingsRates.empty,
    "Mr",
    "forename",
    "surname"
  )

  val mockTotalIncomeTaxService = mock[TotalIncomeTaxService]
  val mockAuditService = mock[AuditService]

  def sut = new TotalIncomeTaxController(mockTotalIncomeTaxService, mockAuditService, FakeAuthAction)

  override def beforeEach(): Unit = {
    when(mockTotalIncomeTaxService.getIncomeData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request))
    ) thenReturn Future.successful(baseModel)
  }

  "Calling Total Income Tax" should {

    "return a successful response for a valid request" in {
      val result =  Future.successful(sut.show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.title should include(Messages("ats.total_income_tax.income_tax")+ Messages("generic.to_from", (taxYear-1).toString, taxYear.toString))
    }

    "display an error page for an invalid request" in {
      val result = Future.successful(sut.show(badRequest))
      status(result) shouldBe 400
      val document = Jsoup.parse(contentAsString(result))
      document.title should include(Messages("generic.error.html.title"))
    }

    "redirect to the no ATS page when there is no annual tax summary data returned" in {
      when(mockTotalIncomeTaxService.getIncomeData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(new NoATSViewModel))
      val result = Future.successful(sut.show(request))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.ErrorController.authorisedNoAts().url
    }

    "have the right user data in the view" in {

      val result = Future.successful(sut.show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString should not include "Technical Difficulties"
      document.getElementById("starting-rate-for-savings-amount").text() shouldBe "£140"
      document.getElementById("start-rate-for-savings-before").text() shouldBe "£110"
      document.getElementById("start-rate-for-savings-rate").text() shouldBe "10%"

      document.getElementById("basic-rate-income-tax-amount").text() shouldBe "£372"
      document.getElementById("basic-rate-income-tax-before").text() shouldBe "£1,860"
      document.getElementById("basic-rate-income-tax-rate").text() shouldBe "20%"

      document.getElementById("higher-rate-income-tax-amount").text() shouldBe "£70"
      document.getElementById("higher-rate-income-tax-before").text() shouldBe "£130"
      document.getElementById("higher-rate-income-tax-rate").text() shouldBe "40%"

      document.getElementById("additional-rate-income-tax-amount").text() shouldBe "£60"
      document.getElementById("additional-rate-income-tax-before").text() shouldBe "£80"
      document.getElementById("additional-rate-income-tax-rate").text() shouldBe "45%"

      document.getElementById("ordinary-rate-amount").text() shouldBe "£50"
      document.getElementById("total-income-tax-amount").text() shouldBe "£372"

      document.toString should include("Total Income Tax")
      document.getElementById("user-info").text should include("forename surname")
      document.getElementById("user-info").text should include("Unique Taxpayer Reference: " + testUtr)
    }

    "hide rows if there is a zero value in the left cell amount field of the view" in {

      val model2 = baseModel.copy(
        startingRateForSavings = Amount(0, "GBP"),
        basicRateIncomeTax = Amount(0, "GBP")
      )

      when(mockTotalIncomeTaxService.getIncomeData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model2))


      val result = Future.successful(sut.show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString should not include "Technical Difficulties"
      document.toString should not include "starting-rate-for-savings-row"
      document.toString should not include "basic-rate-income-tax-row"
      document.toString should not include "higher-rate-income-tax-row"
      document.toString should not include "additional-rate-income-tax-row"
    }

    "hide Higher and Additional Rate fields if the amounts are 0.00" in {

      val model3 = baseModel.copy(
        higherRateIncomeTax = Amount(0, "GBP"),
        additionalRateIncomeTax = Amount(0, "GBP")
      )

      when(mockTotalIncomeTaxService.getIncomeData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model3))

      val result = Future.successful(sut.show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString should not include "Technical Difficulties"
      document.toString should include("basic-rate-income-tax-row")
      document.toString should not include "higher-rate-income-tax-row"
      document.toString should not include "additional-rate-income-tax-row"
    }

    "show 'Total Income Tax page with a correct breadcrumb" in {

      val result = Future.successful(sut.show(request))

      val document = Jsoup.parse(contentAsString(result))

      document.select("#global-breadcrumb li:nth-child(1) a").attr("href") should include("/account")
      document.select("#global-breadcrumb li:nth-child(1) a").text should include("Home")

      document.select("#global-breadcrumb li:nth-child(2) a").attr("href") should include("/annual-tax-summary")
      document.select("#global-breadcrumb li:nth-child(2) a").text shouldBe "Select the tax year"

      document.select("#global-breadcrumb li:nth-child(3) a").attr("href") should include("annual-tax-summary/main?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(3) a").text shouldBe "Your annual tax summary"

      document.select("#global-breadcrumb li:nth-child(4) a").attr("href") should include("/annual-tax-summary/summary?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(4) a").text shouldBe "Your income and taxes"

      document.select("#global-breadcrumb li:nth-child(5) a").attr("href") should include("/annual-tax-summary/nics?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(5) a").text should include("Your Income Tax and National Insurance")

      document.select("#global-breadcrumb li:nth-child(6)").toString should include("<strong>Income Tax</strong>")
    }
  }

  "Dividends section" should {
    "have the right user data for Ordinary, Additional and Higher Rates fields in the view" in {

      val result = Future.successful(sut.show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString should not include "Technical Difficulties"
      document.getElementById("ordinary-rate-amount").text() should equal("£50")
      document.getElementById("ordinary-rate-before").text() should equal("£100")
      document.getElementById("ordinary-rate-rate").text() should equal("10%")

      document.getElementById("upper-rate-amount").text() should equal("£120")
      document.getElementById("upper-rate-before").text() should equal("£30")
      document.getElementById("upper-rate-rate").text() should equal("32.5%")

      document.getElementById("additional-rate-amount").text() should equal("£40")
      document.getElementById("additional-rate-before").text() should equal("£10")
      document.getElementById("additional-rate-rate").text() should equal("37.5%")
    }

    "hide Dividends section if the amount before in each row is 0.00" in {

      val model4 = baseModel.copy(
        ordinaryRate = Amount(0, "GBP"),
        upperRate = Amount(0, "GBP"),
        additionalRate = Amount(0, "GBP")
      )

      when(mockTotalIncomeTaxService.getIncomeData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model4))

      val result = Future.successful(sut.show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString should not include "Technical Difficulties"
      document.toString should not include "dividends-section-row"
      document.toString should not include "ordinary-rate-row"
      document.toString should not include "upper-rate-row"
      document.toString should not include "additional-rate-row"
    }

    "not hide Dividends section if only Ordinary rate amount is greater than 0.00" in {

      val model5 = baseModel.copy(
        upperRate = Amount(0, "GBP"),
        additionalRate = Amount(0, "GBP")
      )

      when(mockTotalIncomeTaxService.getIncomeData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model5))

      val result = Future.successful(sut.show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString should not include "Technical Difficulties"
      document.toString should include("dividends-section-row")
      document.toString should include("ordinary-rate-row")
      document.toString should not include "upper-rate-row"
      document.toString should not include "additional-rate-row"

    }
  }

  "Adjustments section" should {

    "have the right user data for adjustments increasing and reducing income tax" in {

      val result = Future.successful(sut.show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("other-adjustments-increasing-amount").text() should equal("£90")
      document.getElementById("other-adjustments-reducing-amount").text() should equal("minus £20 -£20")
    }

    "hide other adjustments increasing your tax section if the amount is 0.00" in {

      val model6 = baseModel.copy(
        otherAdjustmentsIncreasing = Amount(0, "GBP")
      )

      when(mockTotalIncomeTaxService.getIncomeData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model6))

      val result = Future.successful(sut.show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString should not include "Technical Difficulties"
      document.toString should not include "other-adjustments-increasing-amount"

    }

    "hide other adjustments reducing your tax section if the amount is 0.00" in {

      val model7 = baseModel.copy(
        otherAdjustmentsReducing = Amount(0, "GBP")
      )

      when(mockTotalIncomeTaxService.getIncomeData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model7))

      val result = Future.successful(sut.show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString should not include "Technical Difficulties"
      document.toString should not include "other-adjustments-reducing-amount"
    }

    "hide Adjustments section if all the amounts in this section are 0.00" in {

      val model8 = baseModel.copy(
        otherAdjustmentsIncreasing = Amount(0, "GBP"),
        otherAdjustmentsReducing = Amount(0, "GBP")
      )

      when(mockTotalIncomeTaxService.getIncomeData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model8))

      val result = Future.successful(sut.show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString should not include "Technical Difficulties"
      document.toString should not include "adjustments-section"
    }
  }

  "Total Income Tax" should {

    "show zero value" in {

      val model9 = baseModel.copy(
        marriageAllowanceReceivedAmount = Amount(0, "GBP"),
        totalIncomeTax = Amount(0, "GBP")
      )

      when(mockTotalIncomeTaxService.getIncomeData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model9))

      val result = Future.successful(sut.show(request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-income-tax-amount").text() should equal("£0")
    }

  }

}
