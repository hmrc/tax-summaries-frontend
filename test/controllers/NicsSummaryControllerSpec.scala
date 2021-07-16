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

import controllers.auth.FakeAuthAction
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito.when
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.{AuditService, SummaryService}
import utils.ControllerBaseSpec
import utils.TestConstants._
import view_models._

import scala.concurrent.Future

class NicsSummaryControllerSpec extends ControllerBaseSpec {

  override val taxYear = 2014

  val dataPath = "/summary_json_test.json"

  val model = Summary(
    year = 2014,
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

  val mockSummaryService = mock[SummaryService]
  val mockAuditService = mock[AuditService]

  def sut =
    new NicsController(
      mockSummaryService,
      mockAuditService,
      FakeAuthAction,
      mcc,
      nicsView,
      genericErrorView,
      tokenErrorView)

  override def beforeEach(): Unit =
    when(mockSummaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request))) thenReturn Future
      .successful(model)

  "Calling NICs" should {

    "return a successful response for a valid request" in {
      val result = sut.show(request)
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.title should include(
        Messages("ats.nics.tax_and_nics.title") + Messages("generic.to_from", (taxYear - 1).toString, taxYear.toString))
    }

    "display an error page for an invalid request" in {
      val result = sut.show(badRequest)
      status(result) shouldBe 400
      val document = Jsoup.parse(contentAsString(result))
      document.title should include(Messages("global.error.InternalServerError500.title"))
    }

    "display an error page when AtsUnavailableViewModel is returned" in {

      when(mockSummaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(new ATSUnavailableViewModel))

      val result = sut.show(request)
      status(result) shouldBe INTERNAL_SERVER_ERROR

      val document = Jsoup.parse(contentAsString(result))
      document.title should include(Messages("global.error.InternalServerError500.title"))
    }

    "redirect to the no ATS page when there is no Annual Tax Summary data returned" in {

      when(mockSummaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(new NoATSViewModel))

      val result = sut.show(request)
      status(result) shouldBe SEE_OTHER

      redirectLocation(result).get shouldBe routes.ErrorController.authorisedNoAts().url

    }

    "have the right user data in the view" in {

      val result = sut.show(request)
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-income-tax-amt").text() shouldBe "£372"
      document.getElementById("total-cg-tax-rate").text() shouldBe "56.78%"
      document.getElementById("employee-nic-amount").text() shouldBe "£1,200"
      document.getElementById("total-income-tax-and-nics").text() shouldBe "£1,572"
      document.getElementById("user-info").text should include("forename surname")
      document.getElementById("user-info").text should include("Unique Taxpayer Reference: " + testUtr)
    }

  }
}
