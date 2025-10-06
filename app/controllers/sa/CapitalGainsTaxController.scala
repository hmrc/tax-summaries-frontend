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

package controllers.sa

import com.google.inject.Inject
import config.ApplicationConfig
import controllers.auth.AuthJourney
import controllers.auth.requests.AuthenticatedRequest
import models.ErrorResponse
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{AuditService, CapitalGainsService}
import utils.{GenericViewModel, TaxYearUtil}
import view_models.CapitalGains
import views.html.CapitalGainsView
import views.html.errors.{GenericErrorView, TokenErrorView}

import scala.concurrent.{ExecutionContext, Future}

class CapitalGainsTaxController @Inject() (
  capitalGainsService: CapitalGainsService,
  val auditService: AuditService,
  authJourney: AuthJourney,
  mcc: MessagesControllerComponents,
  capitalGainsView: CapitalGainsView,
  genericErrorView: GenericErrorView,
  tokenErrorView: TokenErrorView,
  taxYearUtil: TaxYearUtil
)(implicit override val appConfig: ApplicationConfig, ec: ExecutionContext)
    extends TaxYearRequest(mcc, genericErrorView, tokenErrorView, taxYearUtil) {

  def authorisedCapitalGains: Action[AnyContent] = authJourney.authForSAIndividualsOrAgents.async { request =>
    show(request)
  }

  type ViewModel = CapitalGains

  override def extractViewModel()(implicit
    request: AuthenticatedRequest[_]
  ): Future[Either[ErrorResponse, GenericViewModel]] =
    extractViewModelWithTaxYear(capitalGainsService.getCapitalGains(_))

  override def obtainResult(result: ViewModel)(implicit request: AuthenticatedRequest[_]): Result =
    Ok(capitalGainsView(result, getActingAsAttorneyFor(request, result.forename, result.surname, result.utr)))
}
