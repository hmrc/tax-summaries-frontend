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
import controllers.auth.AuthJourney
import controllers.auth.requests.AuthenticatedRequest
import models.ErrorResponse
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{AuditService, SummaryService, TotalIncomeTaxService}
import utils.GenericViewModel
import view_models.{IncomeTaxAndNI, Summary, TotalIncomeTax}
import views.html.NicsView
import views.html.errors.{GenericErrorView, TokenErrorView}

import scala.concurrent.{ExecutionContext, Future}

class NicsController @Inject() (
  summaryService: SummaryService,
  val auditService: AuditService,
  authJourney: AuthJourney,
  mcc: MessagesControllerComponents,
  nicsView: NicsView,
  genericErrorView: GenericErrorView,
  tokenErrorView: TokenErrorView,
  totalIncomeTaxService: TotalIncomeTaxService
)(implicit override val appConfig: ApplicationConfig, ec: ExecutionContext)
    extends TaxYearRequest(mcc, genericErrorView, tokenErrorView) {

  def authorisedNics: Action[AnyContent] = authJourney.authForSAIndividualsOrAgents.async { request =>
    show(request)
  }

  type ViewModel = IncomeTaxAndNI

  override def extractViewModel()(implicit
    request: AuthenticatedRequest[_]
  ): Future[Either[ErrorResponse, GenericViewModel]] =
    extractViewModelWithTaxYear(i =>
      for {
        summaryVM <- summaryService.getSummaryData(i)
        incomeVM  <- totalIncomeTaxService.getIncomeData(i)
      } yield
        if (!summaryVM.isInstanceOf[Summary]) {
          summaryVM
        } else if (!incomeVM.isInstanceOf[TotalIncomeTax]) {
          incomeVM
        } else {
          IncomeTaxAndNI(summaryVM.asInstanceOf[Summary], incomeVM.asInstanceOf[TotalIncomeTax])
        }
    )

  override def obtainResult(result: ViewModel)(implicit request: AuthenticatedRequest[_]): Result =
    Ok(
      nicsView(
        result,
        getActingAsAttorneyFor(request, result.summary.forename, result.summary.surname, result.summary.utr)
      )
    )
}
