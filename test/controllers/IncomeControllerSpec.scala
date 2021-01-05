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
import services.{AuditService, IncomeService}
import uk.gov.hmrc.domain.SaUtr
import utils.TestConstants._
import view_models.{Amount, IncomeBeforeTax, NoATSViewModel}
import scala.concurrent.Future

class IncomeControllerSpec extends ControllerBaseSpec with BeforeAndAfterEach {

  override val taxYear = 2014
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
      FakeAuthAction,
      mcc,
      incomeBeforeTaxView,
      genericErrorView,
      tokenErrorView)

  override def beforeEach(): Unit =
    when(mockIncomeService.getIncomeData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
      .thenReturn(Future.successful(baseModel))

  "Calling incomes" should {

    "return a successful response for a valid request" in {
      val result = Future.successful(sut.show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.title should include(
        Messages("ats.income_before_tax.title") + Messages("generic.to_from", (taxYear - 1).toString, taxYear.toString))
    }

    "display an error page for an invalid request" in {
      val result = Future.successful(sut.show(badRequest))
      status(result) shouldBe 400
      val document = Jsoup.parse(contentAsString(result))
      document.title should include(Messages("generic.error.html.title"))
    }

    "redirect to the no ATS page when there is no Annual Tax Summary data returned" in {

      when(mockIncomeService.getIncomeData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(new NoATSViewModel))

      val result = Future.successful(sut.show(request))
      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe routes.ErrorController.authorisedNoAts().url
    }

    "have the right user data in the view" in {

      val result = Future.successful(sut.show(request))

      status(result) shouldBe 200

      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("self-employment-income").text() shouldBe "£1,100"
      document.getElementById("income-from-employment").text() shouldBe "£10,500"
      document.getElementById("income-before-tax").text() shouldBe "£11,600"

      document.getElementById("state-pension-amount").text() shouldBe "£1,000"
      document.getElementById("other-pension-total").text() shouldBe "£2,000"
      document.getElementById("taxable-state-benefits").text() shouldBe "£3,000"
      document.getElementById("other-income-amount").text() shouldBe "£1,500"

      document.toString should include("Your total income")
      document.getElementById("user-info").text() should include("forename surname")
      document.getElementById("user-info").text() should include("Unique Taxpayer Reference: " + testUtr)
      document.select("h1").text shouldBe "Tax year: April 6 2013 to April 5 2014 Your total income"
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
        .thenReturn(model)

      val result = Future.successful(sut.show(request))

      status(result) shouldBe 200

      val document = Jsoup.parse(contentAsString(result))

      document.toString should not include "self-employment-income"
      document.toString should not include "benefits-from-employment"
      document.toString should not include "other_pension_income"
      document.toString should not include "state_pension"
      document.toString should not include "taxable_state_benefits"
      document.toString should not include "income_from_employment"
      document.toString should not include "other_income"
      document.toString should not include "total_income_before_tax"
      document.getElementById("user-info").text should include("forename surname")
      document.getElementById("user-info").text should include("Unique Taxpayer Reference: " + testUtr)
    }

    "show 'Income Before Tax' page with a correct breadcrumb" in {

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
        "<strong>Your total income</strong>")
    }
  }
}
