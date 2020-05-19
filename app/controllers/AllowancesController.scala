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
import config.AppFormPartialRetriever
import connectors.{DataCacheConnector, MiddleConnector}
import controllers.auth.{AuthAction, AuthenticatedRequest}
import models.ErrorResponse
import play.api.Play
import play.api.mvc.Result
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import services.{AllowanceService, AtsService, AuditService, CryptoService}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import utils.GenericViewModel
import view_models.Allowances

import scala.concurrent.Future

class AllowancesController @Inject()(allowanceService: AllowanceService) extends TaxYearRequest {

  implicit val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever

  override val auditService: AuditService = AuditService

  val authAction: AuthAction = Play.current.injector.instanceOf[AuthAction]

  def authorisedAllowance = authAction.async {
    request => show(request)
  }

  type ViewModel = Allowances

  override def extractViewModel()(implicit request: AuthenticatedRequest[_]): Future[Either[ErrorResponse,GenericViewModel]] = {
    extractViewModelWithTaxYear(allowanceService.getAllowances(_))
  }

  override def obtainResult(result: ViewModel)(implicit request: AuthenticatedRequest[_]): Result = {
    Ok(views.html.tax_free_amount(result, getActingAsAttorneyFor(request, result.forename, result.surname, result.utr)))
  }

}
