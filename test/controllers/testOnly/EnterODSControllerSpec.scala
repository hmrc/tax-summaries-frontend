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

package controllers.testOnly

import connectors.MiddleConnector
import connectors.testOnly.TaxSummariesStubsConnector
import forms.testOnly.EnterODSFormProvider
import models.testOnly.{CountryAndODSValues, SAODSModel}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.ControllerBaseSpec
import views.html.testOnly.EnterODSView

import scala.concurrent.Future

class EnterODSControllerSpec extends ControllerBaseSpec {
  private val formProvider                   = new EnterODSFormProvider
  private val view                           = inject[EnterODSView]
  private val mockMiddleConnector            = mock[MiddleConnector]
  private val mockTaxSummariesStubsConnector = mock[TaxSummariesStubsConnector]

  private def controller = new EnterODSController(
    mcc,
    view,
    formProvider,
    mockMiddleConnector,
    mockTaxSummariesStubsConnector
  )

  private val utr     = "00000000010"
  private val country = "0001"

  private val atsSaFields = Seq(
    "abc",
    "def",
    "ghi",
    "aaa"
  )

  private val saODSModel = SAODSModel(utr, taxYear, country, Nil)

  override def beforeEach(): Unit = {
    reset(mockMiddleConnector)
    reset(mockTaxSummariesStubsConnector)
    when(mockMiddleConnector.connectToAtsSaFields(any())(any())).thenReturn(
      Future.successful(Right(atsSaFields))
    )
    when(mockTaxSummariesStubsConnector.get(any(), any())(any(), any()))
      .thenReturn(Future.successful(saODSModel))
    when(mockTaxSummariesStubsConnector.save(any(), any(), any())(any(), any()))
      .thenReturn(Future.successful((): Unit))
  }

  "onPageLoad" must {
    "render the page" in {
      val result = controller.onPageLoad(taxYear, utr)(request)

      status(result) mustBe OK
    }
  }

  "onSubmit" must {
    "redirect when request valid and add in any missing values with order as per fields list" in {
      val postRequest =
        FakeRequest("POST", "/").withFormUrlEncodedBody(("country", "0001"), ("odsValues", "def 180.99"))
      val result      = controller.onSubmit(taxYear, utr)(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(
        controllers.testOnly.routes.DisplayPTAController.onPageLoad(taxYear, utr).url
      )
      val expSavedCountryAndOdsValues =
        CountryAndODSValues(
          country = country,
          odsValues = Map("aaa" -> "0.00", "abc" -> "0.00", "def" -> "180.99", "ghi" -> "0.00")
        )
      verify(mockTaxSummariesStubsConnector, times(1))
        .save(ArgumentMatchers.eq(taxYear), ArgumentMatchers.eq(utr), ArgumentMatchers.eq(expSavedCountryAndOdsValues))(
          any(),
          any()
        )
    }

    "return bad request when request invalid" in {
      val postRequest = FakeRequest("POST", "/").withFormUrlEncodedBody(("country", "0006"))
      val result      = controller.onSubmit(taxYear, utr)(postRequest)

      status(result) mustBe BAD_REQUEST
    }

    "return bad request when request invalid due to invalid ods field" in {
      val postRequest =
        FakeRequest("POST", "/").withFormUrlEncodedBody(("country", "0001"), ("odsValues", "abcd 180.99"))
      val result      = controller.onSubmit(taxYear, utr)(postRequest)

      status(result) mustBe BAD_REQUEST
    }

  }
}
