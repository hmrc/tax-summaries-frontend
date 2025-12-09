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

package paye.controllers

import common.config.ApplicationConfig
import common.controllers.auth.AuthJourney
import common.models.requests.PayeAuthenticatedRequest
import common.models.{AtsNotFoundResponse, PayeAtsData}
import paye.views.html.errors.PayeGenericErrorView
import paye.views.html.PayeYourIncomeAndTaxesView
import paye.services.PayeAtsService
import paye.view_models.PayeYourIncomeAndTaxes
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PayeYourIncomeAndTaxesController @Inject() (
  payeAtsService: PayeAtsService,
  authJourney: AuthJourney,
  mcc: MessagesControllerComponents,
  payeYourIncomeAndTaxesView: PayeYourIncomeAndTaxesView,
  payeGenericErrorView: PayeGenericErrorView
)(implicit appConfig: ApplicationConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport
    with Logging {

  def show(taxYear: Int): Action[AnyContent] =
    authJourney.authForPayeIndividuals(taxYear).async { implicit request: PayeAuthenticatedRequest[_] =>
      payeAtsService.getPayeATSData(request.nino, taxYear).map {
        case Right(successResponse: PayeAtsData) =>
          PayeYourIncomeAndTaxes.buildViewModel(successResponse, taxYear) match {
            case Some(viewModel) =>
              Ok(payeYourIncomeAndTaxesView(viewModel))
            case _               =>
              val exception = new InternalServerException("Missing Paye ATS data")
              logger.error(s"Internal server error ${exception.getMessage}", exception)
              InternalServerError(exception.getMessage)
          }
        case Left(_: AtsNotFoundResponse)        =>
          Redirect(common.controllers.routes.ErrorController.authorisedNoAts(taxYear))
        case _                                   => InternalServerError(payeGenericErrorView())
      }
    }
}
