/*
 * Copyright 2024 HM Revenue & Customs
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

package testOnly.forms

import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.{Constraint, Invalid, Valid}
import testOnly.forms.mappings.Mappings
import testOnly.models.CountryAndODSValues
import testOnly.models.CountryAndODSValues._

class EnterODSFormProvider extends Mappings {

  private val validCountries = Set("0001", "0002", "0003")

  private val constraintCountry: Constraint[String] = Constraint {
    case country if validCountries.contains(country) => Valid
    case _                                           => Invalid("Invalid country specified")
  }

  def apply(validOdsFieldNames: Seq[String]): Form[CountryAndODSValues] = {
    val constraintOdsValues: Constraint[String] = Constraint { odsValues =>
      val duplicatedFields = findDuplicateFields(odsValues)
      if (duplicatedFields.nonEmpty) {
        Invalid(s"Duplicated field values: ${duplicatedFields.sorted.mkString(", ")}")
      } else {
        val keyValuePairs      = stringToKeyValuePairs(odsValues)
        val unrecognisedFields = keyValuePairs.keys.toSeq.diff(validOdsFieldNames)
        if (unrecognisedFields.isEmpty) {
          keyValuePairsToEitherSeqODSValue(keyValuePairs) match {
            case Left(invalidFields) => Invalid(s"Invalid field values: ${invalidFields.sorted.mkString(", ")}")
            case Right(_)            => Valid
          }
        } else {
          Invalid(s"Unrecognised field names: ${unrecognisedFields.sorted.mkString(", ")}")
        }
      }
    }

    Form(
      mapping(
        "country"   -> text().verifying(constraintCountry),
        "odsValues" -> text().verifying(constraintOdsValues)
      )((country, odsValues) => CountryAndODSValues(country, stringToKeyValuePairs(odsValues)))(countryAndODSValues =>
        Some((countryAndODSValues.country, keyValuePairsToString(countryAndODSValues.odsValues)))
      )
    )
  }
}
