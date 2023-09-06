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

package views

import com.google.inject.ImplementedBy
import config.ApplicationConfig
import models.ActingAsAttorneyFor
import models.admin.SCAWrapperToggle
import play.api.Logging
import play.api.i18n.Messages
import play.api.mvc.Request
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.sca.models.BannerConfig
import uk.gov.hmrc.sca.services.WrapperService
import views.html.components.HeadBlock
import views.html.includes.sidebar
import views.html.nonScaWrapperMain

import javax.inject.Inject
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}

@ImplementedBy(classOf[MainTemplateImpl])
trait MainTemplate {

  def apply(
    pageTitle: String,
    backLinkHref: Option[String] = None,
    disableSessionExpired: Boolean = false,
    additionalScripts: Option[Html] = None,
    backLinkAttrs: Map[String, String] = Map.empty,
    actingAttorney: Option[ActingAsAttorneyFor] = None,
    beforeContentHtml: Option[Html] = None,
    pageHeading: String = null,
    showBackLink: Boolean = true,
    headerSectionNeeded: Boolean = false
  )(contentBlock: Html)(implicit
    request: Request[_],
    messages: Messages
  ): HtmlFormat.Appendable
}

class MainTemplateImpl @Inject() (
  appConfig: ApplicationConfig,
  featureFlagService: FeatureFlagService,
  wrapperService: WrapperService,
  oldLayout: nonScaWrapperMain,
  headBlock: HeadBlock,
  sidebar: sidebar
) extends MainTemplate
    with Logging {
  override def apply(
    pageTitle: String,
    backLinkHref: Option[String] = None,
    disableSessionExpired: Boolean = false,
    additionalScripts: Option[Html] = None,
    backLinkAttrs: Map[String, String] = Map.empty,
    actingAttorney: Option[ActingAsAttorneyFor] = None,
    beforeContentHtml: Option[Html] = None,
    pageHeading: String = null,
    showBackLink: Boolean = true,
    headerSectionNeeded: Boolean = false
  )(contentBlock: Html)(implicit request: Request[_], messages: Messages): HtmlFormat.Appendable = {
    val scaWrapperToggle =
      Await.result(featureFlagService.get(SCAWrapperToggle), Duration(appConfig.SCAWrapperFutureTimeout, SECONDS))
    val fullPageTitle    = s"$pageTitle - ${Messages("generic.ats.browser.title")}"

    if (false) {
      logger.debug(s"SCA Wrapper layout used for request `${request.uri}``")

      val showAccountMenu = actingAttorney.isEmpty && !disableSessionExpired

      wrapperService.layout(
        content = contentBlock,
        pageTitle = Some(fullPageTitle),
        serviceNameKey = Some(messages("generic.ats")),
        serviceNameUrl = Some(appConfig.serviceUrl),
        sidebarContent = Some(sidebar(beforeContentHtml)),
        signoutUrl = controllers.routes.AccountController.signOut.url,
        timeOutUrl = Some(controllers.routes.AccountController.sessionExpired.url),
        keepAliveUrl = controllers.routes.AccountController.keepAlive.url,
        styleSheets = Seq(headBlock()),
        bannerConfig = BannerConfig(
          showAlphaBanner = false,
          showBetaBanner = true,
          showHelpImproveBanner = true
        ),
        fullWidth = false,
        hideMenuBar = !showAccountMenu,
        disableSessionExpired = disableSessionExpired,
        backLinkUrl = backLinkHref,
        showBackLinkJS = if (backLinkHref.isDefined) {
          true
        } else {
          false
        }
      )(messages, HeaderCarrierConverter.fromRequest(request), request)

    } else {
      logger.debug(s"Old layout used for request `${request.uri}``")
      oldLayout(
        fullPageTitle,
        backLinkHref,
        disableSessionExpired,
        additionalScripts,
        backLinkAttrs,
        actingAttorney,
        beforeContentHtml,
        pageHeading,
        showBackLink,
        headerSectionNeeded
      )(
        contentBlock
      )
    }
  }
}
