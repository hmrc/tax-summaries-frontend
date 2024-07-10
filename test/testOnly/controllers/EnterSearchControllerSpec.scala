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

package testOnly.controllers

import config.ApplicationConfig
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testOnly.forms.EnterSearchFormProvider
import testOnly.views.html.EnterSearchView
import utils.ControllerBaseSpec

class EnterSearchControllerSpec extends ControllerBaseSpec {
  private val mockApplicationConfig = mock[ApplicationConfig]
  private val formProvider          = new EnterSearchFormProvider
  private val view                  = inject[EnterSearchView]

  private def controller = new EnterSearchController(
    mcc,
    view,
    formProvider,
    mockApplicationConfig
  )

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
      val postRequest = FakeRequest("POST", "/").withFormUrlEncodedBody(("taxYear", "2023"), ("utr", "0000000010"))
      val result      = controller.onSubmit(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(
        testOnly.controllers.routes.EnterODSController.onPageLoad(2023, "0000000010").url
      )
    }

    "return bad request when request invalid" in {
      val postRequest = FakeRequest("POST", "/").withFormUrlEncodedBody(("taxYear", "1998"))
      val result      = controller.onSubmit(postRequest)

      status(result) mustBe BAD_REQUEST
    }
  }
}
