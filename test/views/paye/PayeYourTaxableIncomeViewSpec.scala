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

package views.paye

import controllers.auth.PayeAuthenticatedRequest
import org.jsoup.Jsoup
import play.api.test.FakeRequest
import services.atsData.PayeAtsTestData
import utils.TestConstants
import view_models.paye.PayeYourTaxableIncome
import views.ViewSpecBase
import views.html.paye.PayeYourTaxableIncomeView

class PayeYourTaxableIncomeViewSpec extends TestConstants with ViewSpecBase {

  implicit val request =
    PayeAuthenticatedRequest(testNino, FakeRequest("GET", "/annual-tax-summary/paye/treasury-spending"))

  val payeYourTaxableIncomeViewModel: PayeYourTaxableIncome = PayeAtsTestData.payeYourTaxableIncomeViewModel

  lazy val payeYourTaxableIncomeView = inject[PayeYourTaxableIncomeView]

  def view(viewModel: PayeYourTaxableIncome): String =
    payeYourTaxableIncomeView(payeYourTaxableIncomeViewModel).body

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

      document
        .getElementById("income-before-tax-intro")
        .text() shouldBe "We have calculated this using information held at the time supplied to us by you, your employers, or other sources such as pension providers and banks or building societies."

      document
        .getElementById("income-before-tax-description")
        .text() shouldBe "This information comes from you, your employer(s) or your pension provider(s)."

    }

    "not render taxable income table when they have no taxable income" in {

      val view =
        payeYourTaxableIncomeView(PayeAtsTestData.payeYourTaxableIncomeViewModel.copy(incomeTaxRows = List.empty)).body
      val document = Jsoup.parse(view)

      document.select("#income-tax-table") shouldBe empty

    }
  }
}
