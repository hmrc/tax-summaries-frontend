/*
 * Copyright 2019 HM Revenue & Customs
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

import config.AppFormPartialRetriever
import controllers.auth._
import org.jsoup.Jsoup
import org.scalatest.mockito.MockitoSugar
import play.api.Play
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestConstants._

class ErrorControllerSpec extends UnitSpec with FakeTaxsPlayApplication with MockitoSugar with I18nSupport {

  override def messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]

  trait TestErrorController extends ErrorController {
    implicit val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever
    override val authAction: AuthAction = FakeAuthAction
    override val minAuthAction: MinAuthAction = FakeMinAuthAction
  }

  "ErrorController" should {

    "Show No ATS page" in new TestErrorController {

      implicit lazy val request = AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest())

      val result = authorisedNoAts()(request)
      val document = contentAsString(result)

      status(result) shouldBe 200

      document shouldBe contentAsString(views.html.errors.no_ats_error())
    }

    "show not authorised page" in new TestErrorController {

      implicit lazy val request = AuthenticatedRequest("userId", None, None, None, None, None, None, FakeRequest())

      val result = notAuthorised()(request)
      val document = contentAsString(result)

      status(result) shouldBe 200

      document shouldBe contentAsString(views.html.errors.not_authorised())
    }

  }
}
