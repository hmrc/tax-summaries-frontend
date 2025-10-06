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

package utils

import com.google.inject.Inject
import config.ApplicationConfig
import controllers.auth.requests.AuthenticatedRequest
import models.{ErrorResponse, MissingTaxYear}

class TaxYearUtil @Inject() (
  appConfig: ApplicationConfig
) {

  private val taxYearPattern = """((19|[2-9][0-9])[\d]{2})""".r

  def isValidTaxYear(taxYear: Int): Boolean = {
    val taxYears           = Seq(appConfig.taxYearSA, appConfig.taxYearPAYE, appConfig.taxYearGovSpend)
    val (minYear, maxYear) = (taxYears.min, taxYears.max)
    !(taxYear > maxYear || taxYear <= minYear - appConfig.maxTaxYearsTobeDisplayed)
  }

  def extractTaxYear(implicit request: AuthenticatedRequest[_]): Either[ErrorResponse, Int] =
    request.getQueryString("taxYear") match {
      case Some(taxYearPattern(year, _)) =>
        Right(year.toInt)
      case _                             =>
        Left(MissingTaxYear)
    }

  def isYearListComplete(years: Seq[Int]): Boolean = {
    val taxYears           = Seq(appConfig.taxYearSA, appConfig.taxYearPAYE)
    val (minYear, maxYear) = (taxYears.min, taxYears.max)
    val yearFrom           = minYear - appConfig.maxTaxYearsTobeDisplayed
    val yearTo             = maxYear
    val yrs                = years.distinct.sorted
    val expTotalYears      = yearTo - yearFrom
    yrs.size == expTotalYears && yrs.headOption.contains(yearFrom + 1) && yrs.lastOption.contains(yearTo)
  }
}
