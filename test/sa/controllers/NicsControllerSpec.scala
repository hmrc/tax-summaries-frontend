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
import common.services.AuditService
import common.utils.TestConstants.*
import common.utils.{ControllerBaseSpec, TaxYearUtil}
import common.view_models.*
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq as meq}
import org.mockito.Mockito.{reset, when}
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import sa.services.IncomeTaxAndNIService

import scala.concurrent.Future

class NicsControllerSpec extends ControllerBaseSpec {

  val mockAuditService: AuditService    = mock[AuditService]
  private val mockTotalIncomeTaxService = mock[IncomeTaxAndNIService]
  private val taxYearUtil               = app.injector.instanceOf[TaxYearUtil]
  private val request                   = buildRequest(currentTaxYearSA)

  private def nicsController =
    new NicsController(
      mockAuditService,
      FakeAuthJourney,
      mcc,
      nicsView,
      genericErrorView,
      tokenErrorView,
      mockTotalIncomeTaxService,
      taxYearUtil
    )

  override def beforeEach(): Unit = {
    reset(mockFeatureFlagService, mockTotalIncomeTaxService)
    when(mockTotalIncomeTaxService.getIncomeAndNIData(any())(any(), any()))
      .thenReturn(Future.successful(totalIncomeTaxModel))
    ()
  }

  "Calling deprecated total income tax endpoint" must {
    "redirect to the nics page when there is a tax year in request" in {
      val result = nicsController.redirectForDeprecatedTotalIncomeTaxPage(request)
      status(result) mustBe SEE_OTHER

      redirectLocation(
        result
      ).get mustBe sa.controllers.routes.NicsController.authorisedTaxAndNICs.url + s"?taxYear=$currentTaxYearSA"
    }

    "redirect to the year selection page when there is a no tax year in request" in {
      val result =
        nicsController.redirectForDeprecatedTotalIncomeTaxPage(
          request copy (request = FakeRequest("GET", "/"))
        )
      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe common.controllers.routes.AtsMergePageController.onPageLoad.url
    }
  }

  "Calling NICs" must {

    "return a successful response for a valid request" in {
      val result   = nicsController.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.title must include(
        Messages("ats.nics.tax_and_nics.title") + Messages(
          "generic.to_from",
          (currentTaxYearSA - 1).toString,
          currentTaxYearSA.toString
        )
      )
    }

    "display an error page for an invalid request" in {
      val result = nicsController.show(badRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(common.controllers.routes.ErrorController.authorisedNoTaxYear.url)
    }

    "display an error page when AtsUnavailableViewModel is returned" in {
      when(mockTotalIncomeTaxService.getIncomeAndNIData(any())(any(), any()))
        .thenReturn(Future.successful(new ATSUnavailableViewModel))

      val result = nicsController.show(request)
      status(result) mustBe INTERNAL_SERVER_ERROR

      val document = Jsoup.parse(contentAsString(result))
      document.title must include(Messages("global.error.InternalServerError500.title"))
    }

    "redirect to the main nics page when deprecated endpoint called" in {
      when(mockTotalIncomeTaxService.getIncomeAndNIData(any())(any(), any()))
        .thenReturn(Future.successful(NoATSViewModel(currentTaxYearSA)))

      val result = nicsController.show(request)
      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe common.controllers.routes.ErrorController
        .authorisedNoAts(currentTaxYearSA)
        .url
    }

    "hide rows if there is a zero value in the left cell amount field of the view" in {

      val model2 = totalIncomeTaxModel.copy(
        startingRateForSavings = Amount(0, "GBP"),
        basicRateIncomeTax = Amount(0, "GBP")
      )

      when(mockTotalIncomeTaxService.getIncomeAndNIData(any())(any(), any()))
        .thenReturn(Future.successful(model2))

      val result   = nicsController.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString must not include "Technical Difficulties"
      document.toString must not include "starting-rate-for-savings-row"
      document.toString must not include "basic-rate-income-tax-row"
      document.toString must not include "higher-rate-income-tax-row"
      document.toString must not include "additional-rate-income-tax-row"
    }

    "hide Higher and Additional Rate fields if the amounts are 0.00" in {

      val model3 = totalIncomeTaxModel.copy(
        higherRateIncomeTax = Amount(0, "GBP"),
        additionalRateIncomeTax = Amount(0, "GBP")
      )

      when(mockTotalIncomeTaxService.getIncomeAndNIData(any())(any(), any()))
        .thenReturn(Future.successful(model3))

      val result   = nicsController.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString must not include "Technical Difficulties"
      document.toString must include("basic-rate-income-tax-row")
      document.toString must not include "higher-rate-income-tax-row"
      document.toString must not include "additional-rate-income-tax-row"
    }

    "have the right user data in the view" in {

      val result   = nicsController.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-cg-tax-rate").text() mustBe "56.78%"
      document.getElementById("employee-nic-amount").text() mustBe "£1,200"
      document.getElementById("total-income-tax-and-nics").text() mustBe "£1,572"
      document.getElementById("user-info").text must include("forename surname")
      document.getElementById("user-info").text must include("Unique Taxpayer Reference: " + testUtr)

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

      document.getElementById("user-info").text must include("forename surname")
      document.getElementById("user-info").text must include("Unique Taxpayer Reference: " + testUtr)

    }
  }

  "Dividends section" must {
    "have the right user data for Ordinary, Additional and Higher Rates fields in the view" in {

      val result   = nicsController.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString                                      must not include "Technical Difficulties"
      document.getElementById("ordinary-rate-amount").text() must equal("£50")
      document.getElementById("ordinary-rate-before").text() must equal("£100")
      document.getElementById("ordinary-rate-rate").text()   must equal("10%")

      document.getElementById("upper-rate-amount").text() must equal("£120")
      document.getElementById("upper-rate-before").text() must equal("£30")
      document.getElementById("upper-rate-rate").text()   must equal("32.5%")

      document.getElementById("additional-rate-amount").text() must equal("£40")
      document.getElementById("additional-rate-before").text() must equal("£10")
      document.getElementById("additional-rate-rate").text()   must equal("37.5%")
    }

    "hide Dividends section if the amount before in each row is 0.00" in {

      val model4 = totalIncomeTaxModel.copy(
        ordinaryRate = Amount(0, "GBP"),
        upperRate = Amount(0, "GBP"),
        additionalRate = Amount(0, "GBP")
      )
      when(mockTotalIncomeTaxService.getIncomeAndNIData(any())(any(), any()))
        .thenReturn(Future.successful(model4))

      val result   = nicsController.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString must not include "Technical Difficulties"
      document.toString must not include "dividends-section-row"
      document.toString must not include "ordinary-rate-row"
      document.toString must not include "upper-rate-row"
      document.toString must not include "additional-rate-row"
    }

    "not hide Dividends section if only Ordinary rate amount is greater than 0.00" in {

      val model5 = totalIncomeTaxModel.copy(
        upperRate = Amount(0, "GBP"),
        additionalRate = Amount(0, "GBP")
      )
      when(mockTotalIncomeTaxService.getIncomeAndNIData(meq(currentTaxYearSA))(any(), meq(request)))
        .thenReturn(Future.successful(model5))

      val result   = nicsController.show(request)
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

      val result   = nicsController.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("other-adjustments-increasing-amount").text() must equal("£90")
      document.getElementById("other-adjustments-reducing-amount").text()   must equal("− £20")
    }

    "hide other adjustments increasing your tax section if the amount is 0.00" in {

      val model6 = totalIncomeTaxModel.copy(
        otherAdjustmentsIncreasing = Amount(0, "GBP")
      )
      when(mockTotalIncomeTaxService.getIncomeAndNIData(any())(any(), any()))
        .thenReturn(Future.successful(model6))

      val result   = nicsController.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString must not include "Technical Difficulties"
      document.toString must not include "other-adjustments-increasing-amount"

    }

    "hide other adjustments reducing your tax section if the amount is 0.00" in {

      val model7 = totalIncomeTaxModel.copy(
        otherAdjustmentsReducing = Amount(0, "GBP")
      )
      when(mockTotalIncomeTaxService.getIncomeAndNIData(any())(any(), any()))
        .thenReturn(Future.successful(model7))

      val result   = nicsController.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString must not include "Technical Difficulties"
      document.toString must not include "other-adjustments-reducing-amount"
    }

    "hide Adjustments section if all the amounts in this section are 0.00" in {

      val model8 = totalIncomeTaxModel.copy(
        otherAdjustmentsIncreasing = Amount(0, "GBP"),
        otherAdjustmentsReducing = Amount(0, "GBP")
      )
      when(mockTotalIncomeTaxService.getIncomeAndNIData(any())(any(), any()))
        .thenReturn(Future.successful(model8))

      val result   = nicsController.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString must not include "Technical Difficulties"
      document.toString must not include "adjustments-section"
    }
  }

}
