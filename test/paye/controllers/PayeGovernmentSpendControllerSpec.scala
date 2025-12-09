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

import cats.data.EitherT
import common.config.ApplicationConfig
import common.controllers.auth.FakeAuthJourney
import common.models.requests.PayeAuthenticatedRequest
import common.models.{AtsErrorResponse, AtsNotFoundResponse, PayeAtsData}
import common.services.GovernmentSpendService
import common.utils.TestConstants.governmentSpendFromBackend
import paye.views.html.errors.PayeGenericErrorView
import paye.views.html.PayeGovernmentSpendingView
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

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
    s"return OK response for $currentTaxYearPAYE" in {
      class FakeAppConfig extends ApplicationConfig(inject[ServicesConfig]) {
        override lazy val taxYearGovSpend: Int = currentTaxYearPAYE
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

      when(mockPayeAtsService.getPayeATSData(any(), any())(any()))
        .thenReturn(Future(Right(apiResponsePayeAtsDataCurrentTaxYear.as[PayeAtsData])))

      val response: Seq[(String, Double)] = governmentSpendFromBackend.govSpendAmountData.map { case (key, value) =>
        key -> value.percentage.toDouble
      }

      val serviceResponse: EitherT[Future, AtsErrorResponse, Seq[(String, Double)]] =
        EitherT.rightT(response)

      when(govSpendService.getGovernmentSpendFigures(any())(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(serviceResponse)

      val result = sut.show(currentTaxYearPAYE)(fakeAuthenticatedRequest)

      status(result) mustBe OK

      contentAsString(result) must include(
        Messages("paye.ats.treasury_spending.title") + Messages(
          "generic.to_from",
          (currentTaxYearPAYE - 1).toString,
          currentTaxYearPAYE.toString
        )
      )
    }

    s"return OK response for ${currentTaxYearPAYE - 1}" in {
      when(mockPayeAtsService.getPayeATSData(any(), any())(any()))
        .thenReturn(Future(Right(apiResponsePayeAtsDataPreviousTaxYear.as[PayeAtsData])))

      val response: Seq[(String, Double)] = governmentSpendFromBackend.govSpendAmountData.map { case (key, value) =>
        key -> value.percentage.toDouble
      }

      val serviceResponse: EitherT[Future, AtsErrorResponse, Seq[(String, Double)]] =
        EitherT.rightT(response)

      when(govSpendService.getGovernmentSpendFigures(any())(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(serviceResponse)

      val result = sut.show(currentTaxYearPAYE - 1)(fakeAuthenticatedRequest)

      status(result) mustBe OK

      contentAsString(result) must include(
        Messages("paye.ats.treasury_spending.title") + Messages(
          "generic.to_from",
          (currentTaxYearPAYE - 2).toString,
          (currentTaxYearPAYE - 1).toString
        )
      )
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

    "show Generic Error page and return INTERNAL_SERVER_ERROR if error received from NPS service" in {

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
