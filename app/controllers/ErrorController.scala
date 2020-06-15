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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import uk.gov.hmrc.play.partials.FormPartialRetriever

class ErrorController @Inject()(authAction: AuthAction, minAuthAction: MinAuthAction, mcc : MessagesControllerComponents)(
  implicit val formPartialRetriever: FormPartialRetriever,implicit val appConfig: ApplicationConfig)
    extends FrontendController(mcc) {

  def authorisedNoAts: Action[AnyContent] = authAction { implicit request =>
    Ok(views.html.errors.no_ats_error())
  }

  def notAuthorised: Action[AnyContent] = minAuthAction { implicit request =>
    Ok(views.html.errors.not_authorised())
  }

  def serviceUnavailable: Action[AnyContent] = Action { implicit request: Request[_] =>
    Ok(views.html.errors.service_unavailable())
  }
}
