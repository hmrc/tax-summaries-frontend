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

import config.PayeConfig
import controllers.auth.{FakePayeAuthAction, PayeAuthenticatedRequest}
import models.{AtsBadRequestResponse, AtsErrorResponse, AtsNotFoundResponse, PayeAtsData}
import org.jsoup.Jsoup
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import play.api.http.Status._
import play.api.i18n.Messages
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestConstants.testNino
import views.html.errors.PayeGenericErrorView
import views.html.paye.PayeIncomeTaxAndNicsView

import scala.concurrent.Future

class PayeIncomeTaxAndNicsControllerSpec extends PayeControllerSpecHelpers {

  implicit val fakeAuthenticatedRequest = buildPayeRequest("/annual-tax-summary/paye/total-income-tax")

  lazy val payeGenericErrorView = inject[PayeGenericErrorView]

  val sut =
    new PayeIncomeTaxAndNicsController(
      mockPayeAtsService,
      FakePayeAuthAction,
      mcc,
      inject[PayeIncomeTaxAndNicsView],
      inject[PayeConfig],
      payeGenericErrorView)

  "Paye your income tax and nics controller" must {

    "return OK response" in {

      when(
        mockPayeAtsService
          .getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier], any[PayeAuthenticatedRequest[_]]))
        .thenReturn(Future(Right(expectedResponse.as[PayeAtsData])))

      val result = sut.show(taxYear)(fakeAuthenticatedRequest)

      status(result) mustBe OK

      val document = Jsoup.parse(contentAsString(result))

      document.title must include(
        Messages("paye.ats.total_income_tax.title") + Messages(
          "generic.to_from",
          (taxYear - 1).toString,
          taxYear.toString))
    }

    "redirect user to noAts page when receiving NOT_FOUND from service" in {

      when(
        mockPayeAtsService
          .getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier], any[PayeAuthenticatedRequest[_]]))
        .thenReturn(Future(Left(AtsNotFoundResponse(""))))

      val result = sut.show(taxYear)(fakeAuthenticatedRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.ErrorController.authorisedNoAts(appConfig.taxYear).url
    }

    "redirect user to generic error page when receiving INTERNAL_SERVER_ERROR from service" in {

      when(
        mockPayeAtsService
          .getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier], any[PayeAuthenticatedRequest[_]]))
        .thenReturn(Future(Left(AtsErrorResponse(""))))

      val result = sut.show(taxYear)(fakeAuthenticatedRequest)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) mustBe payeGenericErrorView().toString()
    }

    "redirect user to generic error page when receiving BAD_REQUEST from service" in {

      when(
        mockPayeAtsService
          .getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier], any[PayeAuthenticatedRequest[_]]))
        .thenReturn(Future(Left(AtsBadRequestResponse(""))))

      val result = sut.show(taxYear)(fakeAuthenticatedRequest)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) mustBe payeGenericErrorView().toString()
    }
  }
}
