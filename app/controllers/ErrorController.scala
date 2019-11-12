/*
 * Copyright 2019 HM Revenue & Customs
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

import config.AppFormPartialRetriever
import controllers.auth.{AuthAction, AuthenticatedRequest}
import play.api.Play
import play.api.mvc.Result
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.partials.FormPartialRetriever
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import utils.AccountUtils

object ErrorController extends ErrorController {
  override val formPartialRetriever = AppFormPartialRetriever
  override val authAction = Play.current.injector.instanceOf[AuthAction]
}

trait ErrorController extends FrontendController
        with AccountUtils {

  implicit val formPartialRetriever: FormPartialRetriever

  val authAction: AuthAction

  def authorisedNoAts = authAction {
    implicit request => noAts
  }

  //TODO Check if this is correct to use auth
  def notAuthorised = authAction {
    implicit request => Ok(views.html.errors.not_authorised())
  }

  def noAts(implicit request: AuthenticatedRequest[_]): Result = {
    Ok(views.html.errors.no_ats_error())
  }

}
