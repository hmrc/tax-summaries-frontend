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

package config

import controllers.paye.AppConfigBaseSpec
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import views.ViewSpecBase
import play.api.test.Helpers._

class ErrorHandlerSpec extends MockitoSugar with ViewSpecBase {

  implicit lazy val errorHandler = inject[ErrorHandler]

  implicit val request = FakeRequest()

  "notFoundTemplate" in {
    def notFoundView: Document = Jsoup.parse(errorHandler.notFoundTemplate(request).toString())

    notFoundView.getElementsByTag("h1").toString should include(messages("global.page.not.found.error.heading"))

    notFoundView.getElementsByTag("p").get(2).toString should include(
      messages("global.page.not.found.error.check.web.address.correct"))

    notFoundView.getElementsByTag("p").get(3).toString should include(
      messages("global.page.not.found.error.check.web.address.full"))

    notFoundView.getElementsByTag("p").get(4).toString should include(
      messages(
        "global.page.not.found.error.contact",
        "<a href=\"https://www.gov.uk/government/organisations/hm-revenue-customs/contact/self-assessment\" target=\"_blank\">" + messages(
          "global.page.not.found.error.contact.link.text") + "</a>"
      ))

  }

  "standardErrorTemplate" in {
    def standardErrorTemplateView: Document =
      Jsoup.parse(
        errorHandler
          .standardErrorTemplate(
            "Sorry, the service is unavailable",
            "Sorry, the service is unavailable",
            "You can use this service later or you can contact HMRC online, by phone or by post."
          )
          .toString())

    standardErrorTemplateView.getElementsByTag("h1").toString should include(
      messages("global.error.InternalServerError500.title"))

    standardErrorTemplateView.getElementsByTag("p").get(2).toString should include(
      messages("global.error.InternalServerError500.message.you.can") + " " + messages(
        "global.error.InternalServerError500.message.contact.hmrc") + " " + messages(
        "global.error.InternalServerError500.message.by.phone.post"))

  }
}
