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

import com.google.inject.Inject
import common.config.{ApplicationConfig, PayeConfig}
import common.controllers.auth.AuthJourney
import common.models.requests.PayeAuthenticatedRequest
import common.models.{AtsNotFoundResponse, PayeAtsData}
import common.view_models.paye.PayeIncomeTaxAndNics
import common.views.html.errors.PayeGenericErrorView
import common.views.html.paye.PayeIncomeTaxAndNicsView
import paye.services.PayeAtsService
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext

class PayeIncomeTaxAndNicsController @Inject() (
  payeAtsService: PayeAtsService,
  authJourney: AuthJourney,
  mcc: MessagesControllerComponents,
  payeIncomeTaxAndNicsView: PayeIncomeTaxAndNicsView,
  payeConfig: PayeConfig,
  payeGenericErrorView: PayeGenericErrorView
)(implicit appConfig: ApplicationConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport
    with Logging {

  def show(taxYear: Int): Action[AnyContent] =
    authJourney.authForPayeIndividuals(taxYear).async { implicit request: PayeAuthenticatedRequest[_] =>
      payeAtsService.getPayeATSData(request.nino, taxYear).map {
        case Right(successResponse: PayeAtsData) =>
          Ok(
            payeIncomeTaxAndNicsView(
              PayeIncomeTaxAndNics(
                payeAtsData = successResponse,
                scottishRates = payeConfig.scottishTaxBandKeys,
                uKRates = payeConfig.ukTaxBandKeys,
                adjustments = payeConfig.adjustmentsKeys.toSet
              ),
              successResponse.isWelshTaxPayer,
              includeBRDMessage = successResponse.includeBRDMessage
            )
          )

        case Left(_: AtsNotFoundResponse) =>
          Redirect(common.controllers.routes.ErrorController.authorisedNoAts(taxYear))
        case _                            => InternalServerError(payeGenericErrorView())
      }
    }
}
