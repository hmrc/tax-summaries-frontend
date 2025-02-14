/*
 * Copyright 2025 HM Revenue & Customs
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

import uk.gov.hmrc.crypto.{AesCrypto, PlainText}

import java.net.URLEncoder
import java.time.Instant

object LoginPage {

  private val encKey = "1111111111111111111111"

  // KxF3antRTEtdaFgpwbmoISnwJDJRvyl0NAnCRwa3SB5EIrpF0IMS/wZwQnvsprKx

  private val crypto = new AesCrypto {
    override protected val encryptionKey: String = encKey
  }

  def agentToken(utr: String) = {
    val token =
      URLEncoder.encode(crypto.encrypt(PlainText(s"V3264H:$utr:" + (Instant.now.toEpochMilli))).value, "UTF-8")
    token
  }
}
