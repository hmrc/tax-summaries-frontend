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
import play.api.Logging
import play.api.i18n.Lang
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.binders.{AbsoluteWithHostnameFromAllowlist, OnlyRelative, RedirectUrl}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Singleton

@Singleton
class ApplicationConfig @Inject() (config: ServicesConfig) extends Logging {

  private def getConf(key: String): String =
    config.getConfString(key, throw new Exception(s"Could not find config '$key'"))

  // Services url config
  val serviceUrl: String = config.baseUrl("tax-summaries")

  lazy val cidHost: String = config.baseUrl("citizen-details")

  lazy val pertaxHost: String            = config.baseUrl("pertax")
  lazy val taxSummariesStubsHost: String = config.baseUrl("tax-summaries-stubs")

  lazy val homePageUrl = "/annual-tax-summary/"

  // Encryption config
  lazy val encryptionKey: String      = config.getString("portal.clientagent.encryption.key")
  lazy val encryptionTokenMaxAge: Int = config.getConfInt("encryption.tokenMaxAge", 0)

  // External urls
  lazy val loginCallback: String                 = getConf(s"login-callback.url")
  lazy val loginUrl: String                      = getConf("login.url")
  lazy val portalUrl: String                     = getConf("portal.url")
  lazy val identityVerificationUpliftUrl: String = getConf("paye.iv-uplift-redirect.url")
  lazy val iVUpliftFailureCallback: String       = getConf("paye.iv-uplift-failure.url")
  lazy val contactHmrcSAUrl                      = "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/self-assessment"
  lazy val contactHmrcPayeUrl                    =
    "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/income-tax-enquiries-for-individuals-pensioners-and-employees"
  lazy val govScotAccounts                       = "https://www.gov.scot/accounts"
  lazy val govScotHowItWorks                     = "https://www.gov.uk/scottish-rate-income-tax/how-it-works"

  lazy val survey: String = {
    val surveyOrigin: String = config.getString("sca-wrapper.exit-survey-origin")
    s"""${config.getConfString("feedback-frontend.host", "")}/feedback/$surveyOrigin"""
  }

  def basGatewaySignOut(continueUrl: String): String = {
    val basGatewayFrontendHost: String =
      config.getString("microservice.services.bas-gateway-frontend.url")
    basGatewayFrontendHost + s"/bas-gateway/sign-out-without-state?continue=$continueUrl"
  }

  def scottishIncomeTaxLink(taxYear: Int): String =
    s"https://www.gov.scot/publications/scottish-income-tax-${taxYear - 1}-$taxYear/"

  lazy val englandIncomeTaxLink =
    "https://www.gov.uk/government/publications/how-public-spending-was-calculated-in-your-tax-summary/how-public-spending-was-calculated-in-your-tax-summary"

  lazy val walesIncomeTaxLink = "https://www.gov.wales/calculate-welsh-income-tax-spend"

  // Application name
  lazy val appName: String = config.getString("appName")

  val sessionTimeoutInSeconds: String   = config.getString("timeout.sessionTimeOut")
  val sessionCountdownInSeconds: String = config.getString("timeout.countdownIn")

  private val accessibilityBaseUrl: String     = config.getString(s"accessibility-statement.baseUrl")
  private val accessibilityRedirectUrl: String = config.getString(s"accessibility-statement.redirectUrl")

  def accessibilityStatementUrl(referrer: String): String = {
    val redirectUrl = RedirectUrl(accessibilityBaseUrl + referrer).getEither(
      OnlyRelative | AbsoluteWithHostnameFromAllowlist("localhost")
    ) match {
      case Right(safeRedirectUrl) => safeRedirectUrl.url
      case Left(errorString)      =>
        logger.warn(errorString)
        loginCallback
    }
    s"$accessibilityBaseUrl/accessibility-statement$accessibilityRedirectUrl?referrerUrl=$redirectUrl"
  }

  def languageMap: Map[String, Lang] =
    Map("english" -> Lang("en"), "welsh" -> Lang("cy"))

  def payeFallbackURL: String = config.getString("paye.language.fallbackUrl")

  def saFallbackURL: String = config.getString("sa.language.fallbackUrl")

  lazy val taxYear: Int = config.getInt("taxYear")

  val maxTaxYearsTobeDisplayed: Int = config.getInt("max.taxYears.to.display")

  val showUrBanner: Boolean = config.getBoolean("urBanner.enable")

  lazy val mongoTTL: Int = config.getConfInt("tai.cache.expiryInSeconds", 900)
}
