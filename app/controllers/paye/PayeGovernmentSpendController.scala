/*
 * Copyright 2022 HM Revenue & Customs
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
import models.{AtsNotFoundResponse, AtsResponse, PayeAtsData}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PayeAtsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.renderer.TemplateRenderer
import view_models.paye.PayeGovernmentSpend
import views.html.errors.PayeGenericErrorView
import views.html.paye.PayeGovernmentSpendingView

import scala.concurrent.ExecutionContext

class PayeGovernmentSpendController @Inject()(
  payeAtsService: PayeAtsService,
  payeAuthAction: PayeAuthAction,
  mcc: MessagesControllerComponents,
  payeGovernmentSpendingView: PayeGovernmentSpendingView,
  payeGenericErrorView: PayeGenericErrorView)(
  implicit templateRenderer: TemplateRenderer,
  appConfig: ApplicationConfig,
  ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with Logging {

  def show(taxYear: Int): Action[AnyContent] = payeAuthAction.async { implicit request: PayeAuthenticatedRequest[_] =>
    payeAtsService.getPayeATSData(request.nino, taxYear).map {
      case Right(_: PayeAtsData)
          if (taxYear > appConfig.taxYear || taxYear < appConfig.taxYear - appConfig.maxTaxYearsTobeDisplayed) =>
        Forbidden(payeGenericErrorView())
      case Right(successResponse: PayeAtsData) =>
        Ok(payeGovernmentSpendingView(PayeGovernmentSpend(successResponse, appConfig), successResponse.isWelshTaxPayer))
      case Left(_: AtsNotFoundResponse) => Redirect(controllers.routes.ErrorController.authorisedNoAts(taxYear))
      case _                            => InternalServerError(payeGenericErrorView())
    }
  }
}
