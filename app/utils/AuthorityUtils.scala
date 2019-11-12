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
import services.AgentToken
import uk.gov.hmrc.domain.{SaUtr, TaxIdentifier, Uar}
import uk.gov.hmrc.play.frontend.auth.connectors.domain._

object AuthorityUtils extends AuthorityUtils

trait AuthorityUtils {

  def saAuthority(id: String, utr: String): Authority =
    Authority(
      s"/auth/oid/$id",
      Accounts(
        sa = Some(SaAccount(s"/sa/individual/$utr", SaUtr(utr)))
      ),
      None,
      None,
      CredentialStrength.Weak, //this class may need to be refactored as these methods are only used in test only
      ConfidenceLevel.L50,
      None,
      None,
      None,
      ""
    )

  def taxsAgentAuthority(id: String, uar: String): Authority =
    Authority(
      s"/auth/oid/$id",
      Accounts(
        taxsAgent = Some(TaxSummariesAgentAccount(s"/taxsagent/$uar", Uar(uar)))
      ),
      None,
      None,
      CredentialStrength.Weak,////this class may need to be refactored as these methods are only used in test only
      ConfidenceLevel.L50,
      None,
      None,
      None,
      ""
    )

  def checkUtr(utr: String, agentToken: Option[AgentToken])(implicit request: AuthenticatedRequest[_]): Boolean = {
    (AccountUtils.getAccount(request), agentToken) match {
      case (agentAccount, None) if (AccountUtils.isAgent(request)) =>
        true
      case (agentAccount, Some(agentToken)) if (AccountUtils.isAgent(request)) =>
        SaUtr(utr) == SaUtr(agentToken.clientUtr)
      case (account: SaAccount, _) =>
        SaUtr(utr) == account.utr
    }
  }

  def checkUtr(utr: Option[String], agentToken: Option[AgentToken])(implicit request: AuthenticatedRequest[_]): Boolean = {
    utr.fold { false } { checkUtr(_, agentToken) }
  }

  def getRequestedUtr(account: TaxIdentifier, agentToken: Option[AgentToken] = None): SaUtr = {
    //This warning is unchecked because we know that AuthorisedFor will only give us those accounts
    (account: @unchecked) match {
      case taxsAgent: Uar =>
        agentToken.fold {
          throw AgentTokenException("Token is empty")
        } { agentToken =>
          if (taxsAgent.uar == Uar(agentToken.agentUar)) {
            SaUtr(agentToken.clientUtr)
          } else {
            throw AgentTokenException(s"Incorrect agent UAR: ${taxsAgent.uar}, ${agentToken.agentUar}")
          }
        }
      case sa: SaUtr => sa
    }
  }
}
