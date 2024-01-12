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

package controllers.paye

import config.{ApplicationConfig, PayeConfig}
import controllers.auth.{FakePayeAuthAction, PayeAuthenticatedRequest}
import models.{AtsBadRequestResponse, AtsErrorResponse, AtsNotFoundResponse, PayeAtsData}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import play.api.Configuration
import play.api.http.Status._
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import views.html.errors.PayeGenericErrorView
import views.html.paye.PayeIncomeTaxAndNicsView

import scala.concurrent.Future

class PayeIncomeTaxAndNicsControllerSpec extends PayeControllerSpecHelpers {

  implicit val fakeAuthenticatedRequest: PayeAuthenticatedRequest[AnyContentAsEmpty.type] = buildPayeRequest(
    "/annual-tax-summary/paye/total-income-tax"
  )

  lazy val payeGenericErrorView: PayeGenericErrorView = inject[PayeGenericErrorView]

  val sut =
    new PayeIncomeTaxAndNicsController(
      mockPayeAtsService,
      FakePayeAuthAction,
      mcc,
      inject[PayeIncomeTaxAndNicsView],
      inject[PayeConfig],
      payeGenericErrorView
    )

  "Paye your income tax and nics controller" must {

    "return OK response" in {

      class FakeAppConfig extends ApplicationConfig(inject[ServicesConfig], inject[Configuration]) {
        override lazy val taxYear = 2021
      }

      val fakeAppConfig = new FakeAppConfig

      class FakePayeConfig extends PayeConfig {
        override val payeYear: Int = fakeAppConfig.taxYear
      }

      val fakePayeConfig = new FakePayeConfig

      val fakeAuthenticatedRequest = buildPayeRequest("/annual-tax-summary/paye/total-income-tax")
      val sut                      =
        new PayeIncomeTaxAndNicsController(
          mockPayeAtsService,
          FakePayeAuthAction,
          mcc,
          inject[PayeIncomeTaxAndNicsView],
          fakePayeConfig,
          payeGenericErrorView
        )(fakeAppConfig, implicitly)

      when(
        mockPayeAtsService
          .getPayeATSData(any(), any())(
            any[HeaderCarrier]
          )
      )
        .thenReturn(Future(Right(expectedResponse2021.as[PayeAtsData])))

      val result = sut.show(fakeAppConfig.taxYear)(fakeAuthenticatedRequest)

      status(result) mustBe OK

      val document = Jsoup.parse(contentAsString(result))

      document.title must include(
        Messages("paye.ats.total_income_tax.title") + Messages(
          "generic.to_from",
          (fakeAppConfig.taxYear - 1).toString,
          fakeAppConfig.taxYear.toString
        )
      )
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
      val sut                      =
        new PayeIncomeTaxAndNicsController(
          mockPayeAtsService,
          FakePayeAuthAction,
          mcc,
          inject[PayeIncomeTaxAndNicsView],
          fakePayeConfig,
          payeGenericErrorView
        )(fakeAppConfig, implicitly)

      when(
        mockPayeAtsService
          .getPayeATSData(any(), any())(
            any[HeaderCarrier]
          )
      )
        .thenReturn(Future(Right(expectedResponse2020.as[PayeAtsData])))

      val result = sut.show(fakePayeConfig.payeYear)(fakeAuthenticatedRequest)

      status(result) mustBe OK

      val document = Jsoup.parse(contentAsString(result))

      document.title must include(
        Messages("paye.ats.total_income_tax.title") + Messages(
          "generic.to_from",
          (fakePayeConfig.payeYear - 1).toString,
          fakePayeConfig.payeYear.toString
        )
      )
    }

    "redirect user to noAts page when receiving NOT_FOUND from service" in {

      when(
        mockPayeAtsService
          .getPayeATSData(any(), any())(any())
      )
        .thenReturn(Future(Left(AtsNotFoundResponse(""))))

      val result = sut.show(taxYear)(fakeAuthenticatedRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.ErrorController.authorisedNoAts(appConfig.taxYear).url
    }

    "redirect user to generic error page when receiving INTERNAL_SERVER_ERROR from service" in {

      when(
        mockPayeAtsService
          .getPayeATSData(any(), any())(any[HeaderCarrier])
      )
        .thenReturn(Future(Left(AtsErrorResponse(""))))

      val result = sut.show(taxYear)(fakeAuthenticatedRequest)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) mustBe payeGenericErrorView().toString()
    }

    "redirect user to generic error page when receiving BAD_REQUEST from service" in {

      when(
        mockPayeAtsService
          .getPayeATSData(any(), any())(any[HeaderCarrier])
      )
        .thenReturn(Future(Left(AtsBadRequestResponse(""))))

      val result = sut.show(taxYear)(fakeAuthenticatedRequest)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) mustBe payeGenericErrorView().toString()
    }
  }
}
