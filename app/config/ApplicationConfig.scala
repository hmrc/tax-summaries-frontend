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

import play.api.Mode.Mode
import play.api.{Configuration, Play}
import uk.gov.hmrc.play.config.ServicesConfig

trait ApplicationConfig {

  protected def mode: Mode = Play.current.mode

  protected def runModeConfiguration: Configuration = Play.current.configuration

  val appName: String
  val assetsPrefix: String
  val betaFeedbackUrl: String
  val betaFeedbackUnauthenticatedUrl: String
  val analyticsToken: Option[String]
  val analyticsHost: String
  val reportAProblemUrl: String
  val reportAProblemPartialUrl: String
  val externalReportProblemUrl: String
  val reportAProblemNonJSUrl: String
  val ssoUrl: Option[String]
  val encryptionKey: String
  val encryptionTokenMaxAge: Int
  val loginCallback: String
  val loginUrl: String
  val ytaUrl: String
  val portalUrl: String
  val authHost: String
  val sessionCacheHost: String
  val optimizelyProjectId: String
  val feedbackUrl: String
  val payeYear: Int
  val payeLoginUrl: String
}

object ApplicationConfig extends ApplicationConfig with ServicesConfig {

  def getConf(key: String) = getConfString(key, throw new Exception(s"Could not find config '$key'"))

  // Services url config
  private val contactHost = baseUrl("contact-frontend")
  override lazy val sessionCacheHost = baseUrl("cachable.session-cache")
  override lazy val authHost = baseUrl("auth")
  private val contactFormServiceIdentifier = "TAX-SUMMARIES"

  // Caching config
  lazy val sessionCacheDomain = getConf("cachable.session-cache.domain")

  // Beta feedback config
  override lazy val betaFeedbackUrl = (if (env == "Prod") "" else contactHost) + getConf("contact-frontend.beta-feedback-url.authenticated")
  override lazy val betaFeedbackUnauthenticatedUrl = (if (env == "Prod") "" else contactHost) + getConf("contact-frontend.beta-feedback-url.unauthenticated")

  // Analytics config
  override lazy val analyticsToken: Option[String] = Some(getString(s"google-analytics.token"))
  override lazy val analyticsHost: String = getString(s"google-analytics.host")
  override lazy val ssoUrl = Some(getConf("portal.ssoUrl"))

  override lazy val reportAProblemUrl = contactHost + getConf("contact-frontend.report-a-problem-url")
  override lazy val externalReportProblemUrl = s"$contactHost/contact/problem_reports"
  override lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports?secure=true"

  // Encryption config
  override lazy val encryptionKey = runModeConfiguration.getString("portal.clientagent.encryption.key").getOrElse("1111111111111111111111")
  override lazy val encryptionTokenMaxAge = getConfInt("encryption.tokenMaxAge", 0)

  override lazy val assetsPrefix = getConf("assets.url") + getConf("assets.version")

  // External urls
  override lazy val loginCallback = getConf(s"login-callback.url")
  override lazy val loginUrl = getConf("login.url")
  override lazy val ytaUrl = getConf("yta.url")
  override lazy val portalUrl = getConf("portal.url")
  override lazy val optimizelyProjectId: String = getString("optimizely.projectId")
  override lazy val feedbackUrl: String = getConf("feedback.url")
  override lazy val payeLoginUrl = getConf("login.paye.url")

  //Application name
  override lazy val appName = getString("appName")

  override val payeYear: Int = getInt("paye.year")
}
