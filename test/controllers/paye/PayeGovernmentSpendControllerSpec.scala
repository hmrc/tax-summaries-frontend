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

import cats.data.EitherT
import config.ApplicationConfig
import controllers.auth.FakeAuthJourney
import controllers.auth.requests.PayeAuthenticatedRequest
import models.{AtsErrorResponse, AtsNotFoundResponse, PayeAtsData}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.GovernmentSpendService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.TestConstants.governmentSpendFromBackend
import views.html.errors.PayeGenericErrorView
import views.html.paye.PayeGovernmentSpendingView

import scala.concurrent.{ExecutionContext, Future}

class PayeGovernmentSpendControllerSpec extends PayeControllerSpecHelpers {

  implicit val fakeAuthenticatedRequest: PayeAuthenticatedRequest[AnyContentAsEmpty.type] = buildPayeRequest(
    "/annual-tax-summary/paye/treasury-spending"
  )

  lazy val payeGenericErrorView: PayeGenericErrorView = inject[PayeGenericErrorView]

  val govSpendService: GovernmentSpendService = mock[GovernmentSpendService]

  val sut =
    new PayeGovernmentSpendController(
      mockPayeAtsService,
      FakeAuthJourney,
      mcc,
      inject[PayeGovernmentSpendingView],
      payeGenericErrorView,
      govSpendService
    )

  "Government spend controller" must {

    "return OK response for 2021" in {

      val fakeTaxYear: Int = 2021

      class FakeAppConfig extends ApplicationConfig(inject[ServicesConfig]) {
        override lazy val taxYear: Int = fakeTaxYear
      }

      implicit val appConfig: FakeAppConfig = new FakeAppConfig

      val sut =
        new PayeGovernmentSpendController(
          mockPayeAtsService,
          FakeAuthJourney,
          mcc,
          inject[PayeGovernmentSpendingView],
          payeGenericErrorView,
          govSpendService
        )

      when(
        mockPayeAtsService
          .getPayeATSData(any(), any())(any())
      )
        .thenReturn(Future(Right(expectedResponse2021.as[PayeAtsData])))

      val response: Seq[(String, Double)] = governmentSpendFromBackend.govSpendAmountData.map { case (key, value) =>
        key -> value.percentage.toDouble
      }

      val serviceResponse: EitherT[Future, AtsErrorResponse, Seq[(String, Double)]] =
        EitherT.rightT(response)

      when(govSpendService.getGovernmentSpendFigures(any())(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(serviceResponse)

      val result = sut.show(fakeTaxYear)(fakeAuthenticatedRequest)

      status(result) mustBe OK

      contentAsString(result) must include(
        Messages("paye.ats.treasury_spending.title") + Messages(
          "generic.to_from",
          (fakeTaxYear - 1).toString,
          fakeTaxYear.toString
        )
      )
    }

    "return OK response for 2020" in {

      val taxYear: Int = 2020

      when(
        mockPayeAtsService
          .getPayeATSData(any(), any())(any())
      )
        .thenReturn(Future(Right(expectedResponse2020.as[PayeAtsData])))

      val response: Seq[(String, Double)] = governmentSpendFromBackend.govSpendAmountData.map { case (key, value) =>
        key -> value.percentage.toDouble
      }

      val serviceResponse: EitherT[Future, AtsErrorResponse, Seq[(String, Double)]] =
        EitherT.rightT(response)

      when(govSpendService.getGovernmentSpendFigures(any())(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(serviceResponse)

      val result = sut.show(taxYear)(fakeAuthenticatedRequest)

      status(result) mustBe OK

      contentAsString(result) must include(
        Messages("paye.ats.treasury_spending.title") + Messages(
          "generic.to_from",
          (taxYear - 1).toString,
          taxYear.toString
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

    "show Generic Error page and return INTERNAL_SERVER_ERROR if error received from NPS service" in {

      when(
        mockPayeAtsService
          .getPayeATSData(any(), any())(any())
      )
        .thenReturn(Future(Left(AtsErrorResponse(""))))

      val result = sut.show(taxYear)(fakeAuthenticatedRequest)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) mustBe payeGenericErrorView().toString()
    }
  }

}
