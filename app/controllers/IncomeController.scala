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

package controllers

import com.google.inject.Inject
import config.ApplicationConfig
import controllers.auth.{AuthAction, AuthenticatedRequest}
import models.ErrorResponse
import play.api.i18n.Lang
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{AuditService, IncomeService}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import utils.GenericViewModel
import view_models.IncomeBeforeTax
import views.html.IncomeBeforeTaxView
import views.html.errors.{GenericErrorView, TokenErrorView}

import scala.concurrent.{ExecutionContext, Future}

class IncomeController @Inject()(incomeService: IncomeService, val auditService: AuditService, authAction: AuthAction,
                                 mcc : MessagesControllerComponents,
                                 incomeBeforeTaxView: IncomeBeforeTaxView,
                                 genericErrorView: GenericErrorView,
                                 tokenErrorView: TokenErrorView)(implicit val formPartialRetriever: FormPartialRetriever, appConfig: ApplicationConfig, ec: ExecutionContext)
  extends TaxYearRequest(mcc, genericErrorView, tokenErrorView) {

  def authorisedIncomeBeforeTax: Action[AnyContent] = authAction.async { request =>
    show(request)
  }

  type ViewModel = IncomeBeforeTax

  override def extractViewModel()(
    implicit request: AuthenticatedRequest[_]): Future[Either[ErrorResponse, GenericViewModel]] =
    extractViewModelWithTaxYear(incomeService.getIncomeData(_))

  override def obtainResult(result: ViewModel)(implicit request: AuthenticatedRequest[_]): Result = {
    implicit val lang : Lang = request.lang
    Ok(incomeBeforeTaxView(result, getActingAsAttorneyFor(request, result.forename, result.surname, result.utr)))
  }
}
