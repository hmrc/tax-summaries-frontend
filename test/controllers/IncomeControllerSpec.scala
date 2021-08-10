/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.http.Status.{INTERNAL_SERVER_ERROR, SEE_OTHER}
import play.api.i18n.Messages
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.{AuditService, IncomeService}
import utils.ControllerBaseSpec
import utils.TestConstants._
import view_models.{ATSUnavailableViewModel, Amount, IncomeBeforeTax, NoATSViewModel}

import scala.concurrent.Future

class IncomeControllerSpec extends ControllerBaseSpec {

  override val taxYear = 2014

  val baseModel = IncomeBeforeTax(
    taxYear = 2014,
    utr = testUtr,
    getSelfEmployTotal = Amount(1100, "GBP"),
    getIncomeFromEmployment = Amount(10500, "GBP"),
    getStatePension = Amount(1000, "GBP"),
    getOtherPensionTotal = Amount(2000, "GBP"),
    getTaxableStateBenefit = Amount(3000, "GBP"),
    getOtherIncome = Amount(1500, "GBP"),
    getBenefitsFromEmployment = Amount(20, "GBP"),
    getIncomeBeforeTaxTotal = Amount(11600, "GBP"),
    title = "Mr",
    forename = "forename",
    surname = "surname"
  )

  val mockIncomeService = mock[IncomeService]
  val mockAuditService = mock[AuditService]

  def sut =
    new IncomeController(
      mockIncomeService,
      mockAuditService,
      FakeAuthJourney,
      mcc,
      incomeBeforeTaxView,
      genericErrorView,
      tokenErrorView)

  override def beforeEach(): Unit =
    when(mockIncomeService.getIncomeData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
      .thenReturn(Future.successful(baseModel))

  "Calling incomes" must {

    "return a successful response for a valid request" in {
      val result = sut.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.title must include(
        Messages("ats.income_before_tax.title") + Messages("generic.to_from", (taxYear - 1).toString, taxYear.toString))
    }

    "display an error page for an invalid request" in {
      val result = sut.show(badRequest)
      status(result) mustBe 400
      val document = Jsoup.parse(contentAsString(result))
      document.title must include(Messages("global.error.InternalServerError500.title"))
    }

    "display an error page when AtsUnavailableViewModel is returned" in {

      when(mockIncomeService.getIncomeData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(new ATSUnavailableViewModel))

      val result = sut.show(request)
      status(result) mustBe INTERNAL_SERVER_ERROR

      val document = Jsoup.parse(contentAsString(result))
      document.title must include(Messages("global.error.InternalServerError500.title"))
    }

    "redirect to the no ATS page when there is no Annual Tax Summary data returned" in {

      when(mockIncomeService.getIncomeData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(new NoATSViewModel))

      val result = sut.show(request)
      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe routes.ErrorController.authorisedNoAts.url
    }

    "have the right user data in the view" in {

      val result = sut.show(request)

      status(result) mustBe 200

      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("self-employment-income").text() mustBe "£1,100"
      document.getElementById("income-from-employment").text() mustBe "£10,500"
      document.getElementById("income-before-tax").text() mustBe "£11,600"

      document.getElementById("state-pension-amount").text() mustBe "£1,000"
      document.getElementById("other-pension-total").text() mustBe "£2,000"
      document.getElementById("taxable-state-benefits").text() mustBe "£3,000"
      document.getElementById("other-income-amount").text() mustBe "£1,500"

      document.toString must include("Your total income")
      document.getElementById("user-info").text() must include("forename surname")
      document.getElementById("user-info").text() must include("Unique Taxpayer Reference: " + testUtr)
      document.select("h1").text mustBe "Tax year: April 6 2013 to April 5 2014 Your total income"
    }

    "have zero-value fields hidden in the view" in {

      val model = baseModel.copy(
        getSelfEmployTotal = Amount(0, "GBP"),
        getIncomeFromEmployment = Amount(0, "GBP"),
        getStatePension = Amount(0, "GBP"),
        getOtherPensionTotal = Amount(0, "GBP"),
        getTaxableStateBenefit = Amount(0, "GBP"),
        getOtherIncome = Amount(0, "GBP"),
        getBenefitsFromEmployment = Amount(0, "GBP"),
        getIncomeBeforeTaxTotal = Amount(0, "GBP")
      )

      when(mockIncomeService.getIncomeData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(model))

      val result = sut.show(request)

      status(result) mustBe 200

      val document = Jsoup.parse(contentAsString(result))

      document.toString must not include "self-employment-income"
      document.toString must not include "benefits-from-employment"
      document.toString must not include "other_pension_income"
      document.toString must not include "state_pension"
      document.toString must not include "taxable_state_benefits"
      document.toString must not include "income_from_employment"
      document.toString must not include "other_income"
      document.toString must not include "total_income_before_tax"
      document.getElementById("user-info").text must include("forename surname")
      document.getElementById("user-info").text must include("Unique Taxpayer Reference: " + testUtr)
    }

  }
}
