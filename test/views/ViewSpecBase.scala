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

import controllers.auth.AuthenticatedRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.Assertion
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.ConfidenceLevel.L250
import uk.gov.hmrc.http.HeaderNames
import utils.BaseSpec
import utils.TestConstants.fakeCredentials

trait ViewSpecBase extends BaseSpec {

  implicit val messagesApi: MessagesApi = inject[MessagesApi]
  implicit val messages: Messages       = MessagesImpl(Lang("en"), messagesApi)
  implicit val lang: Lang               = messages.lang

  def asDocument(html: Html): Document = Jsoup.parse(html.toString())

  def assertRenderedByClass(doc: Document, className: String): Assertion =
    assert(doc.getElementsByClass(className) != null, "Element " + className + " was not rendered on the page.")

  lazy val fakeRequest              = FakeRequest("", "").withHeaders(HeaderNames.authorisation -> "Bearer 1")
  lazy val authenticatedFakeRequest = AuthenticatedRequest(
    "",
    None,
    None,
    None,
    isSa = true,
    isAgentActive = true,
    L250,
    fakeCredentials,
    fakeRequest
  )

}
