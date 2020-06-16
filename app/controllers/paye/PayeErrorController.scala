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

import com.google.inject.Inject
import config.ApplicationConfig
import controllers.auth.{PayeAuthAction, PayeAuthenticatedRequest}
import play.api.i18n.{I18nSupport, Lang}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import uk.gov.hmrc.play.partials.FormPartialRetriever
import view_models.paye.PayeAtsMain

class PayeErrorController  @Inject()(payeAuthAction: PayeAuthAction,mcc : MessagesControllerComponents)
                                    (implicit formPartialRetriever: FormPartialRetriever,appConfig: ApplicationConfig)
                                     extends FrontendController(mcc) with I18nSupport{
  val payeYear = appConfig.payeYear

  def genericError (status : Int): Action[AnyContent] = payeAuthAction {
    implicit request: PayeAuthenticatedRequest[_] =>{
      implicit  val lang : Lang =request.lang
      status match {
        case INTERNAL_SERVER_ERROR => InternalServerError(views.html.errors.paye_generic_error())
        case _ => BadGateway(views.html.errors.paye_generic_error())
      }
    }
  }

  def authorisedNoAts: Action[AnyContent] = payeAuthAction {
    implicit request: PayeAuthenticatedRequest[_] => {
      implicit  val lang : Lang =request.lang
      NotFound(views.html.errors.paye_no_ats_error(PayeAtsMain(payeYear)))
    }
  }

  def notAuthorised: Action[AnyContent] = Action {
    implicit request: Request[_] => {
      implicit  val lang : Lang =request.lang
      Ok(views.html.errors.paye_not_authorised())
    }
  }

  def serviceUnavailable: Action[AnyContent] = Action {
    implicit request: Request[_] => {
      implicit  val lang : Lang =request.lang
      Ok(views.html.errors.paye_service_unavailable())
    }
  }
}
