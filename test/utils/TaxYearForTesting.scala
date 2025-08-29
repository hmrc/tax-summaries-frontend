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
  // The latest tax year (end year) available in ATS. This will be either last
  // tax year or last tax year - 1, depending on time of year.
  protected val currentTaxYear: Int         = 2024
  protected val previousTaxYear: Int        = currentTaxYear - 1
  private val maxTaxYearsTobeDisplayed: Int = 4

  protected def listOfTaxYears(noOfYears: Int = maxTaxYearsTobeDisplayed): List[Int] =
    Range.inclusive(currentTaxYear - (noOfYears - 1), currentTaxYear).toList

  // Dummy data for endpoint: /taxs/<UTR>/<TAX-YEAR>/<NUMBER-OF-YEARS>/ats-list
  protected def atsList(utr: String): AtsListData =
    AtsListData(
      utr = utr,
      taxPayer = Some(
        Map(
          "title"    -> "Mr",
          "forename" -> "forename",
          "surname"  -> "surname"
        )
      ),
      atsYearList = Some(listOfTaxYears(maxTaxYearsTobeDisplayed))
    )

  // Dummy data for endpoint: /taxs/<UTR>/<TAX-YEAR>/ats-data
  protected def atsData(taxYear: Int): String = loadAndReplace(
    "/json/ats-data.json",
    Map("<TAXYEAR>" -> taxYear.toString)
  )

  // Dummy data for endpoint: /taxs/<NINO>/<TAX-YEAR>/paye-ats-data
  protected def payAtsData(taxYear: Int): String = loadAndReplace(
    "/json/paye-ats-data.json",
    Map("$nino" -> testNino.nino, "<TAXYEAR>" -> taxYear.toString)
  )

  // Dummy data for endpoint: /taxs/<NINO>/<START-TAX-YEAR>/<END-TAX-YEAR>/paye-ats-data
  protected def payeAtsDataForYearRange(noOfYears: Int = maxTaxYearsTobeDisplayed): String =
    "[" + listOfTaxYears(noOfYears).foldLeft("") { (acc, year) =>
      acc + (if (acc.isEmpty) {
               ""
             } else {
               ","
             }) + payAtsData(year)
    } + "]"
}
