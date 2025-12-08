/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.sa

import controllers.auth.FakeAuthJourney
import models.requests.AuthenticatedRequest
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq as meq}
import org.mockito.Mockito.{reset, when}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, SEE_OTHER}
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.{AuditService, IncomeService}
import utils.TestConstants.*
import utils.{ControllerBaseSpec, TaxYearUtil}
import view_models.{ATSUnavailableViewModel, Amount, IncomeBeforeTax, NoATSViewModel}

import scala.concurrent.Future

class IncomeControllerSpec extends ControllerBaseSpec {
  private val taxYearUtil = app.injector.instanceOf[TaxYearUtil]

  val baseModel: IncomeBeforeTax = IncomeBeforeTax(
    taxYear = currentTaxYearSA,
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

  val mockIncomeService: IncomeService = mock[IncomeService]
  val mockAuditService: AuditService   = mock[AuditService]

  def sut                                                           =
    new IncomeController(
      mockIncomeService,
      mockAuditService,
      FakeAuthJourney,
      mcc,
      incomeBeforeTaxView,
      genericErrorView,
      tokenErrorView,
      taxYearUtil
    )
  private val request: AuthenticatedRequest[AnyContentAsEmpty.type] = buildRequest(currentTaxYearSA)

  override def beforeEach(): Unit = {
    reset(mockFeatureFlagService)

    when(mockIncomeService.getIncomeData(meq(currentTaxYearSA))(any(), meq(request)))
      .thenReturn(Future.successful(baseModel))
    ()
  }

  "Calling incomes" must {

    "return a successful response for a valid request" in {
      val result   = sut.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.title must include(
        Messages("ats.income_before_tax.title") + Messages(
          "generic.to_from",
          (currentTaxYearSA - 1).toString,
          currentTaxYearSA.toString
        )
      )
    }

    "display an error page for an invalid request" in {
      val result = sut.show(badRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.ErrorController.authorisedNoTaxYear.url)
    }

    "display an error page when AtsUnavailableViewModel is returned" in {

      when(mockIncomeService.getIncomeData(meq(currentTaxYearSA))(any(), meq(request)))
        .thenReturn(Future.successful(new ATSUnavailableViewModel))

      val result = sut.show(request)
      status(result) mustBe INTERNAL_SERVER_ERROR

      val document = Jsoup.parse(contentAsString(result))
      document.title must include(Messages("global.error.InternalServerError500.title"))
    }

    "redirect to the no ATS page when there is no Annual Tax Summary data returned" in {

      when(mockIncomeService.getIncomeData(meq(currentTaxYearSA))(any(), meq(request)))
        .thenReturn(Future.successful(NoATSViewModel(currentTaxYearSA)))

      val result = sut.show(request)
      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe controllers.routes.ErrorController.authorisedNoAts(currentTaxYearSA).url
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

      document.toString                           must include("Your total income")
      document.getElementById("user-info").text() must include("forename surname")
      document.getElementById("user-info").text() must include("Unique Taxpayer Reference: " + testUtr)
      document
        .getElementsByAttributeValueMatching("data-component", "ats_page_heading__h1")
        .text mustBe "Your total income"
      document
        .getElementsByAttributeValueMatching("data-component", "ats_page_heading__p")
        .text mustBe s"Tax year: April 6 ${currentTaxYearSA - 1} to April 5 $currentTaxYearSA"
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

      when(mockIncomeService.getIncomeData(meq(currentTaxYearSA))(any(), meq(request)))
        .thenReturn(Future.successful(model))

      val result = sut.show(request)

      status(result) mustBe 200

      val document = Jsoup.parse(contentAsString(result))

      document.toString                         must not include "self-employment-income"
      document.toString                         must not include "benefits-from-employment"
      document.toString                         must not include "other_pension_income"
      document.toString                         must not include "state_pension"
      document.toString                         must not include "taxable_state_benefits"
      document.toString                         must not include "income_from_employment"
      document.toString                         must not include "other_income"
      document.toString                         must not include "total_income_before_tax"
      document.getElementById("user-info").text must include("forename surname")
      document.getElementById("user-info").text must include("Unique Taxpayer Reference: " + testUtr)
    }

  }
}
