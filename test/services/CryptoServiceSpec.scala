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

package services

import java.util.Date

import controllers.FakeTaxsPlayApplication
import org.scalatest.concurrent.ScalaFutures
import play.utils.UriEncoding
import uk.gov.hmrc.crypto.{AesCrypto, PlainText}
import uk.gov.hmrc.play.test.UnitSpec
import utils.AgentTokenException
import utils.TestConstants._

class CryptoServiceSpec extends UnitSpec with FakeTaxsPlayApplication with ScalaFutures {

  class TestCrypt extends CryptoService {

    override lazy val key = testKey
    override lazy val tokenMaxAge: Int = 180

    val agentUar = testUar
    val clientUtr = testUtr
    val invalidUtr = testInvalidUtr
    val timestamp = new Date().getTime()

    val agentToken = AgentToken(
      agentUar,
      clientUtr,
      timestamp
    )

    val crypto = new AesCrypto {
      override protected val encryptionKey: String = key
    }

    // Common method for creating a valid encryped token
    def encryptToken(agent: String = agentUar, client: String = clientUtr, timestamp: Long = new Date().getTime()) = {
      val plain = s"$agent:$client:$timestamp"
      val encrypted = crypto.encrypt(PlainText(plain)).value
      UriEncoding.encodePathSegment(encrypted, "UTF-8")
    }
  }


  "getAgentToken" should {

    "return an AgentToken when the input is valid" in new TestCrypt {

      val result = getAgentToken(encryptToken(timestamp = timestamp))
      result shouldBe agentToken
    }

    "throw an AgentTokenException when an expired token is passed" in new TestCrypt {

      val token = encryptToken(timestamp = (new Date().getTime() - (tokenMaxAge * 1000)))

      val exception = intercept[AgentTokenException] {
        getAgentToken(token)
      }

      exception.message should include("Expired token")
    }

    "throw an exception when the token date is in the future" in new TestCrypt {

      val token = encryptToken(timestamp = (new Date().getTime() + (tokenMaxAge * 1000)))

      val exception = intercept[AgentTokenException] {
        getAgentToken(token)
      }

      exception.message should include("Expired token")
    }

    "throw an AgentTokenException when the agentToken is malformed" in new TestCrypt {

      val token = encryptToken(client = invalidUtr)

      val exception = intercept[AgentTokenException] {
        getAgentToken(token)
      }

      exception.message should include("Malformed token content")
    }

    "throw an AgentTokenException when the agentToken cannot be decryped" in new TestCrypt {

      val token = "loremipsumdolorsitamet"

      val exception = intercept[AgentTokenException] {
        getAgentToken(token)
      }

      exception.message should include("Cannot decrypt token")
    }
  }
}
