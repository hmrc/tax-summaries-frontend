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
import com.typesafe.config.Config
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.LoadAuditingConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.{AppName, ControllerConfig}

class TAXSControllerConfig @Inject()(configuration: Configuration) extends ControllerConfig {
  lazy val controllerConfigs: Config = configuration.underlying.getConfig("controllers")
}

class TAXSAuditConnector @Inject()(environment: Environment, configuration: Configuration) extends AuditConnector with AppName {
  override lazy val auditingConfig: AuditingConfig = LoadAuditingConfig(configuration , environment.mode, "microservice.services.auditing")

  override protected def appNameConfiguration: Configuration = configuration
}

class TAXSSessionCache @Inject()(override val appNameConfiguration: Configuration, val http: HttpClient)
                                (implicit val appConfig: ApplicationConfig)extends SessionCache with AppName {
  override lazy val defaultSource: String = appName
  override lazy val baseUri: String = appConfig.sessionCacheHost
  override lazy val domain: String = appConfig.sessionCacheDomain
}

