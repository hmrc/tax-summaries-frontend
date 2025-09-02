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
import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import play.twirl.api.TwirlHelperImports.twirlJavaCollectionToScala

import scala.util.*

class PayeConfig @Inject() ()(implicit val appConfig: ApplicationConfig) {
  val payeYear: Int        = appConfig.taxYearPAYE
  protected val configPath = "paye.conf"

  private def getKeyForCurrentYear(keyForTaxYear: Int => String) = {
    val key            = keyForTaxYear(payeYear)
    val config: Config = ConfigFactory.load(configPath)
    Try(config.getStringList(key).toList) match {
      case Success(ls)                         => ls
      case Failure(exception: ConfigException) =>
        throw new RuntimeException(s"No keys specified for $payeYear for $key")
      case Failure(exception)                  => throw exception
    }
  }

  lazy val scottishTaxBandKeys: List[String] =
    getKeyForCurrentYear(taxYear => s"scottishTaxBandKeys.$taxYear")

  lazy val ukTaxBandKeys: List[String] = getKeyForCurrentYear(taxYear => s"ukTaxBandKeys.$taxYear")

  lazy val adjustmentsKeys: List[String] = getKeyForCurrentYear(taxYear => s"adjustmentsKeys.$taxYear")

}
