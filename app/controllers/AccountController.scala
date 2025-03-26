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

package controllers

import com.google.inject.Inject
import config.ApplicationConfig
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.session_expired

class AccountController @Inject() (
  mcc: MessagesControllerComponents,
  sessionExpiredView: session_expired
)(implicit val appConfig: ApplicationConfig)
    extends FrontendController(mcc) {

  def signOut: Action[AnyContent] = Action {
    // Redirect(appConfig.feedbackUrl).withNewSession
    Redirect(appConfig.basGatewaySignOut(appConfig.survey))
  }

  def keepAlive: Action[AnyContent] = Action {
    NoContent
  }

  def sessionExpired: Action[AnyContent] = Action { implicit request =>
    Ok(sessionExpiredView()).withNewSession
  }
}
