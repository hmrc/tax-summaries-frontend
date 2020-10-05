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

import controllers.auth.PayeAuthenticatedRequest
import org.jsoup.Jsoup
import play.api.test.FakeRequest
import services.atsData.PayeAtsTestData
import utils.TestConstants
import view_models.paye.PayeYourIncomeAndTaxes
import views.ViewSpecBase
import views.html.paye.PayeYourIncomeAndTaxesView

class PayeYourIncomeAndTaxesViewSpec extends ViewSpecBase with TestConstants {

  implicit val request = PayeAuthenticatedRequest(testNino, FakeRequest("GET", "/annual-tax-summary/paye/treasury-spending"))

  val payeYourIncomeAndTaxesViewModel : PayeYourIncomeAndTaxes =PayeAtsTestData.payeYourIncomeAndTaxesViewModel
lazy val payeYourIncomeAndTaxesView = inject[PayeYourIncomeAndTaxesView]

  def view(viewModel:PayeYourIncomeAndTaxes): String =
    payeYourIncomeAndTaxesView(payeYourIncomeAndTaxesViewModel).body

  "PayeYourIncomeAndTaxesView" should {
    "return correct content for Taxable income section" in {

      val document = Jsoup.parse(view(payeYourIncomeAndTaxesViewModel))

      document.getElementById("taxable-income").text() shouldBe "Taxable income £1,000.00 Your taxable income This is your total taxable income for the year."

    }

    "return correct content for Tax-free amount section" in {

      val document = Jsoup.parse(view(payeYourIncomeAndTaxesViewModel))

      document.getElementById("tax-free-amount").text() shouldBe "Tax-free amount £800.00 Your tax-free amount This is the amount you received without paying tax."

    }

    "return correct content for Income Tax and National Insurance contributions section" in {

      val document = Jsoup.parse(view(payeYourIncomeAndTaxesViewModel))

      document.getElementById("tax-calculated-as").text() shouldBe "Income Tax and National Insurance contributions £200.00 Your Income Tax and National Insurance contributions This is 20% of your taxable income. For every £1 of income, you paid 20p in Income Tax and National Insurance contributions. This does not reflect any refunds, repayments or accepted tax tolerances and therefore may be a different figure to the calculated tax shown in your breakdown."

    }

    "return correct content for Income after Tax and National Insurance contributions section" in {

      val document = Jsoup.parse(view(payeYourIncomeAndTaxesViewModel))

      document.getElementById("income_after_tax_and_nics").text() shouldBe "Income after Tax and National Insurance contributions £100.00"

    }
  }
}