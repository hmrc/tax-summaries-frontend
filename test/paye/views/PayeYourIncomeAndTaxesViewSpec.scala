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

package paye.views

import common.models.requests
import common.models.requests.PayeAuthenticatedRequest
import common.services.atsData.PayeAtsTestData
import common.utils.TestConstants
import common.view_models.paye.PayeYourIncomeAndTaxes
import common.views.ViewSpecBase
import common.views.behaviours.ViewBehaviours
import paye.views.html.PayeYourIncomeAndTaxesView
import org.jsoup.Jsoup
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.Html

class PayeYourIncomeAndTaxesViewSpec extends ViewSpecBase with TestConstants with ViewBehaviours {

  implicit val request: PayeAuthenticatedRequest[AnyContentAsEmpty.type] =
    requests.PayeAuthenticatedRequest(
      testNino,
      fakeCredentials,
      FakeRequest("GET", "/annual-tax-summary/paye/treasury-spending")
    )

  val payeAtsTestData: PayeAtsTestData                            = inject[PayeAtsTestData]
  lazy val payeYourIncomeAndTaxesView: PayeYourIncomeAndTaxesView = inject[PayeYourIncomeAndTaxesView]
  val payeYourIncomeAndTaxesViewModel: PayeYourIncomeAndTaxes     = payeAtsTestData.payeYourIncomeAndTaxesViewModel

  def createView: () => Html =
    () =>
      payeYourIncomeAndTaxesView(
        payeYourIncomeAndTaxesViewModel
      )(messages, request)

  def view: String =
    payeYourIncomeAndTaxesView(payeYourIncomeAndTaxesViewModel).body

  "PayeYourIncomeAndTaxesView when rendered" must {
    behave like pageWithBackLink(createView)
  }

  "PayeYourIncomeAndTaxesView" must {
    "return correct content for Taxable income section" in {

      val document = Jsoup.parse(view)

      document
        .getElementById("taxable-income")
        .text() mustBe "Taxable income £1,000.00 Your taxable income This is your total taxable income for the year."

    }

    "return correct content for Tax-free amount section" in {

      val document = Jsoup.parse(view)

      document
        .getElementById("tax-free-income")
        .text() mustBe "Tax-free income £800.00 Your tax-free income This is the amount you received without paying tax."

    }

    "return correct content for Income Tax and National Insurance contributions section" in {

      val document = Jsoup.parse(view)

      document
        .getElementById("tax-calculated-as")
        .text() mustBe "Income Tax and National Insurance contributions £200.00 Your Income Tax and National Insurance contributions This is 20% of your taxable income. For every £1 of income, you paid 20p in Income Tax and National Insurance contributions."

    }

    "return correct content for Income after Tax and National Insurance contributions section" in {

      val document = Jsoup.parse(view)

      document
        .getElementById("income_after_tax_and_nics")
        .text() mustBe "Income after Tax and National Insurance contributions £100.00"

    }
  }
}
