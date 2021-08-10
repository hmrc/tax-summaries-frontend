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

package controllers.paye

import controllers.auth.{FakePayeAuthAction, PayeAuthenticatedRequest}
import models.PayeAtsData
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK, SEE_OTHER}
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.TestConstants.testNino
import views.html.paye.PayeYourTaxableIncomeView

import scala.concurrent.Future

class PayeYourTaxableIncomeControllerSpec extends PayeControllerSpecHelpers {

  val fakeAuthenticatedRequest =
    PayeAuthenticatedRequest(
      testNino,
      false,
      fakeCredentials,
      FakeRequest("GET", "/annual-tax-summary/paye/treasury-spending"))

  val sut =
    new PayeYourTaxableIncomeController(mockPayeAtsService, FakePayeAuthAction, mcc, inject[PayeYourTaxableIncomeView])

  "Government spend controller" must {

    "return OK response" in {

      when(
        mockPayeAtsService
          .getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier], any[PayeAuthenticatedRequest[_]]))
        .thenReturn(Future(Right(expectedResponse.as[PayeAtsData])))

      val result = sut.show(taxYear)(fakeAuthenticatedRequest)

      status(result) mustBe OK

      contentAsString(result) must include(
        Messages("paye.ats.income_before_tax.title") + Messages(
          "generic.to_from",
          (taxYear - 1).toString,
          taxYear.toString))
    }

    "redirect user to noAts page when receiving NOT_FOUND from service" in {

      when(
        mockPayeAtsService
          .getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier], any[PayeAuthenticatedRequest[_]]))
        .thenReturn(Future(Left(HttpResponse(NOT_FOUND, ""))))

      val result = sut.show(taxYear)(fakeAuthenticatedRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.ErrorController.authorisedNoAts(appConfig.taxYear).url
    }

    "show Generic Error page and return INTERNAL_SERVER_ERROR if error received from NPS service" in {

      when(
        mockPayeAtsService
          .getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier], any[PayeAuthenticatedRequest[_]]))
        .thenReturn(Future(Left(HttpResponse(INTERNAL_SERVER_ERROR, ""))))

      val result = sut.show(taxYear)(fakeAuthenticatedRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.PayeErrorController.genericError(INTERNAL_SERVER_ERROR).url
    }
  }
}
