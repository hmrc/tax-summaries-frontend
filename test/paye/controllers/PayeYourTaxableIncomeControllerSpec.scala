/*
 * Copyright 2024 HM Revenue & Customs
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
import common.models.{AtsErrorResponse, AtsNotFoundResponse, requests}
import common.utils.TestConstants.testNino
import paye.views.html.errors.PayeGenericErrorView
import paye.views.html.PayeYourTaxableIncomeView
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import paye.models.PayeAtsData
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class PayeYourTaxableIncomeControllerSpec extends PayeControllerSpecHelpers {

  implicit val fakeAuthenticatedRequest: PayeAuthenticatedRequest[AnyContentAsEmpty.type] =
    requests.PayeAuthenticatedRequest(
      testNino,
      fakeCredentials,
      FakeRequest("GET", "/annual-tax-summary/paye/treasury-spending")
    )

  lazy val payeGenericErrorView: PayeGenericErrorView = inject[PayeGenericErrorView]

  val sut =
    new PayeYourTaxableIncomeController(
      mockPayeAtsService,
      FakeAuthJourney,
      mcc,
      inject[PayeYourTaxableIncomeView],
      payeGenericErrorView
    )

  "Government spend controller" must {

    s"return OK response when set to $currentTaxYearPAYE" in {

      val taxYear = currentTaxYearPAYE

      when(mockPayeAtsService.getPayeATSData(any(), any())(any()))
        .thenReturn(Future(Right(apiResponsePayeAtsDataCurrentTaxYear.as[PayeAtsData])))

      val result = sut.show(taxYear)(fakeAuthenticatedRequest)

      status(result) mustBe OK

      contentAsString(result) must include(
        Messages("paye.ats.income_before_tax.title") + Messages(
          "generic.to_from",
          (taxYear - 1).toString,
          taxYear.toString
        )
      )
    }

    s"return OK response when set to ${currentTaxYearPAYE - 1}" in {

      val taxYear = currentTaxYearPAYE - 1

      when(mockPayeAtsService.getPayeATSData(any(), any())(any[HeaderCarrier]))
        .thenReturn(Future(Right(apiResponsePayeAtsDataPreviousTaxYear.as[PayeAtsData])))

      val result = sut.show(taxYear)(fakeAuthenticatedRequest)

      status(result) mustBe OK

      contentAsString(result) must include(
        Messages("paye.ats.income_before_tax.title") + Messages(
          "generic.to_from",
          (taxYear - 1).toString,
          taxYear.toString
        )
      )
    }

    "redirect user to noAts page when receiving NOT_FOUND from service" in {

      when(
        mockPayeAtsService
          .getPayeATSData(any(), any())(any[HeaderCarrier])
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
          .getPayeATSData(any(), any())(any[HeaderCarrier])
      )
        .thenReturn(Future(Left(AtsErrorResponse(""))))

      val result = sut.show(currentTaxYearPAYE)(fakeAuthenticatedRequest)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) mustBe payeGenericErrorView().toString()
    }
  }
}
