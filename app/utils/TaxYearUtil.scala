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
import models.{ErrorResponse, InvalidTaxYear}

class TaxYearUtil @Inject() (
  appConfig: ApplicationConfig
) {

  private val taxYearPattern = """((19|[2-9][0-9])[\d]{2})""".r

  def isValidTaxYear(taxYear: Int): Boolean =
    !(taxYear > appConfig.taxYear || taxYear <= (appConfig.taxYear - appConfig.maxTaxYearsTobeDisplayed))

  def extractTaxYear(implicit request: AuthenticatedRequest[_]): Either[ErrorResponse, Int] =
    request.getQueryString("taxYear") match {
      case Some(taxYearPattern(year, _)) =>
        Right(year.toInt)
      case _                             =>
        Left(InvalidTaxYear)
    }
}
