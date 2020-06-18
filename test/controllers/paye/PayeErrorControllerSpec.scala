/*
 * Copyright 2020 HM Revenue & Customs
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

import controllers.ControllerBaseSpec
import controllers.auth.FakePayeAuthAction
import play.api.test.Helpers._
import view_models.paye.PayeAtsMain

class PayeErrorControllerSpec extends PayeControllerSpecHelpers with ControllerBaseSpec {

  implicit val fakeAuthenticatedRequest = buildPayeRequest("/annual-tax-summary/paye/treasury-spending")

  def sut = new PayeErrorController(FakePayeAuthAction, mcc)

  "PayeErrorController" should {

    "Show generic_error page with status INTERNAL_SERVER_ERROR when INTERNAL_SERVER_ERROR is received" in {

      val result = sut.genericError(INTERNAL_SERVER_ERROR)(fakeAuthenticatedRequest)
      val document = contentAsString(result)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      document shouldBe contentAsString(views.html.errors.paye_generic_error())
    }

    "Show generic_error page with status BAD_GATEWAY when GATEWAY_TIMEOUT is received" in {

      val result = sut.genericError(GATEWAY_TIMEOUT)(fakeAuthenticatedRequest)
      val document = contentAsString(result)

      status(result) shouldBe BAD_GATEWAY
      document shouldBe contentAsString(views.html.errors.paye_generic_error())
    }

    "Show generic_error page with status BAD_GATEWAY when BAD_GATEWAY is received" in {

      val result = sut.genericError(BAD_GATEWAY)(fakeAuthenticatedRequest)
      val document = contentAsString(result)

      status(result) shouldBe BAD_GATEWAY
      document shouldBe contentAsString(views.html.errors.paye_generic_error())
    }

    "Show NO ATS page and return NOT_FOUND" in {
      val result = sut.authorisedNoAts(fakeAuthenticatedRequest)
      val document = contentAsString(result)

      status(result) shouldBe NOT_FOUND
      document shouldBe contentAsString(views.html.errors.paye_no_ats_error(PayeAtsMain(taxYear)))
    }
  }
}