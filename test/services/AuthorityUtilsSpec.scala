/*
 * Copyright 2021 HM Revenue & Customs
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

package services

import controllers.auth.AuthenticatedRequest
import models.AgentToken
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.{SaUtr, Uar}
import uk.gov.hmrc.play.test.UnitSpec
import utils.{AccountUtils, AgentTokenException, AuthorityUtils}
import utils.TestConstants._

class AuthorityUtilsSpec extends UnitSpec with MockitoSugar {

  class TestService extends AuthorityUtils {

    val utr = testUtr
    val uar = testUar
    val nonMatchingUtr = testNonMatchingUtr

    val request = AuthenticatedRequest("userId", None, Some(SaUtr(utr)), None, None, None, None, FakeRequest())
    val agentRequest =
      AuthenticatedRequest("userId", Some(Uar(uar)), Some(SaUtr(utr)), None, None, None, None, FakeRequest())

    val account = AccountUtils.getAccount(request)
    val agentAccount = AccountUtils.getAccount(agentRequest)

    val agentToken = AgentToken(
      agentUar = uar,
      clientUtr = utr,
      timestamp = 0
    )
  }

  "checkUtr" should {

    "return true when an SA User has a matching utr and no agent token is passed" in new TestService {
      val result = checkUtr(utr, None)(request)
      result shouldBe true
    }

    "return true when an SA User has a matching utr and an agent token is passed" in new TestService {
      val result = checkUtr(utr, Some(agentToken))(request)
      result shouldBe true
    }

    "return false when an SA User has a non-matching utr and no agent token is passed" in new TestService {
      val result = checkUtr(nonMatchingUtr, None)(request)
      result shouldBe false
    }

    "return false when an SA User has a non-matching utr and an agent token is passed" in new TestService {
      val result = checkUtr(nonMatchingUtr, Some(agentToken))(request)
      result shouldBe false
    }

    "return true when the user is an Agent user and the agentToken.clientUtr matches" in new TestService {
      val result = checkUtr(utr, Some(agentToken))(agentRequest)
      result shouldBe true
    }

    "return true when the user is an Agent user and there is no agentToken" in new TestService {
      val result = checkUtr(utr, None)(agentRequest)
      result shouldBe true
    }

    "return false when the user is an Agent and the agentToken.clientUtr does not match" in new TestService {
      val result = checkUtr(nonMatchingUtr, Some(agentToken))(agentRequest)
      result shouldBe false
    }

    "return false when the utr is None" in new TestService {
      val result = checkUtr(None, None)(request)
      result shouldBe false
    }

    "return true when the utr is Some(_) and the criteria should match" in new TestService {
      val result = checkUtr(Some(utr), None)(request)
      result shouldBe true
    }
  }

  "getRequestedUtr" should {

    "return the utr when user account with no agent token" in new TestService {
      val result = getRequestedUtr(account, None)
      result shouldBe SaUtr(utr)
    }

    "return the utr when user account with agent token" in new TestService {
      val result = getRequestedUtr(account, Some(agentToken))
      result shouldBe SaUtr(utr)
    }

    "throw AgentTokenException when agent account with no agent token" in new TestService {
      val exception = intercept[AgentTokenException] {
        getRequestedUtr(agentAccount, None)
      }
      exception.message shouldBe "Token is empty"
    }

    "return the client utr when agent account with valid agent token" in new TestService {
      val result = getRequestedUtr(agentAccount, Some(agentToken))
      result shouldBe SaUtr(utr)
    }

    "return the client utr when agent account with invalid agent token" in new TestService {

      override val agentToken = AgentToken(
        agentUar = nonMatchingUtr,
        clientUtr = utr,
        timestamp = 0
      )

      val exception = intercept[AgentTokenException] {
        getRequestedUtr(agentAccount, Some(agentToken))
      }

      exception.message should include("Incorrect agent UAR")
    }
  }
}
