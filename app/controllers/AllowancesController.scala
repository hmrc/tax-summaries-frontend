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
import services.{AllowanceService, AuditService}
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import utils.{GenericViewModel, TaxSummariesRegime, TaxYearUtil, TaxsController}
import view_models.Allowances
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.mvc.Results.BadRequest
import uk.gov.hmrc.play.partials.FormPartialRetriever

import scala.concurrent.Future

object AllowancesController extends AllowancesController {
  override val allowanceService = AllowanceService
  override val auditService = AuditService
  override val formPartialRetriever = AppFormPartialRetriever
}

trait AllowancesController extends TaxsController {

  implicit val formPartialRetriever: FormPartialRetriever

  def allowanceService: AllowanceService

  def authorisedAllowance = AuthorisedFor(TaxSummariesRegime, GGConfidence).async {
    user => request => show(user,request)
  }

  type T = Allowances

  override def extractViewModel()(implicit user: User, request: Request[AnyRef]): Future[Either[ErrorResponse,GenericViewModel]] = {
    extractViewModel(allowanceService.getAllowances(_))
  }

  override def obtainResult(result: T)(implicit user: User, request: Request[AnyRef]): Result = {
    Ok(views.html.tax_free_amount(result, getActingAsAttorneyFor(user, result.forename, result.surname, result.utr)))
  }

}
