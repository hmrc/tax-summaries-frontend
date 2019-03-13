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

import play.api.i18n.Messages
import play.api.mvc.Request
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{SaAccount, TaxSummariesAgentAccount, Account}
import uk.gov.hmrc.play.frontend.auth.{ActingAsAttorneyFor, AuthContext => User}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

trait AccountUtils {
  def getAccount(user: User): Account = user.principal.accounts.taxsAgent.getOrElse(user.principal.accounts.sa.get)
  //This warning is unchecked because we know that AuthorisedFor will only give us those accounts
  def getAccountId(user: User): String = (getAccount(user): @unchecked) match {
    case sa: SaAccount => sa.utr.utr
    case ta: TaxSummariesAgentAccount => ta.uar.uar
  }
  def isPortalUser(request: Request[_]): Boolean = request.session.get(utils.Globals.TAXS_USER_TYPE_KEY) == Some(utils.Globals.TAXS_PORTAL_REFERENCE)
  def isAgent(user: User): Boolean = user.principal.accounts.taxsAgent.isDefined
}

trait AttorneyUtils{
  def getActingAsAttorneyFor(user: User, forename: String, surname: String, utr: String): Option[ActingAsAttorneyFor] = {
    if(AccountUtils.isAgent(user)) Some(ActingAsAttorneyFor(Some(s"$forename $surname (${Messages("generic.utr_abbrev")}: $utr)"), Map())) else None
  }
}

trait Analytics{
  def getAnalyticsAttribute(request: Request[_], actingAttorney: Option[ActingAsAttorneyFor]): String = {
    actingAttorney.isDefined match {
      case true => Globals.TAXS_ANALYTICS_AGENT_ATTRIBUTE
      case false => if(AccountUtils.isPortalUser(request)) Globals.TAXS_ANALYTICS_PORTAL_USER_ATTRIBUTE else Globals.TAXS_ANALYTICS_TRANSITIONED_USER_ATTRIBUTE
    }
  }
}

object AccountUtils extends AccountUtils
object AttorneyUtils extends AttorneyUtils
object Analytics extends Analytics
