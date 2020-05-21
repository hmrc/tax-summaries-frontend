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

import akka.actor.ActorSystem
import com.google.inject.Inject
import com.typesafe.config.Config
import play.api.{Configuration, Play}
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.http.{HttpDelete, HttpGet, HttpPost, HttpPut}
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AppName, ControllerConfig}
import uk.gov.hmrc.play.frontend.config.LoadAuditingConfig
import uk.gov.hmrc.play.frontend.filters.{FrontendAuditFilter, FrontendLoggingFilter, MicroserviceFilterSupport}
import uk.gov.hmrc.play.http.ws.{WSDelete, WSGet, WSPost, WSPut}

class TAXSControllerConfig @Inject()(configuration: Configuration) extends ControllerConfig {
  lazy val controllerConfigs: Config = configuration.underlying.getConfig("controllers")
}

class TAXSAuditConnector @Inject()(configuration: Configuration) extends AuditConnector with AppName {
  override lazy val auditingConfig: AuditingConfig = LoadAuditingConfig(s"microservice.services.auditing")

  override protected def appNameConfiguration: Configuration = configuration
}

class TAXSAuditFilter @Inject()(
  override val auditConnector: AuditConnector,
  configuration: Configuration,
  controllerConfig: TAXSControllerConfig)
    extends FrontendAuditFilter with AppName with MicroserviceFilterSupport {
  override def maskedFormFields: Seq[String] = Nil
  override def applicationPort: Option[Int] = None
  override def controllerNeedsAuditing(controllerName: String): Boolean =
    controllerConfig.paramsForController(controllerName).needsAuditing

  override protected def appNameConfiguration: Configuration = configuration
}

class TAXSLoggingFilter @Inject()(controllerConfig: TAXSControllerConfig)
    extends FrontendLoggingFilter with MicroserviceFilterSupport {
  override def controllerNeedsLogging(controllerName: String): Boolean =
    controllerConfig.paramsForController(controllerName).needsLogging
}

class WSHttp @Inject()(config: Configuration)
    extends WSGet with HttpGet with WSPut with HttpPut with WSPost with HttpPost with WSDelete with HttpDelete
    with AppName {
  override val hooks: Seq[HttpHook] = NoneRequired

  protected def appNameConfiguration: Configuration = config

  override protected def actorSystem: ActorSystem = Play.current.actorSystem

  override protected def configuration: Option[Config] = Some(config.underlying)
}

class AppWSGet @Inject()(config: Configuration) extends WSGet with HttpGet with AppName {
  override val hooks: Seq[HttpHook] = NoneRequired

  override protected def appNameConfiguration: Configuration = config

  override protected def actorSystem: ActorSystem = Play.current.actorSystem

  override protected def configuration: Option[Config] = Some(config.underlying)
}

class TAXSSessionCache @Inject()(override val appNameConfiguration: Configuration, val http: WSHttp)
    extends SessionCache with AppName {
  override lazy val defaultSource: String = appName
  override lazy val baseUri: String = ApplicationConfig.sessionCacheHost
  override lazy val domain: String = ApplicationConfig.sessionCacheDomain
}

class TAXSSessionCookieCrypto @Inject()() {
  lazy val crypto = new ApplicationCrypto(Play.current.configuration.underlying).SessionCookieCrypto
}
