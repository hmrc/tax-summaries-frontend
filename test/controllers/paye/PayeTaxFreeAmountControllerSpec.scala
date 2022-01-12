/*
 * Copyright 2022 HM Revenue & Customs
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

import config.ApplicationConfig
import controllers.auth.{FakePayeAuthAction, PayeAuthenticatedRequest}
import models.{AtsErrorResponse, AtsNotFoundResponse, PayeAtsData}
import org.jsoup.Jsoup
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import play.api.Configuration
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.TestConstants.testNino
import views.html.errors.PayeGenericErrorView
import views.html.paye.PayeTaxFreeAmountView

import scala.concurrent.Future

class PayeTaxFreeAmountControllerSpec extends PayeControllerSpecHelpers {

  implicit val fakeAuthenticatedRequest = buildPayeRequest(routes.PayeTaxFreeAmountController.show(taxYear).url)
  lazy val payeGenericErrorView = inject[PayeGenericErrorView]

  val sut = new PayeTaxFreeAmountController(
    mockPayeAtsService,
    FakePayeAuthAction,
    mcc,
    inject[PayeTaxFreeAmountView],
    payeGenericErrorView)

  "Tax Free Amount controller" must {

    "return OK response" in {

      class FakeAppConfig extends ApplicationConfig(inject[ServicesConfig], inject[Configuration]) {
        override lazy val taxYear = 2021
      }

      val fakeAppConfig = new FakeAppConfig

      val fakeAuthenticatedRequest =
        buildPayeRequest(routes.PayeTaxFreeAmountController.show(fakeAppConfig.taxYear).url)

      when(
        mockPayeAtsService
          .getPayeATSData(eqTo(testNino), eqTo(fakeAppConfig.taxYear))(
            any[HeaderCarrier],
            any[PayeAuthenticatedRequest[_]]))
        .thenReturn(Future(Right(expectedResponse2021.as[PayeAtsData])))

      val result = sut.show(fakeAppConfig.taxYear)(fakeAuthenticatedRequest)

      status(result) mustBe OK

      val document = Jsoup.parse(contentAsString(result))

      document.title must include(
        Messages("paye.ats.tax_free_amount.title") + Messages(
          "generic.to_from",
          (fakeAppConfig.taxYear - 1).toString,
          fakeAppConfig.taxYear.toString))
    }

    "return OK response for 2020" in {

      class FakeAppConfig extends ApplicationConfig(inject[ServicesConfig], inject[Configuration]) {
        override lazy val taxYear = 2020
      }

      val fakeAppConfig = new FakeAppConfig

      val fakeAuthenticatedRequest =
        buildPayeRequest(routes.PayeTaxFreeAmountController.show(fakeAppConfig.taxYear).url)

      when(
        mockPayeAtsService
          .getPayeATSData(eqTo(testNino), eqTo(fakeAppConfig.taxYear))(
            any[HeaderCarrier],
            any[PayeAuthenticatedRequest[_]]))
        .thenReturn(Future(Right(expectedResponse2020.as[PayeAtsData])))

      val result = sut.show(fakeAppConfig.taxYear)(fakeAuthenticatedRequest)

      status(result) mustBe OK

      val document = Jsoup.parse(contentAsString(result))

      document.title must include(
        Messages("paye.ats.tax_free_amount.title") + Messages(
          "generic.to_from",
          (fakeAppConfig.taxYear - 1).toString,
          fakeAppConfig.taxYear.toString))
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

    "show Generic Error page and return INTERNAL_SERVER_ERROR if error received from NPS service" in {

      when(
        mockPayeAtsService
          .getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier], any[PayeAuthenticatedRequest[_]]))
        .thenReturn(Future(Left(AtsErrorResponse(""))))

      val result = sut.show(taxYear)(fakeAuthenticatedRequest)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) mustBe payeGenericErrorView().toString()
    }
  }
}
