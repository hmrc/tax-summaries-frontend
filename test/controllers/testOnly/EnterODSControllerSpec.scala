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

import forms.testOnly.EnterODSFormProvider
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.ControllerBaseSpec
import views.html.testOnly.EnterODSView

class EnterODSControllerSpec extends ControllerBaseSpec {
  private val formProvider = new EnterODSFormProvider
  private val view         = inject[EnterODSView]

  private def controller = new EnterODSController(
    mcc,
    view,
    formProvider
  )

  private val utr = "00000000010"

  override def beforeEach(): Unit = {}

  "onPageLoad" must {
    "render the page" in {
      val result = controller.onPageLoad(taxYear, utr)(request)

      status(result) mustBe OK
    }
  }

  "onSubmit" must {
    "redirect when request valid" in {
      val postRequest = FakeRequest("POST", "/").withFormUrlEncodedBody(("country", "0001"), ("odsValues", "abc"))
      val result      = controller.onSubmit(taxYear, utr)(postRequest)

      status(result) mustBe OK
    }

    "return bad request when request invalid" in {
      val postRequest = FakeRequest("POST", "/").withFormUrlEncodedBody(("country", "0006"))
      val result      = controller.onSubmit(taxYear, utr)(postRequest)

      status(result) mustBe BAD_REQUEST
    }
  }
}
