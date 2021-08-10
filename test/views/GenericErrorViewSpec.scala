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

package views

import controllers.auth.AuthenticatedRequest
import org.jsoup.Jsoup
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.{Lang, MessagesImpl}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.SaUtr
import utils.TestConstants
import view_models.{Amount, Rate}
import views.html.errors.GenericErrorView

class GenericErrorViewSpec extends ViewSpecBase with MockitoSugar with TestConstants {

  lazy val requestWithSession = AuthenticatedRequest(
    "userId",
    None,
    Some(SaUtr(testUtr)),
    None,
    true,
    ConfidenceLevel.L50,
    fakeCredentials,
    FakeRequest().withSession("TAXS_USER_TYPE" -> "PORTAL"))

  val languageEn = Lang("en")
  val languageCy = Lang("cy")
  val utr = testUtr
  val amount = new Amount(0.00, "GBP")
  val rate = new Rate("5")

  implicit val messagesEn = MessagesImpl(languageEn, messagesApi)
  implicit val messagesCy = MessagesImpl(languageCy, messagesApi)

  lazy val genericErrorView = inject[GenericErrorView]

  "Logging in as a portal user" must {

    "show the correct contents of the generic error page in English" in {

      val resultEn =
        genericErrorView()(requestWithSession, messagesEn, templateRenderer, appConfig, ec)
      val documentEn = Jsoup.parse(contentAsString(resultEn))
      documentEn.toString must include(messagesEn("global.error.InternalServerError500.title"))
      documentEn.toString must include(
        messagesEn("global.error.InternalServerError500.message.you.can") + " <a href=\"https://www.gov.uk/government/organisations/hm-revenue-customs/contact/self-assessment\">" + messagesEn(
          "global.error.InternalServerError500.message.contact.hmrc") + "</a> " + messagesEn(
          "global.error.InternalServerError500.message.by.phone.post"))
    }

    "show the correct contents of the generic error page in Welsh" in {

      val resultCy =
        genericErrorView()(requestWithSession, messagesCy, templateRenderer, appConfig, ec)
      val documentCy = Jsoup.parse(contentAsString(resultCy))
      documentCy.toString must include(messagesCy("global.error.InternalServerError500.title"))
      documentCy.toString must include(
        messagesCy("global.error.InternalServerError500.message.you.can") + " <a href=\"https://www.gov.uk/government/organisations/hm-revenue-customs/contact/self-assessment\">" + messagesCy(
          "global.error.InternalServerError500.message.contact.hmrc") + "</a> " + messagesCy(
          "global.error.InternalServerError500.message.by.phone.post"))
    }
  }
}
