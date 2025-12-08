/*
 * Copyright 2023 HM Revenue & Customs
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

package common.utils

import common.models.ActingAsAttorneyFor
import common.models.requests.AuthenticatedRequest
import play.api.i18n.Messages
import play.api.mvc.Request
import uk.gov.hmrc.domain.{SaUtr, TaxIdentifier, Uar}

trait AccountUtils {
  def getAccount(request: AuthenticatedRequest[_]): TaxIdentifier =
    request.agentRef.getOrElse(request.saUtr.getOrElse(SaUtr("")))
  // This warning is unchecked because we know that AuthorisedFor will only give us those accounts
  def getAccountId(request: AuthenticatedRequest[_]): String      = (getAccount(request): @unchecked) match {
    case sa: SaUtr => sa.utr
    case ta: Uar   => ta.uar
  }
  def isPortalUser(request: Request[_]): Boolean                  =
    request.session.get(common.utils.Globals.TAXS_USER_TYPE_KEY).contains(common.utils.Globals.TAXS_PORTAL_REFERENCE)
  def isAgent(request: AuthenticatedRequest[_]): Boolean          = request.agentRef.isDefined
}

trait AttorneyUtils {
  def getActingAsAttorneyFor(request: AuthenticatedRequest[_], forename: String, surname: String, utr: String)(implicit
    messages: Messages
  ): Option[ActingAsAttorneyFor] =
    if (AccountUtils.isAgent(request))
      Some(ActingAsAttorneyFor(Some(s"$forename $surname (${messages("generic.utr_abbrev")}: $utr)"), Map()))
    else None
}

object AccountUtils extends AccountUtils
object AttorneyUtils extends AttorneyUtils
