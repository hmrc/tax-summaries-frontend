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

package common.services

import common.models.requests.AuthenticatedRequest
import common.models.{AgentToken, requests}
import common.utils.TestConstants.*
import common.utils.{AccountUtils, AgentTokenException, AuthorityUtils, BaseSpec}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.{SaUtr, TaxIdentifier, Uar}

class AuthorityUtilsSpec extends BaseSpec {

  class TestService extends AuthorityUtils {

    val utr: String            = testUtr
    val uar: String            = testUar
    val nonMatchingUtr: String = testNonMatchingUtr

    protected val request: AuthenticatedRequest[AnyContentAsEmpty.type] = requests.AuthenticatedRequest(
      userId = "userId",
      agentRef = None,
      saUtr = Some(SaUtr(utr)),
      nino = None,
      isAgentActive = false,
      confidenceLevel = ConfidenceLevel.L50,
      credentials = fakeCredentials,
      request = FakeRequest()
    )
    val agentRequest: AuthenticatedRequest[AnyContentAsEmpty.type]      =
      requests.AuthenticatedRequest(
        userId = "userId",
        agentRef = Some(Uar(uar)),
        saUtr = Some(SaUtr(utr)),
        nino = None,
        isAgentActive = false,
        confidenceLevel = ConfidenceLevel.L50,
        credentials = fakeCredentials,
        request = FakeRequest()
      )

    val account: Option[TaxIdentifier]      = AccountUtils.getOptionAccount(request)
    val agentAccount: Option[TaxIdentifier] = AccountUtils.getOptionAccount(agentRequest)

    val agentToken: AgentToken = AgentToken(
      agentUar = uar,
      clientUtr = utr,
      timestamp = 0
    )
  }

  "getRequestedUtr" must {

    "return the utr when user account with no agent token" in new TestService {
      val result: SaUtr = getRequestedUtr(account, None)
      result mustBe SaUtr(utr)
    }

    "return the utr when user account with agent token" in new TestService {
      val result: SaUtr = getRequestedUtr(account, Some(agentToken))
      result mustBe SaUtr(utr)
    }

    "throw AgentTokenException when agent account with no agent token" in new TestService {
      val exception: AgentTokenException = intercept[AgentTokenException] {
        getRequestedUtr(agentAccount, None)
      }
      exception.message mustBe "Token is empty"
    }

    "return the client utr when agent account with valid agent token" in new TestService {
      val result: SaUtr = getRequestedUtr(agentAccount, Some(agentToken))
      result mustBe SaUtr(utr)
    }

    "return the client utr when agent account with invalid agent token" in new TestService {

      override val agentToken: AgentToken = AgentToken(
        agentUar = nonMatchingUtr,
        clientUtr = utr,
        timestamp = 0
      )

      val exception: AgentTokenException = intercept[AgentTokenException] {
        getRequestedUtr(agentAccount, Some(agentToken))
      }

      exception.message must include("Incorrect agent UAR")
    }
  }
}
