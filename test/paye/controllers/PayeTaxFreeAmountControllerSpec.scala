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

package paye.controllers

import common.config.ApplicationConfig
import common.controllers.auth.FakeAuthJourney
import common.models.requests.PayeAuthenticatedRequest
import common.models.{AtsErrorResponse, AtsNotFoundResponse, PayeAtsData}
import paye.views.html.errors.PayeGenericErrorView
import paye.views.html.PayeTaxFreeAmountView
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers.*
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.Future

class PayeTaxFreeAmountControllerSpec extends PayeControllerSpecHelpers {

  implicit val fakeAuthenticatedRequest: PayeAuthenticatedRequest[AnyContentAsEmpty.type] = buildPayeRequest(
    paye.controllers.routes.PayeTaxFreeAmountController.show(currentTaxYearPAYE).url
  )
  lazy val payeGenericErrorView: PayeGenericErrorView                                     = inject[PayeGenericErrorView]

  val sut = new PayeTaxFreeAmountController(
    mockPayeAtsService,
    FakeAuthJourney,
    mcc,
    inject[PayeTaxFreeAmountView],
    payeGenericErrorView
  )

  "Tax Free Amount controller" must {

    "return OK response" in {

      class FakeAppConfig extends ApplicationConfig(inject[ServicesConfig]) {
        override lazy val taxYearSA = currentTaxYearPAYE
      }

      val fakeAppConfig = new FakeAppConfig

      val fakeAuthenticatedRequest =
        buildPayeRequest(paye.controllers.routes.PayeTaxFreeAmountController.show(fakeAppConfig.taxYearSA).url)

      when(mockPayeAtsService.getPayeATSData(any(), any())(any()))
        .thenReturn(Future(Right(apiResponsePayeAtsDataCurrentTaxYear.as[PayeAtsData])))

      val result = sut.show(fakeAppConfig.taxYearSA)(fakeAuthenticatedRequest)

      status(result) mustBe OK

      val document = Jsoup.parse(contentAsString(result))

      document.title must include(
        Messages("paye.ats.tax_free_amount.title") + Messages(
          "generic.to_from",
          (fakeAppConfig.taxYearSA - 1).toString,
          fakeAppConfig.taxYearSA.toString
        )
      )
    }

    s"return OK response for ${currentTaxYearPAYE - 1}" in {

      class FakeAppConfig extends ApplicationConfig(inject[ServicesConfig]) {
        override lazy val taxYearSA = currentTaxYearPAYE - 1
      }

      val fakeAppConfig = new FakeAppConfig

      val fakeAuthenticatedRequest =
        buildPayeRequest(paye.controllers.routes.PayeTaxFreeAmountController.show(fakeAppConfig.taxYearSA).url)

      when(mockPayeAtsService.getPayeATSData(any(), any())(any()))
        .thenReturn(Future(Right(apiResponsePayeAtsDataPreviousTaxYear.as[PayeAtsData])))

      val result = sut.show(fakeAppConfig.taxYearSA)(fakeAuthenticatedRequest)

      status(result) mustBe OK

      val document = Jsoup.parse(contentAsString(result))

      document.title must include(
        Messages("paye.ats.tax_free_amount.title") + Messages(
          "generic.to_from",
          (fakeAppConfig.taxYearSA - 1).toString,
          fakeAppConfig.taxYearSA.toString
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
