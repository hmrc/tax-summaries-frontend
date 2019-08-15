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
import models.{ErrorResponse, SpendData}
import play.api.mvc.{Request, Result}
import services.{AuditService, GovernmentSpendService}
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import utils.{GenericViewModel, TaxSummariesRegime, TaxYearUtil, TaxsController}
import view_models.GovernmentSpend
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import uk.gov.hmrc.play.partials.FormPartialRetriever

import scala.concurrent.Future

object GovernmentSpendController extends GovernmentSpendController {
  override val governmentSpendService = GovernmentSpendService
  override val auditService = AuditService
  override val formPartialRetriever = AppFormPartialRetriever
}

trait GovernmentSpendController extends TaxsController {

  implicit val formPartialRetriever: FormPartialRetriever

  def governmentSpendService: GovernmentSpendService

  def authorisedGovernmentSpendData = AuthorisedFor(TaxSummariesRegime, GGConfidence).async {
    user => request => show(user,request)
  }

  type T = GovernmentSpend

  override def extractViewModel()(implicit user: User, request: Request[AnyRef]): Future[Either[ErrorResponse,GenericViewModel]] = {
    extractViewModel(governmentSpendService.getGovernmentSpendData(_))
  }

  override def obtainResult(result: T)(implicit user:User, request: Request[AnyRef]): Result = {
    Ok(views.html.government_spending(result, assignPercentage(result.govSpendAmountData), getActingAsAttorneyFor(user, result.userForename, result.userSurname, result.userUtr)))
  }

  def assignPercentage(govSpendList: List[(String, SpendData)]): (Double, Double, Double) = {
    var percentEnviron = 0.0
    var percentCultural = 0.0
    var percentHousing = 0.0

    govSpendList.foreach {
      case (key, value) =>
        if(key == "Environment") {
          percentEnviron = value.percentage.doubleValue()
        } else if(key == "Culture") {
          percentCultural = value.percentage.doubleValue()
        } else if(key == "HousingAndUtilities") {
          percentHousing = value.percentage.doubleValue()
        }

    }

    (percentEnviron, percentCultural, percentHousing)
  }

}
