/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.mvc.Request
import play.api.{Configuration, Application}
import play.twirl.api.Html
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.frontend.bootstrap.DefaultFrontendGlobal
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import uk.gov.hmrc.play.frontend.filters.{ FrontendAuditFilter, FrontendLoggingFilter }


object ApplicationGlobal extends DefaultFrontendGlobal with RunMode {

  override lazy val auditConnector: AuditConnector = TAXSAuditConnector
  override lazy val loggingFilter: FrontendLoggingFilter = TAXSLoggingFilter
  override lazy val frontendAuditFilter: FrontendAuditFilter = TAXSAuditFilter

  override def onStart(app: Application) {
    super.onStart(app)
    ApplicationCrypto.verifyConfiguration()
  }

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html =
    views.html.errors.error_template(pageTitle, heading, message)


  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] =
    app.configuration.getConfig(s"microservice.metrics")
}
