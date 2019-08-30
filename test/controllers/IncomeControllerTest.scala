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
import models.InvalidTaxYear
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest.MustMatchers._
import org.scalatest.mock.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation}
import services.{AuditService, IncomeService}
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import utils.AuthorityUtils
import utils.TestConstants._
import view_models.{Amount, IncomeBeforeTax, NoATSViewModel}

import scala.concurrent.Future

class IncomeControllerTest extends UnitSpec with FakeTaxsPlayApplication with MockitoSugar {

  val taxYear = 2015
  val request = FakeRequest("GET", s"?taxYear=$taxYear")
  val badRequest = FakeRequest("GET","?taxYear=20155")
  val user = User(AuthorityUtils.saAuthority(testOid, testUtr))
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

  trait TestController extends IncomeController {

    override lazy val incomeService: IncomeService = mock[IncomeService]
    override lazy val auditService: AuditService = mock[AuditService]
    implicit lazy val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever

    when(incomeService.getIncomeData(Matchers.eq(taxYear))(Matchers.eq(user), Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(baseModel))

  }

  "Calling incomes with no session" should {

    "return a 303 response" in new TestController {

      val result = Future.successful(authorisedIncomeBeforeTax(request))
      status(result) shouldBe 303
    }
  }

  "Calling incomes with session" should {

    "return a 200 response if request contains an valid tax year value " in new TestController {
      val result =  Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.toString should include("<title>Your total income: 2013 to 2014 - Annual tax summary - GOV.UK</title>")
    }

    "return a 400 BadRequest statue with response if request contains an invalid tax year value " in new TestController {
      val result = Future.successful(show(user, badRequest))
      status(result) shouldBe 400
      val document = Jsoup.parse(contentAsString(result))
      document.toString should include("<body>\n  Request does not contain valid tax year\n </body>")
    }

    "redirect to the no ATS page when there is no annual tax summary data returned" in new TestController {

      when(incomeService.getIncomeData(Matchers.eq(taxYear))(Matchers.eq(user), Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(new NoATSViewModel))

      val result = Future.successful(show(user, request))
      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe routes.ErrorController.authorisedNoAts().url

    }

    "have the right user data in the view" in new TestController {

      val result = Future.successful(show(user, request))

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
      document.getElementById("user-info").text() should include("Unique Taxpayer Reference: "+testUtr)
      document.select(".page-header h1").text shouldBe "Tax year: April 6 2013 to April 5 2014 Your total income"
    }

    "have zero-value fields hidden in the view" in new TestController {

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

      when(incomeService.getIncomeData(Matchers.eq(taxYear))(Matchers.eq(user), Matchers.any(), Matchers.eq(request))).thenReturn(model)


      val result = Future.successful(show(user, request))

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
      document.getElementById("user-info").text should include("Unique Taxpayer Reference: "+testUtr)
    }

    "show 'Income Before Tax' page with a correct breadcrumb" in new TestController {

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.select("#global-breadcrumb li:nth-child(1) a").attr("href") should include("/account")
      document.select("#global-breadcrumb li:nth-child(1) a").text should include("Home")

      document.select("#global-breadcrumb li:nth-child(2) a").attr("href") should include("/annual-tax-summary")
      document.select("#global-breadcrumb li:nth-child(2) a").text shouldBe "Select the tax year"

      document.select("#global-breadcrumb li:nth-child(3) a").attr("href") should include("annual-tax-summary/main?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(3) a").text shouldBe "Your annual tax summary"

      document.select("#global-breadcrumb li:nth-child(4) a").attr("href") should include("/annual-tax-summary/summary?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(4) a").text shouldBe "Your income and taxes"

      document.select("#global-breadcrumb li:nth-child(5)").toString should include("<strong>Your total income</strong>")
    }
  }
}
