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

/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http:www.apache.org/licenses/LICENSE-2.0
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
import play.api.mvc.Session
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.{SaUtr, Uar}
import utils.TestConstants._
import utils.{AccountUtils, AgentTokenException, AuthorityUtils, BaseSpec}

class AccountUtilsSpec extends BaseSpec {

  val utr = testUtr
  val uar = testUar
  val nonMatchingUtr = testNonMatchingUtr

  val request = AuthenticatedRequest(
    "userId",
    None,
    Some(SaUtr(utr)),
    None,
    true,
    false,
    ConfidenceLevel.L50,
    fakeCredentials,
    FakeRequest())
  val agentRequest =
    AuthenticatedRequest(
      "userId",
      Some(Uar(uar)),
      Some(SaUtr(utr)),
      None,
      true,
      false,
      ConfidenceLevel.L50,
      fakeCredentials,
      FakeRequest())

  val agentToken = AgentToken(
    agentUar = uar,
    clientUtr = utr,
    timestamp = 0
  )

  "getAccount" must {

    "return SaUtr when an SA User request is passed" in {
      val result = AccountUtils.getAccount(request)
      result mustBe Some(SaUtr(utr))
    }

    "return Uar when an request having agentRef present" in {
      val result = AccountUtils.getAccount(agentRequest)
      result mustBe Some(Uar(uar))
    }

    "return None when account is SaUtr and Uar are None in request" in {
      val requestForNoneAccount = AuthenticatedRequest(
        "userId",
        None,
        None,
        None,
        true,
        false,
        ConfidenceLevel.L50,
        fakeCredentials,
        FakeRequest())
      val result = AccountUtils.getAccount(requestForNoneAccount)
      result mustBe None
    }
  }

  "getAccountIdForAudit" must {

    "return SaUtr when an SA User request is passed" in {
      val result = AccountUtils.getAccountIdForAudit(request)
      result mustBe utr
    }

    "return Uar when an request having agentRef present" in {
      val result = AccountUtils.getAccountIdForAudit(agentRequest)
      result mustBe uar
    }

    "return - when account is SaUtr and Uar are None in request" in {
      val requestForNoneAccount = AuthenticatedRequest(
        "userId",
        None,
        None,
        None,
        true,
        false,
        ConfidenceLevel.L50,
        fakeCredentials,
        FakeRequest())
      val result = AccountUtils.getAccountIdForAudit(requestForNoneAccount)
      result mustBe "-"
    }
  }

  "isAgent" must {

    "return false when an SA User request is passed" in {
      val result = AccountUtils.isAgent(request)
      result mustBe false
    }

    "return true when an request having agentRef present" in {
      val result = AccountUtils.isAgent(agentRequest)
      result mustBe true
    }

  }

}
