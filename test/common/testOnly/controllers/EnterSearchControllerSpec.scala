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

package common.testOnly.controllers

import common.config.ApplicationConfig
import common.models.requests.AuthenticatedRequest
import org.mockito.Mockito.reset
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import common.testOnly.forms.EnterSearchFormProvider
import common.testOnly.views.html.EnterSearchView
import common.utils.ControllerBaseSpec

class EnterSearchControllerSpec extends ControllerBaseSpec {
  private val mockApplicationConfig = mock[ApplicationConfig]
  private val formProvider          = new EnterSearchFormProvider
  private val view                  = inject[EnterSearchView]

  private def controller                                            = new EnterSearchController(
    mcc,
    view,
    formProvider,
    mockApplicationConfig
  )
  private val request: AuthenticatedRequest[AnyContentAsEmpty.type] = buildRequest(currentTaxYearSA)

  override def beforeEach(): Unit =
    reset(mockApplicationConfig)

  "onPageLoad" must {
    "render the page" in {
      val result = controller.onPageLoad(request)

      status(result) mustBe OK
    }
  }

  "onSubmit" must {
    "redirect when request valid" in {
      val postRequest = FakeRequest("POST", "/")
        .withFormUrlEncodedBody(("taxYear", s"$currentTaxYearSA"), ("utr", "0000000010"))
      val result      = controller.onSubmit(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(
        common.testOnly.controllers.routes.EnterODSController.onPageLoad(currentTaxYearSA, "0000000010").url
      )
    }

    "return bad request when request invalid" in {
      val postRequest = FakeRequest("POST", "/").withFormUrlEncodedBody(("taxYear", "1998"))
      val result      = controller.onSubmit(postRequest)

      status(result) mustBe BAD_REQUEST
    }
  }
}
