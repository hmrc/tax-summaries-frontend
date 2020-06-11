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
import models.{ErrorResponse, SpendData}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Result}
import services.{AuditService, GovernmentSpendService}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import utils.GenericViewModel
import view_models.GovernmentSpend

import scala.concurrent.Future

class GovernmentSpendController @Inject()(
  governmentSpendService: GovernmentSpendService,
  val auditService: AuditService,
  authAction: AuthAction)(implicit val formPartialRetriever: FormPartialRetriever, implicit val appConfig: ApplicationConfig)
    extends TaxYearRequest {

  def authorisedGovernmentSpendData: Action[AnyContent] = authAction.async { request =>
    show(request)
  }

  type ViewModel = GovernmentSpend

  override def extractViewModel()(
    implicit request: AuthenticatedRequest[_]): Future[Either[ErrorResponse, GenericViewModel]] =
    extractViewModelWithTaxYear(governmentSpendService.getGovernmentSpendData(_))

  override def obtainResult(result: ViewModel)(implicit request: AuthenticatedRequest[_]): Result =
    Ok(
      views.html.government_spending(
        result,
        assignPercentage(result.govSpendAmountData),
        getActingAsAttorneyFor(request, result.userForename, result.userSurname, result.userUtr)))

  def assignPercentage(govSpendList: List[(String, SpendData)]): (Double, Double, Double) = {
    var percentEnviron = 0.0
    var percentCultural = 0.0
    var percentHousing = 0.0

    govSpendList.foreach {
      case (key, value) =>
        if (key == "Environment") {
          percentEnviron = value.percentage.doubleValue()
        } else if (key == "Culture") {
          percentCultural = value.percentage.doubleValue()
        } else if (key == "HousingAndUtilities") {
          percentHousing = value.percentage.doubleValue()
        }

    }

    (percentEnviron, percentCultural, percentHousing)
  }

}
