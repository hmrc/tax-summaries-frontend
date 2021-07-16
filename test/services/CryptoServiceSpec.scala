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

import models.AgentToken
import play.utils.UriEncoding
import uk.gov.hmrc.crypto.{AesCrypto, PlainText}
import utils.TestConstants._
import utils.{AgentTokenException, BaseSpec}

import java.util.Date

class CryptoServiceSpec extends BaseSpec {

  val maxAge = 180

  def sut = new CryptoService {

    override lazy val key = testKey
    override lazy val tokenMaxAge: Int = maxAge
  }

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
    override protected val encryptionKey: String = testKey
  }

  // Common method for creating a valid encryped token
  def encryptToken(agent: String = agentUar, client: String = clientUtr, timestamp: Long = new Date().getTime()) = {
    val plain = s"$agent:$client:$timestamp"
    val encrypted = crypto.encrypt(PlainText(plain)).value
    UriEncoding.encodePathSegment(encrypted, "UTF-8")
  }

  "getAgentToken" should {

    "return an AgentToken when the input is valid" in {

      val result = sut.getAgentToken(encryptToken(timestamp = timestamp))
      result shouldBe agentToken
    }

    "throw an AgentTokenException when an expired token is passed" in {

      val token = encryptToken(timestamp = (new Date().getTime() - (maxAge * 1000)))

      val exception = intercept[AgentTokenException] {
        sut.getAgentToken(token)
      }

      exception.message should include("Expired token")
    }

    "throw an exception when the token date is in the future" in {

      val token = encryptToken(timestamp = (new Date().getTime() + (maxAge * 1000)))

      val exception = intercept[AgentTokenException] {
        sut.getAgentToken(token)
      }

      exception.message should include("Expired token")
    }

    "throw an AgentTokenException when the agentToken is malformed" in {

      val token = encryptToken(client = invalidUtr)

      val exception = intercept[AgentTokenException] {
        sut.getAgentToken(token)
      }

      exception.message should include("Malformed token content")
    }

    "throw an AgentTokenException when the agentToken cannot be decryped" in {

      val token = "loremipsumdolorsitamet"

      val exception = intercept[AgentTokenException] {
        sut.getAgentToken(token)
      }

      exception.message should include("Cannot decrypt token")
    }
  }
}
