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

package sa.controllers

import com.google.inject.Inject
import common.config.ApplicationConfig
import common.controllers.auth.AuthJourney
import common.models.ErrorResponse
import common.models.requests.AuthenticatedRequest
import common.services.AuditService
import common.utils.{GenericViewModel, TaxYearUtil}
import common.view_models.Summary
import sa.views.html.TaxsMainView
import common.views.html.errors.{GenericErrorView, TokenErrorView}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import sa.services.SummaryService

import scala.concurrent.{ExecutionContext, Future}

class AtsMainController @Inject() (
  summaryService: SummaryService,
  val auditService: AuditService,
  authJourney: AuthJourney,
  mcc: MessagesControllerComponents,
  taxsMainView: TaxsMainView,
  genericErrorView: GenericErrorView,
  tokenErrorView: TokenErrorView,
  taxYearUtil: TaxYearUtil
)(implicit override val appConfig: ApplicationConfig, ec: ExecutionContext)
    extends TaxYearRequest(mcc, genericErrorView, tokenErrorView, taxYearUtil) {

  def authorisedAtsMain: Action[AnyContent] = authJourney.authForSAIndividualsOrAgents.async { request =>
    show(request)
  }

  type ViewModel = Summary

  override def extractViewModel()(implicit
    request: AuthenticatedRequest[_]
  ): Future[Either[ErrorResponse, GenericViewModel]] =
    extractViewModelWithTaxYear(summaryService.getSummaryData(_))

  override def obtainResult(result: ViewModel)(implicit request: AuthenticatedRequest[_]): Result =
    Ok(taxsMainView(result, getActingAsAttorneyFor(request, result.forename, result.surname, result.utr)))
}
