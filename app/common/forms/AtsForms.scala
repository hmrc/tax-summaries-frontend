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

package common.forms

import com.google.inject.Inject
import common.models.AtsYearChoice
import common.utils.TaxYearUtil
import play.api.Logging
import play.api.data.Form
import play.api.data.Forms.*
import play.api.data.validation.{Constraint, Invalid, Valid}

import scala.util.{Failure, Success, Try}

class AtsForms @Inject() (taxYearUtil: TaxYearUtil) extends Logging {

  private val choices = List("SA", "PAYE", "NoATS")

  private val yearChoiceJsonConstraint: Constraint[Option[String]] = Constraint { submittedValue =>
    submittedValue
      .map { value =>
        value.split("-").toList match {
          case atsType :: taxYear :: Nil =>
            val isTaxYearValid = Try(taxYear.toInt) match {
              case Success(value) => taxYearUtil.isValidTaxYear(value)
              case Failure(_)     => false
            }
            if (isTaxYearValid && choices.contains(atsType)) { Valid }
            else { Invalid("ats.select_tax_year.required") }

          case _ => Invalid("ats.select_tax_year.required")
        }
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
