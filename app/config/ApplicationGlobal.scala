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

import play.api.i18n.Lang
import play.api.mvc.Request
import play.api.{Application, Configuration, Play}
import play.twirl.api.Html
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.bootstrap.DefaultFrontendGlobal
import uk.gov.hmrc.play.frontend.filters.{FrontendAuditFilter, FrontendLoggingFilter}
import uk.gov.hmrc.play.partials.FormPartialRetriever

object ApplicationGlobal extends DefaultFrontendGlobal {

  private def lang(implicit request: Request[_]): Lang =
    Lang(request.cookies.get("PLAY_LANG").map(_.value).getOrElse("en"))

  lazy val controllerConfig = new TAXSControllerConfig(configuration)

  override lazy val auditConnector: AuditConnector = new TAXSAuditConnector(configuration)
  override lazy val loggingFilter: FrontendLoggingFilter = new TAXSLoggingFilter(controllerConfig)
  override lazy val frontendAuditFilter: FrontendAuditFilter = new TAXSAuditFilter(auditConnector, configuration, controllerConfig)

  implicit lazy val formPartialRetriever: FormPartialRetriever = new AppFormPartialRetriever(new TAXSSessionCookieCrypto, new AppWSGet(configuration))

  override def onStart(app: Application) {
    super.onStart(app)
    new ApplicationCrypto(Play.current.configuration.underlying).verifyConfiguration()
  }

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(
    implicit request: Request[_]): Html =
    views.html.errors.error_template(pageTitle, heading, message)

  override def notFoundTemplate(implicit request: Request[_]): Html = {
    implicit val _: Lang = lang
    views.html.errors.page_not_found_template()
  }

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] =
    app.configuration.getConfig(s"microservice.metrics")
}
