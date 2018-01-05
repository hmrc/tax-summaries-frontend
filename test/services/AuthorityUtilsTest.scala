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

package services

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.test.UnitSpec
import utils.{AgentTokenException, AccountUtils, AuthorityUtils}
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import utils.TestConstants._

class AuthorityUtilsTest extends UnitSpec with MockitoSugar {

  class TestService extends AuthorityUtils {

    val utr = testUtr
    val uar = testUar
    val nonMatchingUtr = testNonMatchingUtr
    val user = User(AuthorityUtils.saAuthority(testOid, utr))
    val agentUser = User(AuthorityUtils.taxsAgentAuthority(testOid, uar))
    val account = AccountUtils.getAccount(user)
    val agentAccount = AccountUtils.getAccount(agentUser)

    val agentToken = AgentToken(
      agentUar = uar,
      clientUtr = utr,
      timestamp = 0
    )
  }

  "checkUtr" should {

    "return true when an SA User has a matching utr and no agent token is passed" in new TestService {
      val result = checkUtr(utr, None)(user)
      result shouldBe true
    }

    "return true when an SA User has a matching utr and an agent token is passed" in new TestService {
      val result = checkUtr(utr, Some(agentToken))(user)
      result shouldBe true
    }

    "return false when an SA User has a non-matching utr and no agent token is passed" in new TestService {
      val result = checkUtr(nonMatchingUtr, Some(agentToken))(user)
      result shouldBe false
    }

    "return false when an SA User has a non-matching utr and an agent token is passed" in new TestService {
      val result = checkUtr(nonMatchingUtr, Some(agentToken))(user)
      result shouldBe false
    }

    "return true when the user is an Agent user and the agentToken.clientUtr matches" in new TestService {
      val result = checkUtr(utr, Some(agentToken))(agentUser)
      result shouldBe true
    }

    "return true when the user is an Agent user and there is no agentToken" in new TestService {
      val result = checkUtr(utr, None)(agentUser)
      result shouldBe true
    }

    "return false when the user is an Agent and the agentToken.clientUtr does not match" in new TestService {
      val result = checkUtr(nonMatchingUtr, Some(agentToken))(agentUser)
      result shouldBe false
    }

    "return false when the utr is None" in new TestService {
      val result = checkUtr(None, None)(user)
      result shouldBe false
    }

    "return true when the utr is Some(_) and the criteria should match" in new TestService {
      val result = checkUtr(Some(utr), None)(user)
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