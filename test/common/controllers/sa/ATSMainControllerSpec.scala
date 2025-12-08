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

package common.controllers.sa

import common.controllers.auth.FakeAuthJourney
import common.controllers.sa.SummaryControllerSpec.currentTaxYearSA
import common.models.requests.AuthenticatedRequest
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq as meq}
import org.mockito.Mockito.{reset, when}
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers.*
import common.services.*
import common.utils.TestConstants.*
import common.utils.{ControllerBaseSpec, TaxYearUtil}
import common.view_models.*

import scala.concurrent.Future

class ATSMainControllerSpec extends ControllerBaseSpec {
  private val taxYearUtil = app.injector.instanceOf[TaxYearUtil]
  val baseModel: Summary  = Summary(
    year = currentTaxYearSA,
    utr = testUtr,
    employeeNicAmount = Amount(1200, "GBP"),
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
    totalCgTaxRate = Rate("12.34%"),
    nicsAndTaxRate = Rate("56.78%"),
    title = "Mr",
    forename = "forename",
    surname = "surname"
  )

  val mockSummaryService: SummaryService = mock[SummaryService]
  val mockAuditService: AuditService     = mock[AuditService]

  def sut =
    new AtsMainController(
      mockSummaryService,
      mockAuditService,
      FakeAuthJourney,
      mcc,
      taxsMainView,
      genericErrorView,
      tokenErrorView,
      taxYearUtil
    )

  override def beforeEach(): Unit                                   = {
    reset(mockFeatureFlagService)
    when(mockSummaryService.getSummaryData(meq(currentTaxYearSA))(any(), meq(request)))
      .thenReturn(Future.successful(baseModel))
    ()
  }
  private val request: AuthenticatedRequest[AnyContentAsEmpty.type] = buildRequest(currentTaxYearSA)

  "Calling Index Page" must {

    "return a successful response for a valid request" in {

      val result   = sut.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.title          must include(
        Messages("ats.index.html.title")
      )
      contentAsString(result) must include("contact/beta-feedback")
    }

    "display an error page for an invalid request" in {
      val result = sut.show(badRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(common.controllers.routes.ErrorController.authorisedNoTaxYear.url)
    }

    "display an error page when AtsUnavailableViewModel is returned" in {

      when(mockSummaryService.getSummaryData(meq(currentTaxYearSA))(any(), meq(request)))
        .thenReturn(Future.successful(new ATSUnavailableViewModel))

      val result = sut.show(request)
      status(result) mustBe INTERNAL_SERVER_ERROR

      val document = Jsoup.parse(contentAsString(result))
      document.title must include(Messages("global.error.InternalServerError500.title"))
    }

    "redirect to the no ATS page when there is no Annual Tax Summary data returned" in {

      when(mockSummaryService.getSummaryData(meq(currentTaxYearSA))(any(), meq(request)))
        .thenReturn(Future.successful(NoATSViewModel(currentTaxYearSA)))

      val result = sut.show(request)
      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe common.controllers.routes.ErrorController.authorisedNoAts(currentTaxYearSA).url

    }

    "have the right user data in the view" in {

      val result   = sut.show(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe 200
      document.getElementById("tax-calc-link").text mustBe "Your income and taxes"
      document.getElementById("tax-services-link").text mustBe "Your taxes and public spending"
      document
        .select("header[data-component='ats_page_heading']")
        .text mustBe s"Tax year: April 6 ${currentTaxYearSA - 1} to April 5 $currentTaxYearSA Self Assessment Annual Tax Summary"
      document
        .getElementById("index-page-description")
        .text mustBe "This summarises your personal tax and National Insurance, and how they are spent by government."
      document.getElementById("tax-calc-link").tagName mustBe "a"
      document.getElementById("tax-services-link").tagName mustBe "a"
      document.getElementById("user-info").text must include("forename surname")
      document.getElementById("user-info").text must include("Unique Taxpayer Reference: " + testUtr)
    }

    "display the right years" in {

      val model = baseModel.copy(
        year = currentTaxYearSA
      )

      when(mockSummaryService.getSummaryData(meq(currentTaxYearSA))(any(), meq(request)))
        .thenReturn(Future.successful(model))

      val result   = sut.show(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe 200
      document.select("header[data-component='ats_page_heading']").text must include(currentTaxYearSA.toString)
    }

  }
}
