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

import config.ApplicationConfig
import org.joda.time.{DateTime, Interval}
import play.api.libs.json.Json
import play.utils.UriEncoding
import uk.gov.hmrc.crypto.{AesCrypto, Crypted, PlainText}
import utils.AgentTokenException

import scala.util.matching.Regex

object CryptoService extends CryptoService {
  override val key = ApplicationConfig.encryptionKey
  override val tokenMaxAge = ApplicationConfig.encryptionTokenMaxAge
}

trait CryptoService {

  def key : String
  def tokenMaxAge: Int

  protected def aesCrypto = new AesCrypto {
    val encryptionKey = key
  }

  def getAgentToken(token: String): AgentToken = {
    val decryptedToken =
      decryptToken(UriEncoding.decodePath(token, "UTF-8")).value
    val agentToken = parseToken(decryptedToken)

    validateTimestamp(agentToken)
  }

  protected def decryptToken(token: String): PlainText = {
    try {
      aesCrypto.decrypt(Crypted(token))
    } catch {
      case exception: Exception =>
        throw AgentTokenException("Cannot decrypt token: '" + token + "'", exception)
    }
  }

  protected def parseToken(decryptedToken: String): AgentToken = {
    val tokenPattern = new Regex("\\w+:\\d{10}:\\d+")

    if (!tokenPattern.pattern.matcher(decryptedToken).matches) {
      throw AgentTokenException("Malformed token content: '" + decryptedToken + "'")
    }

    val splitToken = decryptedToken.split(':')

    AgentToken(agentUar = splitToken(0), clientUtr = splitToken(1), timestamp = splitToken(2).toLong)
  }

  protected def validateTimestamp(agentToken: AgentToken) = {
    val timeStamp = agentToken.timestamp
    val validInterval = getTokenValidityInterval(timeStamp)
    val timeNow = new DateTime

    if (!validInterval.contains(timeNow)) {
      throw AgentTokenException(s"Expired token. Time now : $timeNow valid interval : $validInterval timestamp : $timeStamp")
    }

    agentToken
  }

  protected def getTokenValidityInterval(timeStamp: Long) = {
    val tokenDateTime = new DateTime(timeStamp)
    val tokenExpiryDateTime = tokenDateTime.plusSeconds(tokenMaxAge)
    new Interval(tokenDateTime, tokenExpiryDateTime)
  }
}

case class AgentToken(agentUar: String, clientUtr: String, timestamp: Long)

object AgentToken {
  implicit val formats = Json.format[AgentToken]
}
