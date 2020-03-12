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

class PayeIncomeTaxAndNicsViewSpec extends UnitSpec with OneAppPerSuite with TestConstants {

  implicit val messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]
  implicit val messages: Messages = Messages(Lang("en"), messagesApi)
  implicit val request = PayeAuthenticatedRequest(testNino, FakeRequest("GET", "/annual-tax-summary/paye/total-income-tax"))
  implicit val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever


  "PayeIncomeTaxAndNicsView" should {

    "have correct data for scottish and rUK tax payer" in {


      val view = views.html.paye.paye_income_tax_and_nics(PayeAtsTestData.payeIncomeTaxAndNicsViewModel).body
      val document = Jsoup.parse(view)

      document.getElementById("scottish_starter_rate").text() shouldBe "Starter rate (£2,000 at 19%) £380.00"

      document.getElementById("scottish_basic_rate").text() shouldBe "Basic rate (£10,150 at 20%) £2,030.00"

      document.getElementById("scottish_intermediate_rate").text() shouldBe "Intermediate rate (£19,430 at 21%) £4,080.00"

      document.getElementById("scottish_higher_rate").text() shouldBe "Higher rate (£31,570 at 41%) £12,943.00"

      document.getElementById("totalScottishIncomeTax").text() shouldBe "Total £19,433.00"

      document.getElementById("ordinary_rate").text() shouldBe "Basic rate Dividend Tax (£19,430 at 19%) £4,080.00"

      document.getElementById("higher_rate_income_tax").text() shouldBe "Higher rate Income Tax (£10,150 at 20%) £2,030.00"

      document.getElementById("basic_rate_income_tax").text() shouldBe "Basic rate Income Tax (£2,000 at 21%) £380.00"

      document.getElementById("upper_rate").text() shouldBe "Higher rate Dividend Tax (£31,570 at 41%) £12,943.00"

      document.getElementById("totalUkIncomeTax").text() shouldBe "Total £18,433.00"

    }
  }
}