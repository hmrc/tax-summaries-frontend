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
import services.{AuditService, SummaryService}
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import utils.{GenericViewModel, TaxSummariesRegime, TaxYearUtil, TaxsController}
import view_models.Summary

import scala.concurrent.Future
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import uk.gov.hmrc.play.partials.FormPartialRetriever

object AtsMainController extends AtsMainController {

  override val summaryService = SummaryService
  override val auditService = AuditService
  override val formPartialRetriever = AppFormPartialRetriever
}

trait AtsMainController extends TaxYearRequest {

  implicit val formPartialRetriever: FormPartialRetriever

  def summaryService: SummaryService

  def authorisedAtsMain = AuthorisedFor(TaxSummariesRegime, GGConfidence).async {
    user => request => show(user, request)
  }

  type ViewModel = Summary


  override def extractViewModel()(implicit user: User, request: Request[AnyRef]): Future[Either[ErrorResponse,GenericViewModel]] = {
    extractViewModelWithTaxYear(summaryService.getSummaryData(_))
  }

  override def obtainResult(result: ViewModel)(implicit user: User, request: Request[AnyRef]): Result = {
    Ok(views.html.taxs_main(result, getActingAsAttorneyFor(user, result.forename, result.surname, result.utr)))
  }
}
