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

package views

import controllers.auth.AuthenticatedRequest
import org.jsoup.Jsoup
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.{Lang, MessagesImpl}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.SaUtr
import utils.TestConstants._
import view_models.{Amount, Rate}

class GenericErrorViewSpec extends ViewSpecBase with MockitoSugar  {

  lazy val requestWithSession = AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest().withSession("TAXS_USER_TYPE" -> "PORTAL"))
  val languageEn = Lang("en")
  val languageCy = Lang("cy")
  val utr = testUtr
  val amount = new Amount(0.00, "GBP")
  val rate = new Rate("5")

  implicit val messagesEn = MessagesImpl(languageEn, messagesApi)
  implicit val messagesCy = MessagesImpl(languageCy, messagesApi)

  "Logging in as a portal user" should {

    "show the correct contents of the generic error page in English" in  {

      val resultEn = views.html.errors.generic_error()(languageEn, requestWithSession, messagesEn, formPartialRetriever, appConfig)
      val documentEn = Jsoup.parse(contentAsString(resultEn))
      documentEn.toString should include("Sorry, there is a problem with the service")
      documentEn.toString should include("Try again later.")
    }

    "show the correct contents of the generic error page in Welsh" in  {

      val resultCy = views.html.errors.generic_error()(languageCy, requestWithSession, messagesCy, formPartialRetriever, appConfig)
      val documentCy = Jsoup.parse(contentAsString(resultCy))
      documentCy.toString should include("Mae’n ddrwg gennym, mae problem gyda’r gwasanaeth")
      documentCy.toString should include("Rhowch gynnig arall arni yn nes ymlaen.")
    }
  }
}
