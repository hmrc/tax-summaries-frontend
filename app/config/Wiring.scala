/*
 * Copyright 2017 HM Revenue & Customs
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

import com.typesafe.config.Config
import play.api.Play
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.http.{HttpDelete, HttpGet, HttpPost, HttpPut}
import uk.gov.hmrc.play.http.ws.{WSDelete, WSGet, WSPost, WSPut}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, RunMode, ServicesConfig}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever
import uk.gov.hmrc.play.frontend.config.LoadAuditingConfig
import uk.gov.hmrc.play.frontend.filters.{FrontendAuditFilter, FrontendLoggingFilter, MicroserviceFilterSupport}

object TAXSControllerConfig extends ControllerConfig {
  lazy val controllerConfigs: Config = Play.current.configuration.underlying.getConfig("controllers")
}

object TAXSAuditConnector extends AuditConnector with AppName with RunMode {
  override lazy val auditingConfig: AuditingConfig = LoadAuditingConfig(s"microservice.services.auditing")
}

object TAXSAuthConnector extends AuthConnector with ServicesConfig {
  val serviceUrl: String = ApplicationConfig.authHost
  lazy val http = WSHttp
}

object TAXSAuditFilter extends FrontendAuditFilter with RunMode with AppName with MicroserviceFilterSupport {
  override def maskedFormFields: Seq[String] = Nil
  override def applicationPort: Option[Int] = None
  override def auditConnector: AuditConnector = TAXSAuditConnector
  override def controllerNeedsAuditing(controllerName: String): Boolean =
    TAXSControllerConfig.paramsForController(controllerName).needsAuditing
}

object TAXSLoggingFilter extends FrontendLoggingFilter with MicroserviceFilterSupport {
  override def controllerNeedsLogging(controllerName: String): Boolean =
    TAXSControllerConfig.paramsForController(controllerName).needsLogging
}

trait WSHttp extends WSGet with HttpGet with WSPut with HttpPut with WSPost with HttpPost with WSDelete with HttpDelete
object WSHttp extends WSHttp with AppName with RunMode {
  override val hooks: Seq[HttpHook] = NoneRequired
}

object WSGet extends WSGet with HttpGet with AppName with RunMode{
  override val hooks: Seq[HttpHook] = NoneRequired
}

object CachedStaticHtmlPartialProvider extends CachedStaticHtmlPartialRetriever {
 override def httpGet: CoreGet = WSGet
}

object TAXSSessionCache extends SessionCache with AppName {
  lazy val http = WSHttp
  override lazy val defaultSource: String = appName
  override lazy val baseUri: String = ApplicationConfig.sessionCacheHost
  override lazy val domain: String = ApplicationConfig.sessionCacheDomain
}
