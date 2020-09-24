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

package config

import com.google.inject.Inject
import play.api.i18n.{Lang, MessagesApi}
import play.api.mvc.Request
import play.api.{Application, Configuration, Environment}
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.http.FrontendErrorHandler
import uk.gov.hmrc.play.partials.FormPartialRetriever

class ErrorHandler @Inject()(
  val messagesApi: MessagesApi,
  val configuration: Configuration,
  val environment: Environment)(
  implicit val formPartialRetriever: FormPartialRetriever,
  implicit val appConfig: ApplicationConfig)
    extends FrontendErrorHandler {

  private def lang(implicit request: Request[_]): Lang =
    Lang(request.cookies.get("PLAY_LANG").map(_.value).getOrElse("en"))

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(
    implicit request: Request[_]): Html = {
    implicit val _: Lang = lang
    views.html.errors.error_template(pageTitle, heading, message)
  }

  override def notFoundTemplate(implicit request: Request[_]): Html = {
    implicit val _: Lang = lang
    views.html.errors.page_not_found_template()
  }

  def microserviceMetricsConfig(implicit app: Application): Option[Configuration] =
    app.configuration.getConfig(s"microservice.metrics")
}
