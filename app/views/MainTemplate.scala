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
import play.api.Logging
import play.api.i18n.Messages
import play.api.mvc.RequestHeader
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.hmrcstandardpage.ServiceURLs
import uk.gov.hmrc.sca.models.BannerConfig
import uk.gov.hmrc.sca.services.WrapperService
import views.html.components.{AdditionalJavascript, HeadBlock}
import views.html.includes.sidebar
import javax.inject.Inject

@ImplementedBy(classOf[MainTemplateImpl])
trait MainTemplate {

  def apply(
    pageTitle: String,
    disableSessionExpired: Boolean = false,
    additionalScripts: Option[Html] = None,
    backLinkAttrs: Map[String, String] = Map.empty,
    actingAttorney: Option[ActingAsAttorneyFor] = None,
    beforeContentHtml: Option[Html] = None,
    pageHeading: String = null,
    showBackLink: Boolean = true,
    headerSectionNeeded: Boolean = false
  )(contentBlock: Html)(implicit
    requestHeader: RequestHeader,
    messages: Messages
  ): HtmlFormat.Appendable
}

class MainTemplateImpl @Inject() (
  appConfig: ApplicationConfig,
  wrapperService: WrapperService,
  headBlock: HeadBlock,
  sidebar: sidebar,
  scripts: AdditionalJavascript
) extends MainTemplate
    with Logging {
  override def apply(
    pageTitle: String,
    disableSessionExpired: Boolean = false,
    additionalScripts: Option[Html] = None,
    backLinkAttrs: Map[String, String] = Map.empty,
    actingAttorney: Option[ActingAsAttorneyFor] = None,
    beforeContentHtml: Option[Html] = None,
    pageHeading: String = null,
    showBackLink: Boolean = true,
    headerSectionNeeded: Boolean = false
  )(contentBlock: Html)(implicit requestHeader: RequestHeader, messages: Messages): HtmlFormat.Appendable = {

    val fullPageTitle = s"$pageTitle - ${Messages("generic.ats.browser.title")}"

    val showAccountMenu = actingAttorney.isEmpty && !disableSessionExpired

    wrapperService.standardScaLayout(
      content = contentBlock,
      pageTitle = Some(fullPageTitle),
      serviceNameKey = Some(messages("generic.ats")),
      serviceURLs = ServiceURLs(
        serviceUrl = Some(appConfig.homePageUrl),
        signOutUrl = Some(controllers.routes.AccountController.signOut.url),
        accessibilityStatementUrl = Some(appConfig.accessibilityStatementUrl(requestHeader.uri))
      ),
      sidebarContent = Some(sidebar(beforeContentHtml)),
      timeOutUrl = Some(controllers.routes.AccountController.sessionExpired.url),
      keepAliveUrl = controllers.routes.AccountController.keepAlive.url,
      styleSheets = Seq(headBlock()),
      bannerConfig = BannerConfig(
        showAlphaBanner = false,
        showBetaBanner = true,
        showHelpImproveBanner = appConfig.showUrBanner
      ),
      fullWidth = false,
      hideMenuBar = !showAccountMenu,
      disableSessionExpired = disableSessionExpired,
      showBackLinkJS = showBackLink,
      scripts = Seq(scripts())
    )(messages, requestHeader)
  }
}
