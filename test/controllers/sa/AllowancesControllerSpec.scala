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
import controllers.auth.requests.AuthenticatedRequest
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.Helpers.*
import services.*
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestConstants.*
import utils.{ControllerBaseSpec, TaxYearUtil}
import view_models.*

import scala.concurrent.Future

class AllowancesControllerSpec extends ControllerBaseSpec {
  private val taxYearUtil = app.injector.instanceOf[TaxYearUtil]

  val baseModel: Allowances = Allowances(
    taxYear = currentTaxYearSA,
    utr = testUtr,
    taxFreeAllowance = Amount(9440, "GBP"),
    marriageAllowanceTransferred = Amount(0, "GBP"),
    otherAllowances = Amount(300, "GBP"),
    totalTaxFree = Amount(9740, "GBP"),
    title = "Mr",
    forename = "forename",
    surname = "surname"
  )

  implicit val hc: HeaderCarrier = new HeaderCarrier

  val noATSViewModel: NoATSViewModel = NoATSViewModel(currentTaxYearSA)

  lazy val taxsController: TaxsController = mock[TaxsController]

  val mockAllowanceService: AllowanceService = mock[AllowanceService]
  val mockAuditService: AuditService         = mock[AuditService]

  def sut =
    new AllowancesController(
      mockAllowanceService,
      mockAuditService,
      FakeAuthJourney,
      mcc,
      taxFreeAmountView,
      genericErrorView,
      tokenErrorView,
      taxYearUtil
    )

  override def beforeEach(): Unit = {
    reset(mockFeatureFlagService)
    when(mockAllowanceService.getAllowances(any())(any(), any())).thenReturn(Future.successful(baseModel))
    ()
  }

  private val request: AuthenticatedRequest[AnyContentAsEmpty.type] = buildRequest(currentTaxYearSA)

  "Calling allowances" must {

    "have the right user data in the view when a valid request is sent" in {

      val result = sut.show(request)

      status(result) mustBe 200

      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("tax-free-total").text() mustBe "£9,740"
      document.getElementById("tax-free-allowance-amount").text() mustBe "£9,440"
      document.getElementById("other-allowances").text() mustBe "£300"
      document.toString                           must include("tax-free-allowance")
      document.getElementById("user-info").text() must include("forename surname")
      document.getElementById("user-info").text() must include("Unique Taxpayer Reference: " + testUtr)
      document
        .select("header[data-component='ats_page_heading']")
        .text mustBe s"Tax year: April 6 ${currentTaxYearSA - 1} to April 5 $currentTaxYearSA Your tax-free income"
    }

    "have zero-value fields hidden in the view" in {

      val model = baseModel.copy(
        taxFreeAllowance = Amount(0, "GBP"),
        otherAllowances = Amount(0, "GBP")
      )
      when(mockAllowanceService.getAllowances(any())(any(), any()))
        .thenReturn(Future.successful(model))

      val result: Future[Result] = sut.show(request)
      status(result) mustBe 200
      val document               = Jsoup.parse(contentAsString(result))

      document.toString must not include "tax-free-allowance-amount"
      document.toString must not include "other-allowances"
    }

    "return a successful response for a valid request" in {
      val result   = sut.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.title must include(
        Messages("ats.tax_free_amount.html.title") + Messages(
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
      when(mockAllowanceService.getAllowances(any())(any(), any()))
        .thenReturn(Future.successful(new ATSUnavailableViewModel))

      val result = sut.show(request)
      status(result) mustBe INTERNAL_SERVER_ERROR

      val document = Jsoup.parse(contentAsString(result))
      document.title must include(Messages("global.error.InternalServerError500.title"))
    }

    "redirect to the no ATS page when there is no Annual Tax Summary data returned" in {
      when(mockAllowanceService.getAllowances(any())(any(), any()))
        .thenReturn(Future.successful(NoATSViewModel(currentTaxYearSA)))
      val result = sut.show(request)
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.ErrorController.authorisedNoAts(currentTaxYearSA).url
    }
  }
}
