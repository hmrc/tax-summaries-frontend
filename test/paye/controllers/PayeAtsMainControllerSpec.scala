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

package paye.controllers

import common.controllers.auth.FakeAuthJourney
import common.models.requests.PayeAuthenticatedRequest
import common.models.{AtsErrorResponse, AtsNotFoundResponse}
import common.utils.JsonUtil
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import paye.models.PayeAtsData
import paye.view_models.PayeAtsMain
import paye.views.html.PayeTaxsMainView
import paye.views.html.errors.PayeGenericErrorView
import play.api.http.Status.*
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}

import scala.concurrent.Future

class PayeAtsMainControllerSpec extends PayeControllerSpecHelpers with JsonUtil {

  implicit val fakeAuthenticatedRequest: PayeAuthenticatedRequest[AnyContentAsEmpty.type] = buildPayeRequest(
    "/annual-tax-summary/paye/treasury-spending"
  )

  lazy val mainView: PayeTaxsMainView                 = inject[PayeTaxsMainView]
  lazy val payeGenericErrorView: PayeGenericErrorView = inject[PayeGenericErrorView]

  def sut: PayeAtsMainController =
    new PayeAtsMainController(mockPayeAtsService, FakeAuthJourney, mcc, mainView, payeGenericErrorView)

  "AtsMain controller" when {

    "return OK response" in {

      when(
        mockPayeAtsService
          .getPayeATSData(any(), any())(any())
      )
        .thenReturn(Future(Right(mock[PayeAtsData])))

      val result = sut.show(currentTaxYearPAYE)(fakeAuthenticatedRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe mainView(PayeAtsMain(currentTaxYearPAYE)).toString
    }

    "redirect user to noAts page when receiving NOT_FOUND from service" in {

      when(
        mockPayeAtsService
          .getPayeATSData(any(), any())(any())
      )
        .thenReturn(Future(Left(AtsNotFoundResponse("Not found"))))

      val result = sut.show(currentTaxYearPAYE)(fakeAuthenticatedRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(
        common.controllers.routes.ErrorController.authorisedNoAts(currentTaxYearPAYE).url
      )
    }

    "show Generic Error page and return INTERNAL_SERVER_ERROR if error received from NPS service" in {
      when(
        mockPayeAtsService
          .getPayeATSData(any(), any())(any())
      )
        .thenReturn(Future(Left(AtsErrorResponse("Error occurred"))))

      val result = sut.show(currentTaxYearPAYE)(fakeAuthenticatedRequest)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) mustBe payeGenericErrorView().toString
    }
  }
}
