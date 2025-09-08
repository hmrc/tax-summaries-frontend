/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.http.Status.OK
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import utils.ControllerBaseSpec
import views.html.errors.{PayeNotAuthorisedView, PayeServiceUnavailableView}

class PayeErrorControllerSpec extends ControllerBaseSpec {

  val payeNotAuthorisedView: PayeNotAuthorisedView           = inject[PayeNotAuthorisedView]
  val payeServiceUnavailableView: PayeServiceUnavailableView = inject[PayeServiceUnavailableView]

  val sut = new PayeErrorController(mcc, payeNotAuthorisedView, payeServiceUnavailableView)

  implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  "PayeErrorController" must {

    "show the payeNotAuthorised view" in {
      val result   = sut.notAuthorised()(fakeRequest)
      val document = contentAsString(result)

      status(result) mustBe OK
      document mustBe contentAsString(payeNotAuthorisedView())
    }

    "show the payeServiceUnavailable view" in {
      val result   = sut.serviceUnavailable()(fakeRequest)
      val document = contentAsString(result)

      status(result) mustBe OK
      document mustBe contentAsString(payeServiceUnavailableView())
    }
  }
}
