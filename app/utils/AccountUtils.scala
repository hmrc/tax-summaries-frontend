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

package utils

import controllers.auth.AuthenticatedRequest
import models.ActingAsAttorneyFor
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Request
import uk.gov.hmrc.domain.{SaUtr, TaxIdentifier, Uar}

trait AccountUtils {
  def getAccount(request: AuthenticatedRequest[_]): TaxIdentifier = request.agentRef.getOrElse(request.saUtr.get)
  //This warning is unchecked because we know that AuthorisedFor will only give us those accounts
  def getAccountId(request: AuthenticatedRequest[_]): String = (getAccount(request): @unchecked) match {
    case sa: SaUtr => sa.utr
    case ta: Uar => ta.uar
  }
  def isPortalUser(request: Request[_]): Boolean = request.session.get(utils.Globals.TAXS_USER_TYPE_KEY) == Some(utils.Globals.TAXS_PORTAL_REFERENCE)
  def isAgent(request: AuthenticatedRequest[_]): Boolean = request.agentRef.isDefined
}

trait AttorneyUtils{
  def getActingAsAttorneyFor(request: AuthenticatedRequest[_], forename: String, surname: String, utr: String): Option[ActingAsAttorneyFor] = {
    if(AccountUtils.isAgent(request)) Some(ActingAsAttorneyFor(Some(s"$forename $surname (${Messages("generic.utr_abbrev")}: $utr)"), Map())) else None
  }
}

trait Analytics{
  def getAnalyticsAttribute(request: AuthenticatedRequest[_], actingAttorney: Option[ActingAsAttorneyFor]): String = {
    actingAttorney.isDefined match {
      case true => Globals.TAXS_ANALYTICS_AGENT_ATTRIBUTE
      case false => if(AccountUtils.isPortalUser(request)) Globals.TAXS_ANALYTICS_PORTAL_USER_ATTRIBUTE else Globals.TAXS_ANALYTICS_TRANSITIONED_USER_ATTRIBUTE
    }
  }
}

object AccountUtils extends AccountUtils
object AttorneyUtils extends AttorneyUtils
object Analytics extends Analytics
