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

package config

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.{Configuration, Environment}
import views.ViewSpecBase
import views.html.errors.{ErrorTemplateView, PageNotFoundTemplateView}

class ErrorHandlerSpec extends ViewSpecBase with MockitoSugar {

  lazy val errorHandler: ErrorHandler = new ErrorHandler(
    messagesApi,
    inject[Configuration],
    inject[Environment],
    inject[ErrorTemplateView],
    inject[PageNotFoundTemplateView]
  )

  implicit val request = FakeRequest()

  "notFoundTemplate" in {
    def notFoundView: Document = Jsoup.parse(errorHandler.notFoundTemplate(request).toString())

    notFoundView.getElementsByTag("h1").toString must include(messages("global.page.not.found.error.heading"))

    notFoundView.getElementsByTag("p").get(0).toString must include(
      messages("global.page.not.found.error.check.web.address.correct")
    )

    notFoundView.getElementsByTag("p").get(1).toString must include(
      messages("global.page.not.found.error.check.web.address.full")
    )

    notFoundView.getElementsByTag("p").get(2).toString must include(
      messages(
        "global.page.not.found.error.contact",
        "<a href=\"https://www.gov.uk/government/organisations/hm-revenue-customs/contact/self-assessment\" class=\"govuk-link\" target=\"_blank\" rel=\"noopener noreferrer\">" + messages(
          "global.page.not.found.error.contact.link.text"
        ) + "</a>"
      )
    )

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
          .toString()
      )

    standardErrorTemplateView.getElementsByTag("h1").toString must include(
      messages("global.error.InternalServerError500.title")
    )

    standardErrorTemplateView.getElementsByTag("p").get(0).toString must include(
      messages("global.error.InternalServerError500.message.you.can") + " " + messages(
        "global.error.InternalServerError500.message.contact.hmrc"
      ) + " " + messages("global.error.InternalServerError500.message.by.phone.post")
    )

  }
}
