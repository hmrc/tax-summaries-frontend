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
import connectors.AuthenticationConnector
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext => User}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.partials.FormPartialRetriever
import utils.{AccountUtils, TAXSGovernmentGateway, TaxSummariesRegime}

object ErrorController extends ErrorController {
  override val formPartialRetriever = AppFormPartialRetriever
}

trait ErrorController extends FrontendController
        with Actions
        with AccountUtils
        with AuthenticationConnector {

  implicit val formPartialRetriever: FormPartialRetriever

  def authorisedNoAts = AuthorisedFor(TaxSummariesRegime, GGConfidence) {
    implicit user => implicit request => noAts
  }
  
  def notAuthorised = AuthenticatedBy(TAXSGovernmentGateway, GGConfidence) {
    implicit user => implicit request => Ok(views.html.errors.not_authorised())
  }

  def noAts(implicit user: User, request: Request[AnyRef]): Result = {
    Ok(views.html.errors.no_ats_error())
  }

  def noYear = AuthenticatedBy(TAXSGovernmentGateway, GGConfidence) {
    implicit user => implicit request => BadRequest(views.html.errors.no_year())
  }

}
