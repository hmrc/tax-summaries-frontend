/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.mvc.{Request, Result}
import services.{AuditService, GovernmentSpendService}
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import utils.{GenericViewModel, TaxSummariesRegime, TaxsController}
import view_models.GovernmentSpend
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.concurrent.Future

object GovernmentSpendController extends GovernmentSpendController {
  override val governmentSpendService = GovernmentSpendService
  override val auditService = AuditService
}

trait GovernmentSpendController extends TaxsController {

  def governmentSpendService: GovernmentSpendService

  def authorisedGovernmentSpendData = AuthorisedFor(TaxSummariesRegime, GGConfidence).async {
    user => request => show(user,request)
  }

  type T = GovernmentSpend

  override def extractViewModel()(implicit user: User, request: Request[AnyRef]): Future[GenericViewModel] = {
    governmentSpendService.getGovernmentSpendData
  }

  override def obtainResult(result: T)(implicit user:User, request: Request[AnyRef]): Result = {
    Ok(views.html.government_spending(result, getActingAsAttorneyFor(user, result.userForename, result.userSurname, result.userUtr)))
  }
}
