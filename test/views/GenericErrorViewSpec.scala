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

package views

import models.requests
import models.requests
import models.requests.AuthenticatedRequest
import org.jsoup.Jsoup
import play.api.i18n.{Lang, MessagesImpl}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.SaUtr
import utils.TestConstants
import view_models.{Amount, Rate}
import views.html.errors.GenericErrorView

class GenericErrorViewSpec extends ViewSpecBase with TestConstants {

  lazy val requestWithSession: AuthenticatedRequest[AnyContentAsEmpty.type] = requests.AuthenticatedRequest(
    "userId",
    None,
    Some(SaUtr(testUtr)),
    None,
    isAgentActive = false,
    ConfidenceLevel.L50,
    fakeCredentials,
    FakeRequest().withSession("TAXS_USER_TYPE" -> "PORTAL")
  )

  val languageEn: Lang = Lang("en")
  val languageCy: Lang = Lang("cy")
  val utr: String      = testUtr
  val amount: Amount   = new Amount(0.00, "GBP")
  val rate: Rate       = new Rate("5")

  implicit val messagesEn: MessagesImpl = MessagesImpl(languageEn, messagesApi)
  implicit val messagesCy: MessagesImpl = MessagesImpl(languageCy, messagesApi)

  lazy val genericErrorView: GenericErrorView = inject[GenericErrorView]

  "Logging in as a portal user" must {

    "show the correct contents of the generic error page in English" in {

      val resultEn   =
        genericErrorView()(requestWithSession, messagesEn, appConfig)
      val documentEn = Jsoup.parse(contentAsString(resultEn))
      documentEn.toString must include(messagesEn("global.error.InternalServerError500.title"))
      documentEn.toString must include(
        messagesEn(
          "global.error.InternalServerError500.message.you.can"
        ) + " <a href=\"https://www.gov.uk/government/organisations/hm-revenue-customs/contact/self-assessment\" class=\"govuk-link\">" + messagesEn(
          "global.error.InternalServerError500.message.contact.hmrc"
        ) + "</a> " + messagesEn("global.error.InternalServerError500.message.by.phone.post")
      )
    }

    "show the correct contents of the generic error page in Welsh" in {

      val resultCy   =
        genericErrorView()(requestWithSession, messagesCy, appConfig)
      val documentCy = Jsoup.parse(contentAsString(resultCy))
      documentCy.toString must include(messagesCy("global.error.InternalServerError500.title"))
      documentCy.toString must include(
        messagesCy(
          "global.error.InternalServerError500.message.you.can"
        ) + " <a href=\"https://www.gov.uk/government/organisations/hm-revenue-customs/contact/self-assessment\" class=\"govuk-link\">" + messagesCy(
          "global.error.InternalServerError500.message.contact.hmrc"
        ) + "</a> " + messagesCy("global.error.InternalServerError500.message.by.phone.post")
      )
    }
  }
}
