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

package views.paye

import config.AppFormPartialRetriever
import controllers.auth.PayeAuthenticatedRequest
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.test.FakeRequest
import services.atsData.PayeAtsTestData
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestConstants
import view_models.paye.PayeGovernmentSpend

class PayeGovernmentSpendViewSpec extends UnitSpec with OneAppPerSuite with TestConstants {

  implicit val messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]
  implicit val messages: Messages = Messages(Lang("en"), messagesApi)
  implicit val request = PayeAuthenticatedRequest("1234567890", testNino, FakeRequest("GET", "/annual-tax-summary/paye/treasury-spending"))
  implicit val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever

  "view" should {
    "have correct data and heading for given taxYear" in {

      val view = views.html.paye.paye_government_spending(PayeAtsTestData.payeGovernmentSpendViewModel).body
      val document = Jsoup.parse(view)

      document.getElementById("Welfare").text() shouldBe "Welfare (23.5%)"
      document.select("#Welfare + dd").text() shouldBe "£451"

      document.getElementById("Health").text() shouldBe "Health (20.2%)"
      document.select("#Health + dd").text() shouldBe "£388"

      document.getElementById("StatePensions").text() shouldBe "State Pensions (12.8%)"
      document.select("#StatePensions + dd").text() shouldBe "£246"

      document.getElementById("Education").text() shouldBe "Education (11.8%)"
      document.select("#Education + dd").text() shouldBe "£226"

      document.getElementById("Defence").text() shouldBe "Defence (5.3%)"
      document.select("#Defence + dd").text() shouldBe "£102"

      document.getElementById("NationalDebtInterest").text() shouldBe "National debt interest (5.1%)"
      document.select("#NationalDebtInterest + dd").text() shouldBe "£98"

      document.getElementById("Transport").text() shouldBe "Transport (4.3%)"
      document.select("#Transport + dd").text() shouldBe "£83"

      document.getElementById("PublicOrderAndSafety").text() shouldBe "Public order and safety (4.3%)"
      document.select("#PublicOrderAndSafety + dd").text() shouldBe "£83"

      document.getElementById("BusinessAndIndustry").text() shouldBe "Business and industry (3.6%)"
      document.select("#BusinessAndIndustry + dd").text() shouldBe "£69"

      document.getElementById("GovernmentAdministration").text() shouldBe "Government administration (2.1%)"
      document.select("#GovernmentAdministration + dd").text() shouldBe "£40"

      document.getElementById("HousingAndUtilities").text() shouldBe "Housing and utilities, like street lighting (1.6%)"
      document.select("#HousingAndUtilities + dd").text() shouldBe "£31"

      document.getElementById("Environment").text() shouldBe "Environment (1.5%)"
      document.select("#Environment + dd").text() shouldBe "£29"

      document.getElementById("Culture").text() shouldBe "Culture, like sports, libraries and museums (1.5%)"
      document.select("#Culture + dd").text() shouldBe "£29"

      document.getElementById("OverseasAid").text() shouldBe "Overseas aid (1.2%)"
      document.select("#OverseasAid + dd").text() shouldBe "£23"

      document.getElementById("UkContributionToEuBudget").text() shouldBe "UK contribution to the EU budget (1%)"
      document.select("#UkContributionToEuBudget + dd").text() shouldBe "£19"

      document.select("#TotalAmount + dd").text() shouldBe "£200"

      document
        .select("h1")
        .text shouldBe "How your tax was spent"
      document
        .select("h2.heading-xlarge")
        .text shouldBe "6 April 2018 to 5 April 2019"
    }

    "link to Scottish government spending page for Scottish users" in {
      val view = views.html.paye.paye_government_spending(PayeAtsTestData.payeGovernmentSpendViewModel.copy(isScottish = true)).body
      val document = Jsoup.parse(view)

      document.select("#scottish-spending-link a").attr("href") shouldBe "https://www.gov.scot/publications/scottish-income-tax-2019-2020/"
    }

    "not link to Scottish government spending page for non-Scottish users" in {
      val view = views.html.paye.paye_government_spending(PayeAtsTestData.payeGovernmentSpendViewModel.copy(isScottish = false)).body
      val document = Jsoup.parse(view)

      document.select("#scottish-spending-link") shouldBe empty
    }
  }
}
