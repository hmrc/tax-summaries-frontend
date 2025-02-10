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

package utils

import com.google.inject.Inject
import controllers.auth.requests.AuthenticatedRequest
import models.AgentToken
import uk.gov.hmrc.domain.{SaUtr, TaxIdentifier, Uar}

class AuthorityUtils @Inject() () {

  def checkUtr(utr: String, agentToken: Option[AgentToken])(implicit request: AuthenticatedRequest[_]): Boolean =
    (AccountUtils.getAccount(request), agentToken) match {
      case (_, None) if AccountUtils.isAgent(request)             =>
        true
      case (_, Some(agentToken)) if AccountUtils.isAgent(request) =>
        SaUtr(utr) == SaUtr(agentToken.clientUtr)
      case (account: SaUtr, _)                                    =>
        SaUtr(utr) == account
    }

  def checkUtr(utr: Option[String], agentToken: Option[AgentToken])(implicit
    request: AuthenticatedRequest[_]
  ): Boolean =
    utr.fold(false)(checkUtr(_, agentToken))

  def getRequestedUtr(account: TaxIdentifier, agentToken: Option[AgentToken] = None): SaUtr =
    // This warning is unchecked because we know that AuthorisedFor will only give us those accounts
    (account: @unchecked) match {
      case taxsAgent: Uar =>
        agentToken.fold {
          throw AgentTokenException("Token is empty")
        } { agentToken =>
          if (taxsAgent == Uar(agentToken.agentUar)) {
            SaUtr(agentToken.clientUtr)
          } else {
            throw AgentTokenException(s"Incorrect agent UAR: ${taxsAgent.uar}, ${agentToken.agentUar}")
          }
        }
      case sa: SaUtr      => sa
    }
}
