/*
 * Copyright 2022 HM Revenue & Customs
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

import controllers.auth.{FakeAuthAction, FakeAuthJourney}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito.when
import play.api.i18n.Messages
import play.api.test.Helpers._
import services._
import utils.ControllerBaseSpec
import utils.TestConstants._
import view_models._

import scala.concurrent.Future

class TotalIncomeTaxControllerSpec extends ControllerBaseSpec {

  override val taxYear = 2014

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
    welshIncomeTax = Amount(100, "GBP"),
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

  def sut =
    new TotalIncomeTaxController(
      mockTotalIncomeTaxService,
      mockAuditService,
      FakeAuthJourney,
      mcc,
      totalIncomeTaxView,
      genericErrorView,
      tokenErrorView)

  override def beforeEach(): Unit =
    when(mockTotalIncomeTaxService.getIncomeData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request))) thenReturn Future
      .successful(baseModel)

  "Calling Total Income Tax" must {

    "return a successful response for a valid request" in {
      val result = sut.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.title must include(
        Messages("ats.total_income_tax.income_tax") + Messages(
          "generic.to_from",
          (taxYear - 1).toString,
          taxYear.toString))
    }

    "display an error page for an invalid request" in {
      val result = sut.show(badRequest)
      status(result) mustBe 400
      val document = Jsoup.parse(contentAsString(result))
      document.title must include(Messages("global.error.InternalServerError500.title"))
    }

    "display an error page when AtsUnavailableViewModel is returned" in {

      when(mockTotalIncomeTaxService.getIncomeData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(new ATSUnavailableViewModel))

      val result = sut.show(request)
      status(result) mustBe INTERNAL_SERVER_ERROR

      val document = Jsoup.parse(contentAsString(result))
      document.title must include(Messages("global.error.InternalServerError500.title"))
    }

    "redirect to the no ATS page when there is no Annual Tax Summary data returned" in {
      when(mockTotalIncomeTaxService.getIncomeData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(new NoATSViewModel))
      val result = sut.show(request)
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.ErrorController.authorisedNoAts(appConfig.taxYear).url
    }

    "have the right user data in the view" in {

      val result = sut.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString must not include "Technical Difficulties"
      document.getElementById("starting-rate-for-savings-amount").text() mustBe "£140"
      document.getElementById("start-rate-for-savings-before").text() mustBe "£110"
      document.getElementById("start-rate-for-savings-rate").text() mustBe "10%"

      document.getElementById("basic-rate-income-tax-amount").text() mustBe "£372"
      document.getElementById("basic-rate-income-tax-before").text() mustBe "£1,860"
      document.getElementById("basic-rate-income-tax-rate").text() mustBe "20%"

      document.getElementById("higher-rate-income-tax-amount").text() mustBe "£70"
      document.getElementById("higher-rate-income-tax-before").text() mustBe "£130"
      document.getElementById("higher-rate-income-tax-rate").text() mustBe "40%"

      document.getElementById("additional-rate-income-tax-amount").text() mustBe "£60"
      document.getElementById("additional-rate-income-tax-before").text() mustBe "£80"
      document.getElementById("additional-rate-income-tax-rate").text() mustBe "45%"

      document.getElementById("ordinary-rate-amount").text() mustBe "£50"
      document.getElementById("total-income-tax-amount-nics").text() mustBe "£372"

      document.toString must include("Total Income Tax")
      document.getElementById("user-info").text must include("forename surname")
      document.getElementById("user-info").text must include("Unique Taxpayer Reference: " + testUtr)
    }

    "hide rows if there is a zero value in the left cell amount field of the view" in {

      val model2 = baseModel.copy(
        startingRateForSavings = Amount(0, "GBP"),
        basicRateIncomeTax = Amount(0, "GBP")
      )

      when(mockTotalIncomeTaxService.getIncomeData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(model2))

      val result = sut.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString must not include "Technical Difficulties"
      document.toString must not include "starting-rate-for-savings-row"
      document.toString must not include "basic-rate-income-tax-row"
      document.toString must not include "higher-rate-income-tax-row"
      document.toString must not include "additional-rate-income-tax-row"
    }

    "hide Higher and Additional Rate fields if the amounts are 0.00" in {

      val model3 = baseModel.copy(
        higherRateIncomeTax = Amount(0, "GBP"),
        additionalRateIncomeTax = Amount(0, "GBP")
      )

      when(mockTotalIncomeTaxService.getIncomeData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(model3))

      val result = sut.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString must not include "Technical Difficulties"
      document.toString must include("basic-rate-income-tax-row")
      document.toString must not include "higher-rate-income-tax-row"
      document.toString must not include "additional-rate-income-tax-row"
    }

  }

  "Dividends section" must {
    "have the right user data for Ordinary, Additional and Higher Rates fields in the view" in {

      val result = sut.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString must not include "Technical Difficulties"
      document.getElementById("ordinary-rate-amount").text() must equal("£50")
      document.getElementById("ordinary-rate-before").text() must equal("£100")
      document.getElementById("ordinary-rate-rate").text() must equal("10%")

      document.getElementById("upper-rate-amount").text() must equal("£120")
      document.getElementById("upper-rate-before").text() must equal("£30")
      document.getElementById("upper-rate-rate").text() must equal("32.5%")

      document.getElementById("additional-rate-amount").text() must equal("£40")
      document.getElementById("additional-rate-before").text() must equal("£10")
      document.getElementById("additional-rate-rate").text() must equal("37.5%")
    }

    "hide Dividends section if the amount before in each row is 0.00" in {

      val model4 = baseModel.copy(
        ordinaryRate = Amount(0, "GBP"),
        upperRate = Amount(0, "GBP"),
        additionalRate = Amount(0, "GBP")
      )

      when(mockTotalIncomeTaxService.getIncomeData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(model4))

      val result = sut.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString must not include "Technical Difficulties"
      document.toString must not include "dividends-section-row"
      document.toString must not include "ordinary-rate-row"
      document.toString must not include "upper-rate-row"
      document.toString must not include "additional-rate-row"
    }

    "not hide Dividends section if only Ordinary rate amount is greater than 0.00" in {

      val model5 = baseModel.copy(
        upperRate = Amount(0, "GBP"),
        additionalRate = Amount(0, "GBP")
      )

      when(mockTotalIncomeTaxService.getIncomeData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(model5))

      val result = sut.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString must not include "Technical Difficulties"
      document.toString must include("dividends-section-row")
      document.toString must include("ordinary-rate-row")
      document.toString must not include "upper-rate-row"
      document.toString must not include "additional-rate-row"

    }
  }

  "Adjustments section" must {

    "have the right user data for adjustments increasing and reducing income tax" in {

      val result = sut.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("other-adjustments-increasing-amount").text() must equal("£90")
      document.getElementById("other-adjustments-reducing-amount").text() must equal("minus £20 -£20")
    }

    "hide other adjustments increasing your tax section if the amount is 0.00" in {

      val model6 = baseModel.copy(
        otherAdjustmentsIncreasing = Amount(0, "GBP")
      )

      when(mockTotalIncomeTaxService.getIncomeData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(model6))

      val result = sut.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString must not include "Technical Difficulties"
      document.toString must not include "other-adjustments-increasing-amount"

    }

    "hide other adjustments reducing your tax section if the amount is 0.00" in {

      val model7 = baseModel.copy(
        otherAdjustmentsReducing = Amount(0, "GBP")
      )

      when(mockTotalIncomeTaxService.getIncomeData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(model7))

      val result = sut.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString must not include "Technical Difficulties"
      document.toString must not include "other-adjustments-reducing-amount"
    }

    "hide Adjustments section if all the amounts in this section are 0.00" in {

      val model8 = baseModel.copy(
        otherAdjustmentsIncreasing = Amount(0, "GBP"),
        otherAdjustmentsReducing = Amount(0, "GBP")
      )

      when(mockTotalIncomeTaxService.getIncomeData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(model8))

      val result = sut.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString must not include "Technical Difficulties"
      document.toString must not include "adjustments-section"
    }
  }

  "Total Income Tax" must {

    "show zero value" in {

      val model9 = baseModel.copy(
        marriageAllowanceReceivedAmount = Amount(0, "GBP"),
        totalIncomeTax = Amount(0, "GBP")
      )

      when(mockTotalIncomeTaxService.getIncomeData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(model9))

      val result = sut.show(request)
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-income-tax-amount-nics").text() must equal("£0")
    }

  }

}
