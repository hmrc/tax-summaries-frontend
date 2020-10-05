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

package controllers

import com.google.inject.Inject
import config.ApplicationConfig
import controllers.auth.{AuthAction, MinAuthAction}
import play.api.i18n.{I18nSupport, Lang}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import uk.gov.hmrc.play.partials.FormPartialRetriever
import views.html.errors.NotAuthorisedView
import views.html.errors.NoAtsErrorView
import views.html.errors.ServiceUnavailableView

class ErrorController @Inject()(
  authAction: AuthAction,
  minAuthAction: MinAuthAction,
  mcc: MessagesControllerComponents,
  notAuthorisedView: NotAuthorisedView,
  noAtsErrorView: NoAtsErrorView,
  serviceUnavailableView: ServiceUnavailableView)(
  implicit val formPartialRetriever: FormPartialRetriever,
  appConfig: ApplicationConfig)
    extends FrontendController(mcc) with I18nSupport {

  def authorisedNoAts: Action[AnyContent] = authAction { implicit request =>
    implicit val lang: Lang = request.lang
    Ok(noAtsErrorView())
  }

  def notAuthorised: Action[AnyContent] = minAuthAction { implicit request =>
    implicit val lang: Lang = request.lang
    Ok(notAuthorisedView())
  }

  def serviceUnavailable: Action[AnyContent] = Action { implicit request: Request[_] =>
    implicit val lang: Lang = request.lang
    Ok(serviceUnavailableView())
  }
}
