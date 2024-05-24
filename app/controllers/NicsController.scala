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
import services.{AuditService, IncomeTaxAndNIService}
import utils.GenericViewModel
import view_models.IncomeTaxAndNI
import views.html.NicsView
import views.html.errors.{GenericErrorView, TokenErrorView}

import scala.concurrent.{ExecutionContext, Future}

class NicsController @Inject() (
  val auditService: AuditService,
  authJourney: AuthJourney,
  mcc: MessagesControllerComponents,
  nicsView: NicsView,
  genericErrorView: GenericErrorView,
  tokenErrorView: TokenErrorView,
  incomeTaxAndNIService: IncomeTaxAndNIService
)(implicit override val appConfig: ApplicationConfig, ec: ExecutionContext)
    extends TaxYearRequest(mcc, genericErrorView, tokenErrorView) {

  private def redirectToMainTaxAndNIPage(request: AuthenticatedRequest[AnyContent]): Result =
    request.getQueryString("taxYear") match {
      case Some(taxYear) => Redirect(controllers.routes.NicsController.authorisedTaxAndNICs.url + s"?taxYear=$taxYear")
      case _             => Redirect(controllers.routes.AtsMergePageController.onPageLoad)
    }

  def redirectForDeprecatedTotalIncomeTaxPage: Action[AnyContent] = authJourney.authForSAIndividualsOrAgents {
    request => redirectToMainTaxAndNIPage(request)
  }

  def redirectForDeprecatedNicsPage: Action[AnyContent] = authJourney.authForSAIndividualsOrAgents { request =>
    redirectToMainTaxAndNIPage(request)
  }

  def authorisedTaxAndNICs: Action[AnyContent] = authJourney.authForSAIndividualsOrAgents.async { request =>
    show(request)
  }

  type ViewModel = IncomeTaxAndNI

  override def extractViewModel()(implicit
    request: AuthenticatedRequest[_]
  ): Future[Either[ErrorResponse, GenericViewModel]] =
    extractViewModelWithTaxYear(incomeTaxAndNIService.getIncomeAndNIData(_))

  override def obtainResult(result: ViewModel)(implicit request: AuthenticatedRequest[_]): Result =
    Ok(
      nicsView(
        result,
        getActingAsAttorneyFor(request, result.forename, result.surname, result.utr)
      )
    )
}
