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

package sa.controllers

import common.controllers.auth.FakeAuthJourney
import common.models.requests.AuthenticatedRequest
import common.services.AuditService
import common.utils.TestConstants.{capitalGains, testUtr}
import common.utils.{ControllerBaseSpec, TaxYearUtil}
import common.view_models.{ATSUnavailableViewModel, Amount, CapitalGains, NoATSViewModel}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq as meq}
import org.mockito.Mockito.{reset, when}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, SEE_OTHER}
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import sa.services.CapitalGainsService

import scala.concurrent.Future

class CapitalGainsTaxControllerSpec extends ControllerBaseSpec {
  private val taxYearUtil     = app.injector.instanceOf[TaxYearUtil]
  val baseModel: CapitalGains = capitalGains

  val mockCapitalGainsService: CapitalGainsService = mock[CapitalGainsService]
  val mockAuditService: AuditService               = mock[AuditService]

  def sut: CapitalGainsTaxController                                =
    new CapitalGainsTaxController(
      mockCapitalGainsService,
      mockAuditService,
      FakeAuthJourney,
      mcc,
      capitalGainsView,
      genericErrorView,
      tokenErrorView,
      taxYearUtil
    )
  private val request: AuthenticatedRequest[AnyContentAsEmpty.type] = buildRequest(currentTaxYearSA)

  override def beforeEach(): Unit = {
    reset(mockFeatureFlagService)

    when(mockCapitalGainsService.getCapitalGains(meq(currentTaxYearSA))(any(), meq(request)))
      .thenReturn(Future.successful(baseModel))
    ()
  }

  "Calling Capital Gains" must {

    "return a successful response for a valid request" in {
      val result   = sut.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.title must include(
        Messages("ats.capital_gains_tax.html.title") + Messages(
          "generic.to_from",
          (currentTaxYearSA - 1).toString,
          currentTaxYearSA.toString
        )
      )
    }

    "display an error page for an invalid request " in {
      val result = sut.show(badRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(common.controllers.routes.ErrorController.authorisedNoTaxYear.url)
    }

    "display an error page when AtsUnavailableViewModel is returned" in {

      when(mockCapitalGainsService.getCapitalGains(meq(currentTaxYearSA))(any(), meq(request)))
        .thenReturn(Future.successful(new ATSUnavailableViewModel))

      val result = sut.show(request)
      status(result) mustBe INTERNAL_SERVER_ERROR

      val document = Jsoup.parse(contentAsString(result))
      document.title must include(Messages("global.error.InternalServerError500.title"))
    }

    "redirect to the no ATS page when there is no Annual Tax Summary data returned" in {
      when(mockCapitalGainsService.getCapitalGains(meq(currentTaxYearSA))(any(), meq(request)))
        .thenReturn(Future.successful(NoATSViewModel(currentTaxYearSA)))
      val result = sut.show(request)
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe common.controllers.routes.ErrorController
        .authorisedNoAts(currentTaxYearSA)
        .url
    }

    "show Your Capital Gains section with the right user data" in {

      val result   = sut.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("taxable-gains").text() mustBe "£20,000"
      document.getElementById("less-taxable-gains").text() mustBe "− £10,600"
      document.getElementById("cg-pay-tax-on").text() mustBe "£9,400"
      document.getElementById("tax-period").text() mustBe s"${currentTaxYearSA - 1} to $currentTaxYearSA"
      document.getElementById("total-cg-tax-rate").text() mustBe "12.34%"
      document.getElementById("user-info").text() must include("forename surname")
      document.getElementById("user-info").text() must include("Unique Taxpayer Reference: " + testUtr)
      document
        .getElementsByAttributeValueMatching("data-component", "ats_page_heading__h1")
        .text mustBe "Capital Gains Tax"
      document
        .getElementsByAttributeValueMatching("data-component", "ats_page_heading__p")
        .text mustBe s"Tax year: April 6 ${currentTaxYearSA - 1} to April 5 $currentTaxYearSA"
    }

    "show Capital Gains Tax section if total amount of capital gains to pay tax on is not 0.00" in {

      val result   = sut.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("cg-pay-tax-on").text() mustBe "£9,400"
      document.getElementById("capital-gains-tax-section") must not be null
    }

    "hide Capital Gains Tax section if total amount of capital gains to pay tax on is 0.00" in {

      val model2 = baseModel.copy(
        payCgTaxOn = Amount(0, "GBP"),
        taxableGains = Amount(0, "GBP")
      )

      when(mockCapitalGainsService.getCapitalGains(meq(currentTaxYearSA))(any(), meq(request)))
        .thenReturn(Future.successful(model2))

      val result   = sut.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("cg-pay-tax-on").text() mustBe "£0"
      document.getElementById("capital-gains-tax-section") must be(null)
    }

    "show Capital Gains Tax section with correct user data" in {

      val result   = sut.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("entrepreneurs-relief-rate-before").text() must equal("£1,111")
      document.getElementById("entrepreneurs-relief-rate").text()        must equal("10%")
      document.getElementById("entrepreneurs-relief-rate-amount").text() must equal("£1,000")

      document.getElementById("ordinary-rate-before").text() must equal("£2,222")
      document.getElementById("ordinary-rate").text()        must equal("18%")
      document.getElementById("ordinary-rate-amount").text() must equal("£2,000")

      document.getElementById("upper-rate-before").text() must equal("£3,333")
      document.getElementById("upper-rate").text()        must equal("28%")
      document.getElementById("upper-rate-amount").text() must equal("£3,000")
    }

    "hide Entrepreneurs' Relief Rate field if the amount on the left side is 0.00" in {

      val model3 = baseModel.copy(
        entrepreneursReliefRateBefore = Amount(0, "GBP")
      )

      when(mockCapitalGainsService.getCapitalGains(meq(currentTaxYearSA))(any(), meq(request)))
        .thenReturn(Future.successful(model3))

      val result   = sut.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString                                            must not include "Technical Difficulties"
      document.getElementById("entrepreneurs-relief-rate-section") must be(null)
    }

    "hide Ordinary Rate field if the amount on the left side is 0.00" in {

      val model4 = baseModel.copy(
        ordinaryRateBefore = Amount(0, "GBP")
      )

      when(mockCapitalGainsService.getCapitalGains(meq(currentTaxYearSA))(any(), meq(request)))
        .thenReturn(Future.successful(model4))

      val result   = sut.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString                                must not include "Technical Difficulties"
      document.getElementById("ordinary-rate-section") must be(null)
    }

    "hide Upper Rate field if the amount on the left side is 0.00" in {

      val model5 = baseModel.copy(
        upperRateBefore = Amount(0, "GBP")
      )

      when(mockCapitalGainsService.getCapitalGains(meq(currentTaxYearSA))(any(), meq(request)))
        .thenReturn(Future.successful(model5))

      val result   = sut.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString                             must not include "Technical Difficulties"
      document.getElementById("upper-rate-section") must be(null)
    }

    "show Adjustments section with correct user data" in {

      val result   = sut.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("adjustment-to-capital-gains-tax-amount").text() must equal("− £500")
    }

    "show Total Capital Gains Tax with correct user data" in {

      val result   = sut.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-capital-gains-tax-amount").text() must equal("£5,500")
    }

    "hide Adjustments section if the Adjustments amount is 0.00" in {

      val model6 = baseModel.copy(
        adjustmentsAmount = Amount(0, "GBP")
      )

      when(mockCapitalGainsService.getCapitalGains(meq(currentTaxYearSA))(any(), meq(request)))
        .thenReturn(Future.successful(model6))

      val result   = sut.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString                              must not include "Technical Difficulties"
      document.getElementById("adjustments-section") must be(null)
    }

    "show capital gains description if total capital gains tax is not 0" in {

      val result   = sut.show(request)
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-cg-description")     must not be null
      document.getElementById("total-cg-tax-rate").text() must equal("12.34%")
    }

    "hide capital gains description if total capital gains tax is 0" in {

      val model7 = baseModel.copy(
        totalCapitalGainsTaxAmount = Amount(0, "GBP")
      )

      when(mockCapitalGainsService.getCapitalGains(meq(currentTaxYearSA))(any(), meq(request)))
        .thenReturn(Future.successful(model7))

      val result   = sut.show(request)
      val document = Jsoup.parse(contentAsString(result))

      document.toString                               must not include "Technical Difficulties"
      document.getElementById("total-cg-description") must be(null)
    }

  }
}
