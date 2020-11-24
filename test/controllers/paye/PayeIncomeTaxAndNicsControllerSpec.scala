/*
 * Copyright 2020 HM Revenue & Customs
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

import controllers.ControllerBaseSpec
import controllers.auth.{FakePayeAuthAction, PayeAuthenticatedRequest}
import models.PayeAtsData
import org.jsoup.Jsoup
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import play.api.http.Status._
import play.api.i18n.Messages
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.TestConstants.testNino
import views.html.paye.PayeIncomeTaxAndNicsView

class PayeIncomeTaxAndNicsControllerSpec extends PayeControllerSpecHelpers with ControllerBaseSpec {

  val fakeAuthenticatedRequest = buildPayeRequest("/annual-tax-summary/paye/total-income-tax")
  val sut =
    new PayeIncomeTaxAndNicsController(mockPayeAtsService, FakePayeAuthAction, mcc, inject[PayeIncomeTaxAndNicsView])

  "Paye your income tax and nics controller" should {

    "return OK response" in {

      when(
        mockPayeAtsService
          .getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier], any[PayeAuthenticatedRequest[_]]))
        .thenReturn(Right(expectedResponse.as[PayeAtsData]))

      val result = sut.show(taxYear)(fakeAuthenticatedRequest)

      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))

      document.title should include(
        Messages("paye.ats.total_income_tax.title") + Messages(
          "generic.to_from",
          taxYear.toString,
          (taxYear + 1).toString))
    }

    "redirect user to noAts page when receiving NOT_FOUND from service" in {

      when(
        mockPayeAtsService
          .getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier], any[PayeAuthenticatedRequest[_]]))
        .thenReturn(Left(HttpResponse(NOT_FOUND, "")))

      val result = sut.show(taxYear)(fakeAuthenticatedRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.PayeErrorController.authorisedNoAts().url
    }

    "redirect user to generic error page when receiving INTERNAL_SERVER_ERROR from service" in {

      when(
        mockPayeAtsService
          .getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier], any[PayeAuthenticatedRequest[_]]))
        .thenReturn(Left(HttpResponse(INTERNAL_SERVER_ERROR, "")))

      val result = sut.show(taxYear)(fakeAuthenticatedRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.PayeErrorController.genericError(500).url
    }

    "redirect user to generic error page when receiving BAD_REQUEST from service" in {

      when(
        mockPayeAtsService
          .getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier], any[PayeAuthenticatedRequest[_]]))
        .thenReturn(Left(HttpResponse(BAD_REQUEST, "")))

      val result = sut.show(taxYear)(fakeAuthenticatedRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.PayeErrorController.genericError(400).url
    }
  }
}
