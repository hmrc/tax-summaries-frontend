/*
 * Copyright 2017 HM Revenue & Customs
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

import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import play.api.test.FakeRequest
import play.api.i18n.{MessagesApi, Lang, Messages}
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import uk.gov.hmrc.play.test.UnitSpec
import utils.AuthorityUtils
import view_models.{Amount, Rate}
import org.scalatestplus.play.{HtmlUnitFactory, OneBrowserPerSuite, OneServerPerSuite, PlaySpec}
import utils.TestConstants._

class GenericErrorViewTest extends UnitSpec with OneServerPerSuite with OneBrowserPerSuite with HtmlUnitFactory with MockitoSugar  {

  val request = FakeRequest()
  val language = Lang("en")
  val utr = testUtr
  val user = User(AuthorityUtils.saAuthority(testOid, utr))
  val amount = new Amount(0.00, "GBP")
  val rate = new Rate("5")

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val messages = Messages(language, messagesApi)

  "Logging in as a portal user" should {

    "show the correct contents of the generic error page" in  {

      val result = views.html.errors.generic_error()(language, request.withSession("TAXS_USER_TYPE" -> "PORTAL"), user, messages)
      val document = Jsoup.parse(contentAsString(result))

      document.select("#proposition-links a").text should include("Back to HMRC Online Services")
      val href = document.select("#proposition-links a").first().attr("href")
      href should be("https://online.hmrc.gov.uk/self-assessment/ind/" + utr)

      document.toString should not include "0345 123 4567"
      document.toString should not include "no.ats.error.list.item1"
      document.toString should not include "no.ats.error.list.lede"
      document.toString should not include "taxsummaries@hmrc.gsi.gov.uk"

      document.select("#global-breadcrumb li:nth-child(1) a").toString should include("<a href=\"/annual-tax-summary\">")
      document.select("#global-breadcrumb li:nth-child(1) a").text shouldBe "Select the tax year"
      document.select("#global-breadcrumb li:nth-child(2)").text shouldBe "Technical Difficulties"
    }
  }
}
