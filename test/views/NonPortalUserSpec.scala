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

    "not show the menu link in the header if on a mobile device" in {

      val spendData = new SpendData(amount, 20)
      val scottishIncomeTax = new Amount(0.00, "GBP")
      val fakeViewModel = new GovernmentSpend(
        2014,
        utr,
        List(
          ("welfare", spendData),
          ("health", spendData),
          ("education", spendData),
          ("pension", spendData),
          ("national_debt_interest", spendData),
          ("defence", spendData),
          ("criminal_justice", spendData),
          ("transport", spendData),
          ("business_and_industry", spendData),
          ("government_administration", spendData),
          ("culture", spendData),
          ("environment", spendData),
          ("housing_and_utilities", spendData),
          ("overseas_aid", spendData),
          ("uk_contribution_to_eu_budget", spendData),
          ("gov_spend_total", spendData)
        ),
        "",
        "",
        "",
        amount,
        "",
        scottishIncomeTax
      )
      val result =
        governmentSpendingView(fakeViewModel, (20.0, 20.0, 20.0))(request, messages, formPartialRetriever, appConfig)
      val document = Jsoup.parse(contentAsString(result))

      val menu_toggle = document.select(".js-header-toggle.menu")
      menu_toggle.text should not include "Menu"

      val href = menu_toggle.attr("href")
      href should not be "#proposition-links"
    }

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
      val result = taxsMainView(fakeViewModel)(request, messages, formPartialRetriever, appConfig)
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("wrapper").attr("data-journey") should include(
        "annual-tax-summary:transitioned-user:start")
    }
  }
}
