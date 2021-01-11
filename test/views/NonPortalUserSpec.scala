/*
 * Copyright 2021 HM Revenue & Customs
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

package views

import controllers.auth.AuthenticatedRequest
import models.SpendData
import org.jsoup.Jsoup
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import uk.gov.hmrc.domain.SaUtr
import utils.TestConstants._
import view_models._
import views.html.{GovernmentSpendingView, TaxsMainView}

class NonPortalUserSpec extends ViewSpecBase with MockitoSugar {

  val request = AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest())
  val utr = testUtr
  val amount = new Amount(0.00, "GBP")
  val rate = new Rate("5")
  lazy val taxsMainView = inject[TaxsMainView]
  lazy val governmentSpendingView = inject[GovernmentSpendingView]

  "Logging in as a transitioned user" should {

    "contain GA event attribute on the landing page" in {

      val fakeViewModel = Summary(
        2014,
        utr,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        rate,
        rate,
        "",
        "",
        "")
      val result = taxsMainView(fakeViewModel)(request, messages, formPartialRetriever, templateRenderer, appConfig)
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("wrapper").attr("data-journey") should include(
        "annual-tax-summary:transitioned-user:start")
    }
  }
}
