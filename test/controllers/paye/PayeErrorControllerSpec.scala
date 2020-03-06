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
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestConstants.testNino

class PayeErrorControllerSpec  extends UnitSpec with GuiceOneAppPerTest with I18nSupport {

  override def messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]
  val fakeAuthenticatedRequest = PayeAuthenticatedRequest(testNino, FakeRequest("GET", "/annual-tax-summary/paye/no-ats"))

  class TestController extends PayeErrorController {
    override val payeAuthAction: PayeAuthAction = FakePayeAuthAction
  }

  "Show NO ATS page and return NOT_FOUND" in new TestController{
    val result = authorisedNoAts(fakeAuthenticatedRequest)
    val document = Jsoup.parse(contentAsString(result))

    status(result) shouldBe 404
    document.title should include(Messages("paye.ats.no_ats.title"))
  }
}