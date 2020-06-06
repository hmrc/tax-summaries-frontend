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
import play.api.Mode.Mode
import play.api.{Configuration, Environment, Play}
import uk.gov.hmrc.play.config.ServicesConfig

class ApplicationConfig @Inject()(val runModeConfiguration: Configuration, environment: Environment) extends ServicesConfig {


  protected def mode: Mode = environment.mode

//  protected def runModeConfiguration: Configuration = Play.current.configuration

  def getConf(key: String) = getConfString(key, throw new Exception(s"Could not find config '$key'"))

  // Services url config
  val serviceUrl = baseUrl("tax-summaries")
  val agentServiceUrl = baseUrl("tax-summaries-agent")

  private val contactHost = baseUrl("contact-frontend")
  lazy val sessionCacheHost = baseUrl("cachable.session-cache")
  lazy val authHost = baseUrl("auth")
  private val contactFormServiceIdentifier = "TAX-SUMMARIES"

  // Caching config
  lazy val sessionCacheDomain = getConf("cachable.session-cache.domain")

  // Beta feedback config
  lazy val betaFeedbackUrl = (if (env == "Prod") "" else contactHost) + getConf("contact-frontend.beta-feedback-url.authenticated")
  lazy val betaFeedbackUnauthenticatedUrl = (if (env == "Prod") "" else contactHost) + getConf("contact-frontend.beta-feedback-url.unauthenticated")

  // Analytics config
  lazy val analyticsToken: Option[String] = Some(getString(s"google-analytics.token"))
  lazy val analyticsHost: String = getString(s"google-analytics.host")
  lazy val ssoUrl = Some(getConf("portal.ssoUrl"))

  lazy val reportAProblemUrl = contactHost + getConf("contact-frontend.report-a-problem-url")
  lazy val externalReportProblemUrl = s"$contactHost/contact/problem_reports"
  lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports?secure=true"

  // Encryption config
  lazy val encryptionKey = runModeConfiguration.getString("portal.clientagent.encryption.key").getOrElse("1111111111111111111111")
  lazy val encryptionTokenMaxAge = getConfInt("encryption.tokenMaxAge", 0)

  lazy val assetsPrefix = getConf("assets.url") + getConf("assets.version")

  // External urls
  lazy val loginCallback = getConf(s"login-callback.url")
  lazy val loginUrl = getConf("login.url")
  lazy val ytaUrl = getConf("yta.url")
  lazy val portalUrl = getConf("portal.url")
  lazy val optimizelyProjectId: String = getString("optimizely.projectId")
  lazy val feedbackUrl: String = getConf("feedback.url")
  lazy val payeLoginUrl = getConf("paye.login.url")
  lazy val payeLoginCallbackUrl = getConf("paye.login-callback.url")
  lazy val identityVerificationUpliftUrl = getConf("paye.iv-uplift-redirect.url")
  lazy val iVUpliftFailureCallback = getConf("paye.iv-uplift-failure.url")

  lazy val govUkServiceManual: String = getConf("govUkServiceManual.url")

  //Application name
  lazy val appName = getString("appName")

  val payeYear: Int = getInt("paye.year")

  val saShuttered: Boolean = getBoolean("shuttering.sa")

  val payeShuttered: Boolean = getBoolean("shuttering.paye")
}
