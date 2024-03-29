/*
 * Copyright 2023 HM Revenue & Customs
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

import controllers.auth.FakeAuthJourney
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.{AuditService, SummaryService}
import utils.ControllerBaseSpec
import utils.TestConstants._
import view_models._

import scala.concurrent.Future

class NicsSummaryControllerSpec extends ControllerBaseSpec {

  val dataPath = "/summary_json_test_2021.json"

  val model: Summary = Summary(
    year = taxYear,
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
    new NicsController(
      mockSummaryService,
      mockAuditService,
      FakeAuthJourney,
      mcc,
      nicsView,
      genericErrorView,
      tokenErrorView
    )

  override def beforeEach(): Unit = {
    reset(mockFeatureFlagService)

    when(
      mockSummaryService.getSummaryData(any())(any(), any())
    ) thenReturn Future
      .successful(model)
  }

  "Calling NICs" must {

    "return a successful response for a valid request" in {
      val result   = sut.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.title must include(
        Messages("ats.nics.tax_and_nics.title") + Messages("generic.to_from", (taxYear - 1).toString, taxYear.toString)
      )
    }

    "display an error page for an invalid request" in {
      val result   = sut.show(badRequest)
      status(result) mustBe 400
      val document = Jsoup.parse(contentAsString(result))
      document.title must include(Messages("global.error.InternalServerError500.title"))
    }

    "display an error page when AtsUnavailableViewModel is returned" in {
      when(mockSummaryService.getSummaryData(any())(any(), any()))
        .thenReturn(Future.successful(new ATSUnavailableViewModel))

      val result = sut.show(request)
      status(result) mustBe INTERNAL_SERVER_ERROR

      val document = Jsoup.parse(contentAsString(result))
      document.title must include(Messages("global.error.InternalServerError500.title"))
    }

    "redirect to the no ATS page when there is no Annual Tax Summary data returned" in {
      when(mockSummaryService.getSummaryData(any())(any(), any()))
        .thenReturn(Future.successful(NoATSViewModel(taxYear)))

      val result = sut.show(request)
      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe routes.ErrorController.authorisedNoAts(appConfig.taxYear).url

    }

    "have the right user data in the view" in {

      val result   = sut.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-income-tax-amt").text() mustBe "£372"
      document.getElementById("total-cg-tax-rate").text() mustBe "56.78%"
      document.getElementById("employee-nic-amount").text() mustBe "£1,200"
      document.getElementById("total-income-tax-and-nics").text() mustBe "£1,572"
      document.getElementById("user-info").text must include("forename surname")
      document.getElementById("user-info").text must include("Unique Taxpayer Reference: " + testUtr)
    }
  }
}
