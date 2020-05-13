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

import controllers.auth.{FakePayeAuthAction, PayeAuthAction, PayeAuthenticatedRequest}
import org.jsoup.Jsoup
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestConstants._
import view_models.paye.PayeAtsMain

class PayeErrorControllerSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar with I18nSupport {

  override def messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]
  val taxYear = 2018

  trait TestErrorController extends PayeErrorController {
    implicit val fakeAuthenticatedRequest = PayeAuthenticatedRequest(testNino, FakeRequest("GET", "/annual-tax-summary/paye/treasury-spending"))
    override val payeAuthAction: PayeAuthAction = FakePayeAuthAction
    override val payeYear = taxYear
  }

  "PayeErrorController" should {

    "Show generic_error page with status INTERNAL_SERVER_ERROR when INTERNAL_SERVER_ERROR is received" in new TestErrorController {

      val result = genericError(INTERNAL_SERVER_ERROR)(fakeAuthenticatedRequest)
      val document = contentAsString(result)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      document shouldBe contentAsString(views.html.errors.paye_generic_error())
    }

    "Show generic_error page with status BAD_GATEWAY when GATEWAY_TIMEOUT is received" in new TestErrorController {

      val result = genericError(GATEWAY_TIMEOUT)(fakeAuthenticatedRequest)
      val document = contentAsString(result)

      status(result) shouldBe BAD_GATEWAY
      document shouldBe contentAsString(views.html.errors.paye_generic_error())
    }

    "Show generic_error page with status BAD_GATEWAY when BAD_GATEWAY is received" in new TestErrorController {

      val result = genericError(BAD_GATEWAY)(fakeAuthenticatedRequest)
      val document = contentAsString(result)

      status(result) shouldBe BAD_GATEWAY
      document shouldBe contentAsString(views.html.errors.paye_generic_error())
    }

    "Show NO ATS page and return NOT_FOUND" in new TestErrorController {
      val result = authorisedNoAts(fakeAuthenticatedRequest)
      val document = contentAsString(result)

      status(result) shouldBe NOT_FOUND
      document shouldBe contentAsString(views.html.errors.paye_no_ats_error(PayeAtsMain(payeYear)))
    }
  }
}