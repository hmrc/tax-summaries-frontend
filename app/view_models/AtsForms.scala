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

package view_models

import com.google.inject.Inject
import config.ApplicationConfig
import models.AtsYearChoice
import play.api.Logging
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid}

class AtsForms @Inject() (appConfig: ApplicationConfig) extends Logging {

  private val yearChoiceJsonConstraint: Constraint[Option[String]] = Constraint { submittedValue =>
    submittedValue
      .map { value =>
        val data = value.split("-")
        if (data.size == 2) {
          (data.head, data(1)) match {
            case (_, year)
                if year.toInt < (appConfig.taxYear - appConfig.maxTaxYearsTobeDisplayed) || year.toInt > appConfig.taxYear =>
              Invalid("ats.select_tax_year.required")
            case ("SA", _)    => Valid
            case ("PAYE", _)  => Valid
            case ("NoATS", _) => Valid
            case _            => Invalid("ats.select_tax_year.required")
          }
        } else Invalid("ats.select_tax_year.required")
      }
      .getOrElse(Invalid("ats.select_tax_year.required"))
  }

  val yearChoice = "year"

  val atsYearFormMapping: Form[AtsYearChoice] = Form(
    mapping(
      yearChoice -> optional(text)
        .verifying("ats.select_tax_year.required", _.nonEmpty)
        .verifying(yearChoiceJsonConstraint)
    )(
      AtsYearChoice.fromFormString
    )(AtsYearChoice.toOptionString)
  )
}
