/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.Html
import services.atsData.PayeAtsTestData
import utils.TestConstants
import view_models.paye.PayeYourTaxableIncome
import views.ViewSpecBase
import views.behaviours.ViewBehaviours
import views.html.paye.PayeYourTaxableIncomeView

class PayeYourTaxableIncomeViewSpec extends ViewSpecBase with TestConstants with ViewBehaviours {

  implicit val request: PayeAuthenticatedRequest[AnyContentAsEmpty.type] =
    PayeAuthenticatedRequest(
      testNino,
      isSa = false,
      fakeCredentials,
      FakeRequest("GET", "/annual-tax-summary/paye/treasury-spending")
    )

  val payeAtsTestData: PayeAtsTestData                          = inject[PayeAtsTestData]
  val payeYourTaxableIncomeViewModel: PayeYourTaxableIncome     = payeAtsTestData.payeYourTaxableIncomeViewModel
  lazy val payeYourTaxableIncomeView: PayeYourTaxableIncomeView = inject[PayeYourTaxableIncomeView]

  def createView: () => Html =
    () =>
      payeYourTaxableIncomeView(
        payeYourTaxableIncomeViewModel
      )(messages, request)

  def view(viewModel: PayeYourTaxableIncome): String =
    payeYourTaxableIncomeView(payeYourTaxableIncomeViewModel).body

  "PayeYourTaxableIncomeView when rendered" must {
    behave like pageWithBackLink(createView)
  }

  "PayeYourTaxableIncomeView" must {
    "return correct content for Taxable income section" in {

      val document = Jsoup.parse(view(payeYourTaxableIncomeViewModel))

      document.getElementById("self_employment_income").text() mustBe "Self-employment £450.00"

      document.getElementById("income_from_employment").text() mustBe "Employment £550.00"

      document.getElementById("state_pension").text() mustBe "State pension £652.00"

      document.getElementById("taxable_state_benefits").text() mustBe "Taxable state benefits £751.00"

      document.getElementById("other_income").text() mustBe "Other income (including interest) £851.00"

      document.getElementById("benefits_from_employment").text() mustBe "Benefits from employment £251.00"

      document.getElementById("total_income_before_tax").text() mustBe "Your income before tax £351.00"

      document.getElementById("income-before-tax-foot").text() mustBe "Your income before tax £1,000.00"

      document
        .getElementById("income-before-tax-intro")
        .text() mustBe "We have calculated this using information given to us by you and other sources. This includes your employer, pension providers, and banks or building societies."

    }

    "not render taxable income table when they have no taxable income" in {

      val view     =
        payeYourTaxableIncomeView(payeAtsTestData.payeYourTaxableIncomeViewModel.copy(incomeTaxRows = List.empty)).body
      val document = Jsoup.parse(view)

      document.select("#income-tax-table") mustBe empty

    }
  }
}
