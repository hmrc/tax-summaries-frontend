/*
 * Copyright 2019 HM Revenue & Customs
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

import config.AppFormPartialRetriever
import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import play.api.test.FakeRequest
import play.api.i18n.{Lang, Messages, MessagesApi}
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import uk.gov.hmrc.play.test.UnitSpec
import utils.AuthorityUtils
import view_models.{Amount, Rate}
import org.scalatestplus.play.{HtmlUnitFactory, OneBrowserPerSuite, OneServerPerSuite, PlaySpec}
import utils.TestConstants._
import uk.gov.hmrc.play.partials.FormPartialRetriever

class GenericErrorViewTest extends UnitSpec with OneServerPerSuite with OneBrowserPerSuite with HtmlUnitFactory with MockitoSugar  {

  val request = FakeRequest()
  val languageEn = Lang("en")
  val languageCy = Lang("cy")
  val utr = testUtr
  val user = User(AuthorityUtils.saAuthority(testOid, utr))
  val amount = new Amount(0.00, "GBP")
  val rate = new Rate("5")

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val messagesEn = Messages(languageEn, messagesApi)
  implicit val messagesCy = Messages(languageCy, messagesApi)
  implicit lazy val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever

  "Logging in as a portal user" should {

    "show the correct contents of the generic error page in English" in  {

      val resultEn = views.html.errors.generic_error()(languageEn, request.withSession("TAXS_USER_TYPE" -> "PORTAL"), user, messagesEn, formPartialRetriever)
      val documentEn = Jsoup.parse(contentAsString(resultEn))
      documentEn.toString should include("Sorry, there is a problem with the service")
      documentEn.toString should include("Try again later.")
    }

    "show the correct contents of the generic error page in Welsh" in  {

      val resultCy = views.html.errors.generic_error()(languageCy, request.withSession("TAXS_USER_TYPE" -> "PORTAL"), user, messagesCy, formPartialRetriever)
      val documentCy = Jsoup.parse(contentAsString(resultCy))
      documentCy.toString should include("Mae’n ddrwg gennym, mae problem gyda’r gwasanaeth")
      documentCy.toString should include("Rhowch gynnig arall arni yn nes ymlaen.")
    }
  }
}
