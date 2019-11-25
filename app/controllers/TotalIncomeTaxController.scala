/*
 * Copyright 2019 HM Revenue & Customs
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

import config.AppFormPartialRetriever
import controllers.auth.{AuthAction, AuthenticatedRequest}
import models.ErrorResponse
import play.api.Play
import play.api.mvc.Result
import services.{AuditService, TotalIncomeTaxService}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import utils.GenericViewModel
import view_models.TotalIncomeTax
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import scala.concurrent.Future

object TotalIncomeTaxController extends TotalIncomeTaxController {
  override val totalIncomeTaxService = TotalIncomeTaxService
  override val auditService = AuditService
  override val formPartialRetriever = AppFormPartialRetriever
  override val authAction = Play.current.injector.instanceOf[AuthAction]
}

trait TotalIncomeTaxController extends TaxYearRequest {

  implicit val formPartialRetriever: FormPartialRetriever

  val authAction: AuthAction

  def totalIncomeTaxService: TotalIncomeTaxService

  def authorisedTotalIncomeTax = authAction.async {
    request => show(request)
  }

  type ViewModel = TotalIncomeTax

  override def extractViewModel()(implicit request: AuthenticatedRequest[_]): Future[Either[ErrorResponse,GenericViewModel]] = {
    extractViewModelWithTaxYear(totalIncomeTaxService.getIncomeData(_))
  }

  override def obtainResult(result:ViewModel)(implicit request: AuthenticatedRequest[_]): Result = {
    Ok(views.html.total_income_tax(result, getActingAsAttorneyFor(request, result.forename, result.surname, result.utr)))
  }
}
