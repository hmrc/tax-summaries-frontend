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

import java.time.LocalDate

import com.google.inject.Inject
import config.ApplicationConfig
import controllers.auth.{PayeAuthAction, PayeAuthenticatedRequest}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.time.{CurrentTaxYear, TaxYear}
import views.html.HowTaxIsSpentView
import views.html.errors._

class PayeErrorController @Inject()(
  payeAuthAction: PayeAuthAction,
  mcc: MessagesControllerComponents,
  payeGenericErrorView: PayeGenericErrorView,
  howTaxIsSpentView: HowTaxIsSpentView,
  payeNotAuthorisedView: PayeNotAuthorisedView,
  payeServiceUnavailableView: PayeServiceUnavailableView)(
  implicit formPartialRetriever: FormPartialRetriever,
  appConfig: ApplicationConfig)
    extends FrontendController(mcc) with I18nSupport with CurrentTaxYear {

  val payeYear = appConfig.payeYear
  override def now: () => LocalDate = () => LocalDate.now()

  def genericError(status: Int): Action[AnyContent] = payeAuthAction { implicit request: PayeAuthenticatedRequest[_] =>
    {
      status match {
        case INTERNAL_SERVER_ERROR => InternalServerError(payeGenericErrorView())
        case _                     => BadGateway(payeGenericErrorView())
      }
    }
  }

  def authorisedNoAts: Action[AnyContent] = payeAuthAction { implicit request: PayeAuthenticatedRequest[_] =>
    {
      Ok(howTaxIsSpentView(current.previous))
    }
  }

  def notAuthorised: Action[AnyContent] = Action { implicit request: Request[_] =>
    {
      Ok(payeNotAuthorisedView())
    }
  }

  def serviceUnavailable: Action[AnyContent] = Action { implicit request: Request[_] =>
    {
      Ok(payeServiceUnavailableView())
    }
  }
}
