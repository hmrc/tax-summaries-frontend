/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import uk.gov.hmrc.play.bootstrap.config.{AuditingConfigProvider, ServicesConfig}

import javax.inject.Singleton
import scala.collection.JavaConverters._

@Singleton
class ApplicationConfig @Inject()(config: ServicesConfig, configuration: Configuration) {

  def getConf(key: String): String = config.getConfString(key, throw new Exception(s"Could not find config '$key'"))

  val auditingConfig: AuditingConfig = new AuditingConfigProvider(configuration, appName).get()

  // Services url config
  val serviceUrl = config.baseUrl("tax-summaries")
  val agentServiceUrl = config.baseUrl("tax-summaries-agent")
  val serviceIdentifier = config.getString("service-identifier")

  private val contactHost = config.baseUrl("contact-frontend")
  lazy val sessionCacheHost = config.baseUrl("cachable.session-cache")
  lazy val cidHost = config.baseUrl("citizen-details")

  lazy val authHost = config.baseUrl("auth")
  private val contactFormServiceIdentifier = "TAX-SUMMARIES"

  // Caching config
  lazy val sessionCacheDomain = getConf("cachable.session-cache.domain")

  lazy val ssoUrl = Some(getConf("portal.ssoUrl"))

  lazy val reportAProblemUrl = contactHost + getConf("contact-frontend.report-a-problem-url")
  lazy val externalReportProblemUrl = s"$contactHost/contact/problem_reports"
  lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports?secure=true"

  lazy val switchToPayeUrl = "/annual-tax-summary/paye/main"
  lazy val switchToSAUrl = "/annual-tax-summary/"

  // Encryption config
  lazy val encryptionKey = config.getString("portal.clientagent.encryption.key")
  lazy val encryptionTokenMaxAge = config.getConfInt("encryption.tokenMaxAge", 0)

  lazy val assetsPrefix = config.getString(s"assets.url") + config.getString(s"assets.version") + '/'

  // External urls
  lazy val loginCallback = getConf(s"login-callback.url")
  lazy val loginUrl = getConf("login.url")
  lazy val ytaUrl = getConf("yta.url")
  lazy val portalUrl = getConf("portal.url")
  lazy val optimizelyProjectId: String = config.getString("optimizely.projectId")
  lazy val feedbackUrl: String = getConf("feedback.url")
  lazy val payeLoginUrl = getConf("paye.login.url")
  lazy val payeLoginCallbackUrl = getConf("paye.login-callback.url")
  lazy val identityVerificationUpliftUrl = getConf("paye.iv-uplift-redirect.url")
  lazy val iVUpliftFailureCallback = getConf("paye.iv-uplift-failure.url")
  lazy val contactHmrcSAUrl = "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/self-assessment"
  lazy val contactHmrcPayeUrl =
    "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/income-tax-enquiries-for-individuals-pensioners-and-employees"
  lazy val govScotAccounts = "https://www.gov.scot/accounts"
  lazy val govScotHowItWorks = "https://www.gov.uk/scottish-rate-income-tax/how-it-works"

  lazy val scottishIncomeTaxLinkCyMinusOne = "https://www.gov.scot/publications/scottish-income-tax-2019-2020/"

  def scottishIncomeTaxLink(taxYear: Int): String =
    s"https://www.gov.scot/publications/scottish-income-tax-${taxYear - 1}-$taxYear/"

  lazy val calculateWelshIncomeTaxSpend = "https://www.gov.wales/calculate-welsh-income-tax-spend"

  lazy val govUkServiceManual: String = getConf("govUkServiceManual.url")

  lazy val frontendTemplatePath: String =
    config.getString("microservice.services.frontend-template-provider.path")

  //Application name
  lazy val appName = config.getString("appName")

  val saShuttered: Boolean = config.getBoolean("shuttering.sa")

  val payeShuttered: Boolean = config.getBoolean("shuttering.paye")

  val isWelshEnabled: Boolean = config.getBoolean("welsh.enabled")

  val accessibilityStatementToggle: Boolean = config.getBoolean("accessibility-statement.enabled")
  val accessibilityBaseUrl: String = config.getString(s"accessibility-statement.baseUrl")
  private val accessibilityRedirectUrl: String = config.getString(s"accessibility-statement.redirectUrl")
  def accessibilityStatementUrl(referrer: String) =
    s"$accessibilityBaseUrl/accessibility-statement$accessibilityRedirectUrl?referrerUrl=${SafeRedirectUrl(
      accessibilityBaseUrl + referrer).encodedUrl}"

  def languageMap: Map[String, Lang] =
    Map("english" -> Lang("en"), "welsh" -> Lang("cy"))

  def payeRouteToSwitchLanguage =
    (lang: String) => controllers.routes.SaLanguageController.switchToLanguage(lang)

  def saRouteToSwitchLanguage =
    (lang: String) => controllers.paye.routes.PayeLanguageController.switchToLanguage(lang)

  def payeFallbackURL: String = config.getString("paye.language.fallbackUrl")

  def saFallbackURL: String = config.getString("sa.language.fallbackUrl")

  val taxYear: Int = config.getInt("taxYear")

  val currentTaxYearSpendData: Boolean = config.getBoolean("feature.CurrentTaxYearSpendData.enabled")

  val maxTaxYearsTobeDisplayed: Int = config.getInt("max.taxYears.to.display")

  def spendCategories(taxYear: Int): List[String] =
    configuration.underlying.getStringList(s"categoryOrder.$taxYear").asScala.toList
}
