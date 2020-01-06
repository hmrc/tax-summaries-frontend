/*
 * Copyright 2020 HM Revenue & Customs
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

package config

import uk.gov.hmrc.crypto.{CompositeSymmetricCrypto, PlainText}
import uk.gov.hmrc.http.CoreGet
import uk.gov.hmrc.play.partials.FormPartialRetriever

object AppFormPartialRetriever extends AppFormPartialRetriever {
  override lazy val httpGet: CoreGet = WSGet
  override lazy val sessionCrypto: CompositeSymmetricCrypto = TAXSSessionCookieCrypto.crypto
}

trait AppFormPartialRetriever extends FormPartialRetriever {

  val httpGet: CoreGet
  val sessionCrypto: CompositeSymmetricCrypto

  override lazy val crypto: String => String =
    str => sessionCrypto.encrypt(PlainText(str)).value
}
