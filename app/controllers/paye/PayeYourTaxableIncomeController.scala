/*
 * Copyright 2021 HM Revenue & Customs
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
import models.PayeAtsData
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PayeAtsService
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import view_models.paye.PayeYourTaxableIncome
import views.html.paye.PayeYourTaxableIncomeView

import scala.concurrent.ExecutionContext

class PayeYourTaxableIncomeController @Inject()(
  payeAtsService: PayeAtsService,
  payeAuthAction: PayeAuthAction,
  mcc: MessagesControllerComponents,
  payeYourTaxableIncomeView: PayeYourTaxableIncomeView)(
  implicit formPartialRetriever: FormPartialRetriever,
  templateRenderer: TemplateRenderer,
  appConfig: ApplicationConfig,
  ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = payeAuthAction.async { implicit request: PayeAuthenticatedRequest[_] =>
    {
      payeAtsService.getPayeATSData(request.nino, taxYear).map {

        case Right(successResponse: PayeAtsData) =>
          val viewModel = PayeYourTaxableIncome.buildViewModel(successResponse)
          Ok(payeYourTaxableIncomeView(viewModel))
        case Left(response: HttpResponse) =>
          response.status match {
            case NOT_FOUND => Redirect(controllers.paye.routes.PayeErrorController.authorisedNoAts())
            case _ =>
              Logger.error(s"Error received, Http status: ${response.status}")
              Redirect(controllers.paye.routes.PayeErrorController.genericError(response.status))
          }
      }
    }
  }
}
