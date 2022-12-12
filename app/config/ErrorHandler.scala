/*
 * Copyright 2022 HM Revenue & Customs
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

import com.google.inject.Inject
import play.api.i18n.{Lang, MessagesApi}
import play.api.mvc.Request
import play.api.{Application, Configuration, Environment}
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import views.html.errors.{ErrorTemplateView, PageNotFoundTemplateView}

import scala.concurrent.ExecutionContext

class ErrorHandler @Inject() (
  val messagesApi: MessagesApi,
  val configuration: Configuration,
  errorTemplateView: ErrorTemplateView,
  pageNotFoundTemplateView: PageNotFoundTemplateView
)(implicit val appConfig: ApplicationConfig)
    extends FrontendErrorHandler {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit
    request: Request[_]
  ): Html =
    errorTemplateView(pageTitle, heading, message)

  override def notFoundTemplate(implicit request: Request[_]): Html =
    pageNotFoundTemplateView()
}
