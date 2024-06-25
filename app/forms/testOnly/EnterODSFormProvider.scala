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

package forms.testOnly

import forms.mappings.{Constraints, Mappings}
import modules.testOnly.CountryAndODSValues
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.{Constraint, Invalid, Valid}
import services.testOnly.ODSValuesConverter

import javax.inject.Inject

class EnterODSFormProvider @Inject() (odsValuesConverter: ODSValuesConverter) extends Mappings with Constraints {

  private val validCountries = Set("0001", "0002", "0003")

  private val constraintCountry: Constraint[String] = Constraint {
    case country if validCountries.contains(country) => Valid
    case _                                           => Invalid("Invalid country specified")
  }

  def apply(validOdsFieldNames: Seq[String]): Form[CountryAndODSValues] = {
    val constraintOdsValues: Constraint[CountryAndODSValues] = Constraint { countryAndODSValues =>
      val unrecognisedFields =
        odsValuesConverter.toKeyValuePairs(countryAndODSValues.odsValues).keys.toSeq.diff(validOdsFieldNames)
      if (unrecognisedFields.nonEmpty) {
        Valid
      } else {
        Invalid(s"Unrecognised field names: $unrecognisedFields")
      }
    }

    Form(
      mapping(
        "country"   -> text().verifying(constraintCountry),
        "odsValues" -> text()
      )(CountryAndODSValues.apply)(CountryAndODSValues.unapply).verifying(constraintOdsValues)
    )
  }
}