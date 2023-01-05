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

package controllers

import com.google.inject.Inject
import config.ApplicationConfig
import controllers.auth.{AuthJourney, AuthenticatedRequest}
import models.ErrorResponse
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{AuditService, TotalIncomeTaxService}
import utils.GenericViewModel
import view_models.TotalIncomeTax
import views.html.TotalIncomeTaxView
import views.html.errors.{GenericErrorView, TokenErrorView}

import scala.concurrent.{ExecutionContext, Future}

class TotalIncomeTaxController @Inject() (
  totalIncomeTaxService: TotalIncomeTaxService,
  val auditService: AuditService,
  authJourney: AuthJourney,
  mcc: MessagesControllerComponents,
  totalIncomeTaxView: TotalIncomeTaxView,
  genericErrorView: GenericErrorView,
  tokenErrorView: TokenErrorView
)(implicit override val appConfig: ApplicationConfig, ec: ExecutionContext)
    extends TaxYearRequest(mcc, genericErrorView, tokenErrorView) {

  def authorisedTotalIncomeTax: Action[AnyContent] = authJourney.authWithSelfAssessment.async { request =>
    show(request)
  }

  type ViewModel = TotalIncomeTax

  override def extractViewModel()(implicit
    request: AuthenticatedRequest[_]
  ): Future[Either[ErrorResponse, GenericViewModel]] =
    extractViewModelWithTaxYear(totalIncomeTaxService.getIncomeData(_))

  override def obtainResult(result: ViewModel)(implicit request: AuthenticatedRequest[_]): Result =
    Ok(totalIncomeTaxView(result, getActingAsAttorneyFor(request, result.forename, result.surname, result.utr)))
}
