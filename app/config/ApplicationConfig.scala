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

package config

import com.google.inject.Inject
import play.api.Configuration
import play.api.i18n.Lang
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Singleton
import scala.jdk.CollectionConverters.CollectionHasAsScala

@Singleton
class ApplicationConfig @Inject() (config: ServicesConfig, configuration: Configuration) {

  def getConf(key: String): String = config.getConfString(key, throw new Exception(s"Could not find config '$key'"))

  def getExternalUrl(key: String): String = config.getString(s"external-urls.$key")

  // Services url config
  val serviceUrl: String = config.baseUrl("tax-summaries")

  lazy val sessionCacheHost: String = config.baseUrl("cachable.session-cache")
  lazy val cidHost: String          = config.baseUrl("citizen-details")

  lazy val pertaxHost: String           = config.baseUrl("pertax")
  lazy val contactFormServiceIdentifier = "ATS"

  // Caching config
  lazy val sessionCacheDomain: String = getConf("cachable.session-cache.domain")

  lazy val homePageUrl                    = "/annual-tax-summary/"
  lazy val contactFrontendBaseUrl: String = getExternalUrl("contact-frontend.host")
  lazy val betaFeedbackUrl                =
    s"$contactFrontendBaseUrl/contact/beta-feedback-unauthenticated?service=$contactFormServiceIdentifier"

  // Encryption config
  lazy val encryptionKey: String      = config.getString("portal.clientagent.encryption.key")
  lazy val encryptionTokenMaxAge: Int = config.getConfInt("encryption.tokenMaxAge", 0)

  // External urls
  lazy val loginCallback: String                 = getConf(s"login-callback.url")
  lazy val loginUrl: String                      = getConf("login.url")
  lazy val portalUrl: String                     = getConf("portal.url")
  lazy val feedbackUrl: String                   = getConf("feedback.url")
  lazy val payeLoginUrl: String                  = getConf("paye.login.url")
  lazy val payeLoginCallbackUrl: String          = getConf("paye.login-callback.url")
  lazy val identityVerificationUpliftUrl: String = getConf("paye.iv-uplift-redirect.url")
  lazy val iVUpliftFailureCallback: String       = getConf("paye.iv-uplift-failure.url")
  lazy val contactHmrcSAUrl                      = "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/self-assessment"
  lazy val contactHmrcPayeUrl                    =
    "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/income-tax-enquiries-for-individuals-pensioners-and-employees"
  lazy val govScotAccounts                       = "https://www.gov.scot/accounts"
  lazy val govScotHowItWorks                     = "https://www.gov.uk/scottish-rate-income-tax/how-it-works"

  def scottishIncomeTaxLink(taxYear: Int): String =
    s"https://www.gov.scot/publications/scottish-income-tax-${taxYear - 1}-$taxYear/"

  lazy val calculateWelshIncomeTaxSpend = "https://www.gov.wales/calculate-welsh-income-tax-spend"

  //Application name
  lazy val appName: String = config.getString("appName")

  val saShuttered: Boolean = config.getBoolean("shuttering.sa")

  val payeShuttered: Boolean = config.getBoolean("shuttering.paye")

  val sessionTimeoutInSeconds: String   = config.getString("timeout.sessionTimeOut")
  val sessionCountdownInSeconds: String = config.getString("timeout.countdownIn")

  val accessibilityBaseUrl: String             = config.getString(s"accessibility-statement.baseUrl")
  private val accessibilityRedirectUrl: String = config.getString(s"accessibility-statement.redirectUrl")

  def accessibilityStatementUrl(referrer: String) =
    s"$accessibilityBaseUrl/accessibility-statement$accessibilityRedirectUrl?referrerUrl=${SafeRedirectUrl(accessibilityBaseUrl + referrer).encodedUrl}"

  def languageMap: Map[String, Lang] =
    Map("english" -> Lang("en"), "welsh" -> Lang("cy"))

  def payeFallbackURL: String        = config.getString("paye.language.fallbackUrl")

  def saFallbackURL: String = config.getString("sa.language.fallbackUrl")

  lazy val taxYear: Int = config.getInt("taxYear")

  val maxTaxYearsTobeDisplayed: Int = config.getInt("max.taxYears.to.display")

  def spendCategories(taxYear: Int): List[String] =
    configuration.underlying.getStringList(s"categoryOrder.$taxYear").asScala.toList

  lazy val messagesFrontendUrl: String       =
    config.baseUrl("message-frontend")
  lazy val messagesFrontendTimeoutInSec: Int = config.getInt("messages-frontend.timeout-in-seconds")

  lazy val internalAuthResourceType: String =
    config.getConfString("internal-auth.resource-type", "ddcn-live-admin-frontend")

  val ehCacheTtlInSeconds: Int = config.getInt("ehCache.ttlInSeconds")

  val SCAWrapperFutureTimeout: Int = config.getInt("sca-wrapper.future-timeout")
}
