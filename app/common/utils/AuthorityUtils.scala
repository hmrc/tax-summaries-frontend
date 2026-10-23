/*
 * Copyright 2026 HM Revenue & Customs
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

import com.google.inject.{Inject, Singleton}
import common.models.AgentToken
import uk.gov.hmrc.domain.{SaUtr, TaxIdentifier, Uar}

@Singleton
class AuthorityUtils @Inject() {
  def getRequestedUtr(account: TaxIdentifier, agentToken: Option[AgentToken] = None): SaUtr =
    // This warning is unchecked because we know that AuthorisedFor will only give us those accounts
    (account: @unchecked) match {
      case taxsAgent @ Uar(_) =>
        agentToken.fold {
          throw AgentTokenException("Token is empty")
        } { agentToken =>
          if (taxsAgent == Uar(agentToken.agentUar)) {
            SaUtr(agentToken.clientUtr)
          } else {
            throw AgentTokenException(s"Incorrect agent UAR: ${taxsAgent.uar}, ${agentToken.agentUar}")
          }
        }
      case sa @ SaUtr(_)      => sa
    }
}
