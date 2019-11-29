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

package utils

import uk.gov.hmrc.domain.SaUtrGenerator
import view_models.{Amount, Rate, SavingsRates, SavingsTax, ScottishRates, ScottishTax, TotalIncomeTax}

import scala.util.Random

trait TestConstants {

  // We only want one test nino and utr throughout, therefore assign a value in the object declaration
  lazy val testUtr = new SaUtrGenerator().nextSaUtr.utr
  lazy val testUar = "V" + genRandNumString(4) + "H"
  lazy val testInvalidUtr = genRandNumString(4)
  lazy val testKey = genRandNumString(22)
  lazy val testOid = genRandNumString(12)
  lazy val testNonMatchingUtr = new SaUtrGenerator().nextSaUtr.utr

  def genRandNumString(length: Int) = Random.nextInt(9).toString * length

  val testTotalIncomeTax = TotalIncomeTax(
    year = 2014,
    utr = "",
    Amount.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    Amount.empty,
    ScottishTax.empty,
    Amount.empty,
    Amount.empty,
    SavingsTax.empty,
    "",
    Rate.empty,
    Rate.empty,
    Rate.empty,
    Rate.empty,
    Rate.empty,
    Rate.empty,
    Rate.empty,
    ScottishRates.empty,
    SavingsRates.empty,
    "Mr",
    "forename",
    "surname"
  )
}

object TestConstants extends TestConstants
