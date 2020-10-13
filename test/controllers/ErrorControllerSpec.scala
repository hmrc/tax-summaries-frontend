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

package controllers

import java.time.LocalDate

import controllers.auth._
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.time.CurrentTaxYear
import utils.TestConstants._

class ErrorControllerSpec extends ControllerBaseSpec with CurrentTaxYear {

  override def now: () => LocalDate = () => LocalDate.now()

  def sut =
    new ErrorController(
      FakeAuthAction,
      FakeMinAuthAction,
      mcc,
      notAuthorisedView,
      howTaxIsSpentView,
      serviceUnavailableView)
  implicit lazy val messageApi = inject[MessagesApi]

  "ErrorController" should {

    "Show No ATS page" in {

      implicit lazy val request =
        AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest())
      val result = sut.authorisedNoAts()(request)
      val document = contentAsString(result)

      status(result) shouldBe 200

      document shouldBe contentAsString(howTaxIsSpentView(current.previous))
    }

    "show not authorised page" in {

      implicit lazy val request = AuthenticatedRequest("userId", None, None, None, None, None, None, FakeRequest())
      val result = sut.notAuthorised()(request)
      val document = contentAsString(result)

      status(result) shouldBe 200

      document shouldBe contentAsString(notAuthorisedView())
    }
  }
}
