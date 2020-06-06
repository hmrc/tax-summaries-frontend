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

import config.ApplicationConfig
import controllers.auth.PayeAuthenticatedRequest
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.test.FakeRequest
import services.atsData.PayeAtsTestData
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestConstants
import view_models.paye.PayeYourTaxableIncome

class PayeYourTaxableIncomeViewSpec extends UnitSpec with OneAppPerSuite with TestConstants {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val messages: Messages = Messages(Lang("en"), messagesApi)
  implicit val request = PayeAuthenticatedRequest(testNino, FakeRequest("GET", "/annual-tax-summary/paye/treasury-spending"))
  implicit val formPartialRetriever: FormPartialRetriever = app.injector.instanceOf[FormPartialRetriever]
  implicit lazy val appConfig = app.injector.instanceOf[ApplicationConfig]

  val payeYourTaxableIncomeViewModel : PayeYourTaxableIncome =PayeAtsTestData.payeYourTaxableIncomeViewModel

  def view(viewModel:PayeYourTaxableIncome): String =
    views.html.paye.paye_your_taxable_income(payeYourTaxableIncomeViewModel).body

  "PayeYourTaxableIncomeView" should {
    "return correct content for Taxable income section" in {

      val document = Jsoup.parse(view(payeYourTaxableIncomeViewModel))

      document.getElementById("self_employment_income").text() shouldBe "Self-employment £450.00"

      document.getElementById("income_from_employment").text() shouldBe "Employment £550.00"

      document.getElementById("state_pension").text() shouldBe "State pension £652.00"

      document.getElementById("taxable_state_benefits").text() shouldBe "Taxable state benefits £751.00"

      document.getElementById("other_income").text() shouldBe "Other income (including interest) £851.00"

      document.getElementById("benefits_from_employment").text() shouldBe "Benefits from employment £251.00"

      document.getElementById("total_income_before_tax").text() shouldBe "Your income before tax £351.00"

      document.getElementById("income-before-tax-foot").text() shouldBe "Your income before tax £1,000.00"

    }

    "not render taxable income table when they have no taxable income" in {

      val view = views.html.paye.paye_your_taxable_income(PayeAtsTestData.payeYourTaxableIncomeViewModel.copy(incomeTaxRows = List.empty)).body
      val document = Jsoup.parse(view)

      document.select("#income-tax-table") shouldBe empty

    }
  }
}