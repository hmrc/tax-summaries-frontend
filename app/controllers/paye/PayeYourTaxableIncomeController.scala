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

import config.{AppFormPartialRetriever, ApplicationConfig, TAXSSessionCookieCrypto}
import connectors.MiddleConnector
import controllers.auth.{PayeAuthAction, PayeAuthenticatedRequest}
import models.PayeAtsData
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import play.api.{Logger, Play}
import services.PayeAtsService
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.frontend.controller.FrontendController
import view_models.paye.PayeYourTaxableIncome

object PayeYourTaxableIncomeController extends PayeYourTaxableIncomeController {
  override val payeAuthAction = Play.current.injector.instanceOf[PayeAuthAction]
  override val payeYear: Int = ApplicationConfig.payeYear
  override val payeAtsService = new PayeAtsService(new MiddleConnector)
}

trait PayeYourTaxableIncomeController extends FrontendController {

  implicit lazy val formPartialRetriever = new AppFormPartialRetriever(new TAXSSessionCookieCrypto)

  val payeAuthAction: PayeAuthAction
  val payeYear: Int
  val payeAtsService : PayeAtsService

  def show: Action[AnyContent] = payeAuthAction.async {
    implicit request: PayeAuthenticatedRequest[_] =>
      payeAtsService.getPayeATSData(request.nino, payeYear).map {

        case Right(successResponse: PayeAtsData) => {
          val viewModel = PayeYourTaxableIncome.buildViewModel(successResponse)
          Ok(views.html.paye.paye_your_taxable_income(viewModel))
        }
        case Left(response: HttpResponse) => response.status match {
          case NOT_FOUND => Redirect(controllers.paye.routes.PayeErrorController.authorisedNoAts())
          case _ => {
            Logger.error(s"Error received, Http status: ${response.status}")
            Redirect(controllers.paye.routes.PayeErrorController.genericError(response.status))
          }
        }
      }
  }
}

