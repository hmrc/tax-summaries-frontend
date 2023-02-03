/*
 * Copyright 2023 HM Revenue & Customs
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

import com.google.inject.Inject
import com.typesafe.config.{Config, ConfigFactory}
import play.twirl.api.TwirlHelperImports.twirlJavaCollectionToScala

class PayeConfig @Inject() ()(implicit val appConfig: ApplicationConfig) {
  val payeYear: Int        = appConfig.taxYear
  protected val configPath = "paye.conf"

  lazy val scottishTaxBandKeys: List[String] = {
    val config: Config = ConfigFactory.load(configPath)
    val taxBands       = Option(config.getStringList(s"scottishTaxBandKeys.$payeYear")).map(_.toList)
    taxBands.getOrElse(throw new RuntimeException(s"No scottish tax band keys specified for $payeYear"))
  }

  lazy val ukTaxBandKeys: List[String] = {
    val config: Config = ConfigFactory.load(configPath)
    val taxBands       = Option(config.getStringList(s"ukTaxBandKeys.$payeYear")).map(_.toList)
    taxBands.getOrElse(throw new RuntimeException(s"No uk tax band keys specified for $payeYear"))
  }

  lazy val adjustmentsKeys: List[String] = {
    val config: Config = ConfigFactory.load(configPath)
    val adjustments    = Option(config.getStringList(s"adjustmentsKeys.$payeYear")).map(_.toList)
    adjustments.getOrElse(throw new RuntimeException(s"No adjust keys specified for $payeYear"))
  }
}
