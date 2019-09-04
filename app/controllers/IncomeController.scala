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
import models.ErrorResponse
import play.api.mvc.{Request, Result}
import services.{AuditService, IncomeService}
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import utils.{GenericViewModel, TaxSummariesRegime, TaxYearUtil, TaxsController}
import view_models.IncomeBeforeTax
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import uk.gov.hmrc.play.partials.FormPartialRetriever

import scala.concurrent.Future

object IncomeController extends IncomeController {
  override val incomeService = IncomeService
  override val auditService = AuditService
  override val formPartialRetriever = AppFormPartialRetriever
}

trait IncomeController extends TaxYearRequest {

  implicit val formPartialRetriever: FormPartialRetriever

  def incomeService: IncomeService

  def authorisedIncomeBeforeTax = AuthorisedFor(TaxSummariesRegime, GGConfidence).async {
    user => request => show(user,request)
  }

  type ViewModel = IncomeBeforeTax

  override def extractViewModel()(implicit user: User, request: Request[AnyRef]): Future[Either[ErrorResponse,GenericViewModel]] = {
    extractViewModelWithTaxYear(incomeService.getIncomeData(_))
  }

  override def obtainResult(result: ViewModel)(implicit user: User, request: Request[AnyRef]): Result = {
    Ok(views.html.income_before_tax(result, getActingAsAttorneyFor(user, result.forename, result.surname, result.utr)))
  }
}
