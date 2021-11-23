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

import config.{ApplicationConfig, PayeConfig}
import controllers.auth.{FakePayeAuthAction, PayeAuthenticatedRequest}
import models.PayeAtsData
import org.jsoup.Jsoup
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import play.api.Configuration
import play.api.http.Status._
import play.api.i18n.Messages
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.TestConstants.testNino
import views.html.paye.PayeIncomeTaxAndNicsView

import scala.concurrent.Future

class PayeIncomeTaxAndNicsControllerSpec extends PayeControllerSpecHelpers {

  val fakeAuthenticatedRequest = buildPayeRequest("/annual-tax-summary/paye/total-income-tax")
  val sut =
    new PayeIncomeTaxAndNicsController(
      mockPayeAtsService,
      FakePayeAuthAction,
      mcc,
      inject[PayeIncomeTaxAndNicsView],
      inject[PayeConfig])

  "Paye your income tax and nics controller" must {

    "return OK response" in {

      when(
        mockPayeAtsService
          .getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier], any[PayeAuthenticatedRequest[_]]))
        .thenReturn(Future(Right(expectedResponse2021.as[PayeAtsData])))

      val result = sut.show(taxYear)(fakeAuthenticatedRequest)

      status(result) mustBe OK

      val document = Jsoup.parse(contentAsString(result))

      document.title must include(
        Messages("paye.ats.total_income_tax.title") + Messages(
          "generic.to_from",
          (taxYear - 1).toString,
          taxYear.toString))
    }

    "return OK response when tax year is set to 2020" in {

      class FakeAppConfig extends ApplicationConfig(inject[ServicesConfig], inject[Configuration]) {
        override lazy val taxYear = 2020
      }

      val fakeAppConfig = new FakeAppConfig

      class FakePayeConfig extends PayeConfig {
        override val payeYear: Int = fakeAppConfig.taxYear
      }

      val fakePayeConfig = new FakePayeConfig

      val fakeAuthenticatedRequest = buildPayeRequest("/annual-tax-summary/paye/total-income-tax")
      val sut =
        new PayeIncomeTaxAndNicsController(
          mockPayeAtsService,
          FakePayeAuthAction,
          mcc,
          inject[PayeIncomeTaxAndNicsView],
          fakePayeConfig)(implicitly, fakeAppConfig, implicitly)

      when(
        mockPayeAtsService
          .getPayeATSData(eqTo(testNino), eqTo(fakeAppConfig.taxYear))(
            any[HeaderCarrier],
            any[PayeAuthenticatedRequest[_]]))
        .thenReturn(Future(Right(expectedResponse2020.as[PayeAtsData])))

      val result = sut.show(fakePayeConfig.payeYear)(fakeAuthenticatedRequest)

      status(result) mustBe OK

      val document = Jsoup.parse(contentAsString(result))

      document.title must include(
        Messages("paye.ats.total_income_tax.title") + Messages(
          "generic.to_from",
          (fakePayeConfig.payeYear - 1).toString,
          fakePayeConfig.payeYear.toString))
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

    "redirect user to generic error page when receiving INTERNAL_SERVER_ERROR from service" in {

      when(
        mockPayeAtsService
          .getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier], any[PayeAuthenticatedRequest[_]]))
        .thenReturn(Future(Left(HttpResponse(INTERNAL_SERVER_ERROR, ""))))

      val result = sut.show(taxYear)(fakeAuthenticatedRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.PayeErrorController.genericError(500).url
    }

    "redirect user to generic error page when receiving BAD_REQUEST from service" in {

      when(
        mockPayeAtsService
          .getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier], any[PayeAuthenticatedRequest[_]]))
        .thenReturn(Future(Left(HttpResponse(BAD_REQUEST, ""))))

      val result = sut.show(taxYear)(fakeAuthenticatedRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.PayeErrorController.genericError(400).url
    }
  }
}
