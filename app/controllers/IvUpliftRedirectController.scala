/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers

import com.google.inject.Inject
import config.ApplicationConfig
import controllers.auth.{PayeAuthAction, PayeAuthenticatedRequest}
import models.PayeAtsData
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.PayeAtsService
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import view_models.paye.PayeAtsMain
import views.html.paye.PayeTaxsMainView

import scala.concurrent.{ExecutionContext, Future}

class IvUpliftRedirectController @Inject()(mcc: MessagesControllerComponents)(
  appConfig: ApplicationConfig,
  ec: ExecutionContext)
    extends FrontendController(mcc) {

  def upliftConfidenceLevel: Action[AnyContent] = Action.async {
    Future.successful(
      Redirect(
        appConfig.identityVerificationUpliftUrl,
        Map(
          "origin"          -> Seq(appConfig.appName),
          "confidenceLevel" -> Seq(ConfidenceLevel.L200.toString),
          "completionURL"   -> Seq(appConfig.loginCallback),
          "failureURL"      -> Seq(appConfig.iVUpliftFailureCallback)
        )
      ))
  }
}
