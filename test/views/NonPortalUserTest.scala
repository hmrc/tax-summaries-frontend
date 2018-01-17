/*
 * Copyright 2018 HM Revenue & Customs
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

import models.SpendData
import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import play.api.i18n.{Messages, Lang}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.api.test.FakeRequest
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import uk.gov.hmrc.play.test.UnitSpec
import utils.AuthorityUtils
import view_models._
import controllers.FakeTaxsPlayApplication
import utils.TestConstants._

class NonPortalUserTest extends UnitSpec with FakeTaxsPlayApplication with MockitoSugar {

  val messages: Messages = mock[Messages]
  val request = FakeRequest()
  val language = Lang("en")
  val utr = testUtr
  val user = User(AuthorityUtils.saAuthority(testOid, utr))
  val amount = new Amount(0.00, "GBP")
  val rate = new Rate("5")

  "Logging in as a transitioned user" should {
    
    "not show the menu link in the header if on a mobile device" in  {

      val spendData = new SpendData(amount, 20)
      val scottishIncomeTax = new Amount(0.00, "GBP")
      val fakeViewModel = new GovernmentSpend(2014, utr, List(("welfare", spendData), ("health", spendData),
        ("education", spendData), ("pension", spendData), ("national_debt_interest", spendData), ("defence", spendData),
        ("criminal_justice", spendData), ("transport", spendData), ("business_and_industry", spendData),
        ("government_administration", spendData), ("culture", spendData), ("environment", spendData),
        ("housing_and_utilities", spendData), ("overseas_aid", spendData), ("uk_contribution_to_eu_budget", spendData),
        ("gov_spend_total", spendData)), "", "", "", amount, "", scottishIncomeTax)
      val result = views.html.government_spending(fakeViewModel)(language, request, messages)
      val document = Jsoup.parse(contentAsString(result))

      val menu_toggle = document.select(".js-header-toggle.menu")
          menu_toggle.text should not include "Menu"

      val href = menu_toggle.attr("href")
          href should not be "#proposition-links"
    }

    "contain GA event attribute on the landing page" in  {

      val fakeViewModel = Summary(2014, utr, amount, amount, amount, amount, amount, amount,
        amount, amount, amount, amount, amount, rate, rate, "", "", "")
      val result = views.html.taxs_main(fakeViewModel)(request, messages, language)
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("wrapper").attr("data-journey") should include("annual-tax-summary:transitioned-user:start")
    }
  }
}
