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

package common.controllers.paye

import common.controllers.auth.FakeAuthJourney
import common.models.requests.PayeAuthenticatedRequest
import common.models.{AtsBadRequestResponse, AtsErrorResponse, AtsNotFoundResponse, PayeAtsData}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status._
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import common.services.atsData.PayeAtsTestData
import common.views.html.errors.PayeGenericErrorView
import common.views.html.paye.PayeYourIncomeAndTaxesView

import scala.concurrent.Future

class PayeYourIncomeAndTaxesControllerSpec extends PayeControllerSpecHelpers {

  implicit val fakeAuthenticatedRequest: PayeAuthenticatedRequest[AnyContentAsEmpty.type] = buildPayeRequest(
    "/annual-tax-summary/paye/treasury-spending"
  )

  lazy val payeAtsTestData: PayeAtsTestData           = inject[PayeAtsTestData]
  lazy val payeGenericErrorView: PayeGenericErrorView = inject[PayeGenericErrorView]

  val sut = new PayeYourIncomeAndTaxesController(
    mockPayeAtsService,
    FakeAuthJourney,
    mcc,
    inject[PayeYourIncomeAndTaxesView],
    payeGenericErrorView
  )

  "Government spend controller" must {

    "return OK response" in {

      when(mockPayeAtsService.getPayeATSData(any(), any())(any()))
        .thenReturn(Future(Right(apiResponsePayeAtsDataPreviousTaxYear.as[PayeAtsData])))

      val result = sut.show(currentTaxYearPAYE)(fakeAuthenticatedRequest)

      status(result) mustBe OK

      val document = Jsoup.parse(contentAsString(result))

      document.title must include(
        Messages("paye.ats.summary.title") + Messages(
          "generic.to_from",
          (currentTaxYearPAYE - 1).toString,
          currentTaxYearPAYE.toString
        )
      )
    }

    "return 200 when total_income_before_tax key is missing in PAYE ATS data" in {

      when(
        mockPayeAtsService
          .getPayeATSData(any(), any())(any())
      )
        .thenReturn(Future(Right(payeAtsTestData.malformedYourIncomeAndTaxesData)))

      val result = sut.show(currentTaxYearPAYE)(fakeAuthenticatedRequest)

      status(result) mustBe OK
    }

    "throw internal server exception when summary data is missing" in {

      when(
        mockPayeAtsService
          .getPayeATSData(any(), any())(any())
      )
        .thenReturn(Future(Right(payeAtsTestData.missingYourIncomeAndTaxesData)))

      val result = sut.show(currentTaxYearPAYE)(fakeAuthenticatedRequest)

      status(result) mustBe INTERNAL_SERVER_ERROR

      contentAsString(result) mustBe "Missing Paye ATS data"
    }

    "redirect user to noAts page when receiving NOT_FOUND from service" in {

      when(
        mockPayeAtsService
          .getPayeATSData(any(), any())(any())
      )
        .thenReturn(Future(Left(AtsNotFoundResponse(""))))

      val result = sut.show(currentTaxYearPAYE)(fakeAuthenticatedRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe common.controllers.routes.ErrorController
        .authorisedNoAts(currentTaxYearPAYE)
        .url
    }

    "show Generic Error page and return INTERNAL_SERVER_ERROR when receiving BAD_REQUEST from service" in {

      when(
        mockPayeAtsService
          .getPayeATSData(any(), any())(any())
      )
        .thenReturn(Future(Left(AtsBadRequestResponse(""))))

      val result = sut.show(currentTaxYearPAYE)(fakeAuthenticatedRequest)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) mustBe payeGenericErrorView().toString()
    }

    "show Generic Error page and return INTERNAL_SERVER_ERROR if error received from service" in {

      when(
        mockPayeAtsService
          .getPayeATSData(any(), any())(any())
      )
        .thenReturn(Future(Left(AtsErrorResponse(""))))

      val result = sut.show(currentTaxYearPAYE)(fakeAuthenticatedRequest)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) mustBe payeGenericErrorView().toString()
    }
  }

}
