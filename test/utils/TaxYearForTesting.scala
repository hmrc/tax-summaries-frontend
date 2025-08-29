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

import models.AtsListData
import utils.TestConstants.testNino

trait TaxYearForTesting extends JsonUtil {
  protected val currentTaxYear: Int         = 2024
  protected val previousTaxYear: Int        = currentTaxYear - 1
  private val maxTaxYearsTobeDisplayed: Int = 4

  protected def generateSaAtsYearList(utr: String): AtsListData =
    AtsListData(
      utr = utr,
      taxPayer = Some(
        Map(
          "title"    -> "Mr",
          "forename" -> "forename",
          "surname"  -> "surname"
        )
      ),
      atsYearList = Some(Range.inclusive(currentTaxYear - (maxTaxYearsTobeDisplayed - 1), currentTaxYear).toList)
    )

  def atsData(taxYear: Int): String = loadAndReplace(
    "/json/ats-data.json",
    Map("<TAXYEAR>" -> taxYear.toString)
  )

  def govSpend(taxYear: Int): String = loadAndReplace(
    "/json/gov-spend.json",
    Map("$nino" -> testNino.nino, "<TAXYEAR>" -> taxYear.toString)
  )
  def payeAtsData: String            = loadAndReplace(
    "/json/paye-ats-data.json",
    Map(
      "<TAXYEAR-1>" -> previousTaxYear.toString,
      "<TAXYEAR-2>" -> currentTaxYear.toString
    )
  )

}
