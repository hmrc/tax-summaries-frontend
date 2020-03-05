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

package controllers.paye

import config.AppFormPartialRetriever
import controllers.auth.{PayeAuthAction, PayeAuthenticatedRequest}
import play.api.Play
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Results.NotFound
import play.api.mvc.{Action, AnyContent}

object PayeErrorController extends PayeErrorController {
  override val payeAuthAction = Play.current.injector.instanceOf[PayeAuthAction]
}

trait PayeErrorController {
  implicit val formPartialRetriever = AppFormPartialRetriever
  val payeAuthAction: PayeAuthAction

  def authorisedNoAts: Action[AnyContent] = payeAuthAction {
    implicit request: PayeAuthenticatedRequest[_] => NotFound(views.html.errors.paye_no_ats_error())
  }
}
