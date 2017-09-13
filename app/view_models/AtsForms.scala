/*
 * Copyright 2017 HM Revenue & Customs
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

import utils.TAXSEnums.YearEnum
import utils.validation.ConstraintUtil.CompulsoryEnumMappingParameter
import utils.validation.ErrorMessagesUtilAPI._
import utils.validation.MappingUtilAPI._
import play.api.data.Forms._
import play.api.data.Form

object AtsForms {

  private val year_compulsory = {
    val question = CompulsoryEnumMappingParameter(
      simpleFieldIsEmptyConstraintParameter("year", "ats.select_tax_year.required"),
      YearEnum
    )
    compulsoryEnum(question)
  }

  val atsYearFormMapping = Form(mapping(
    "year" -> year_compulsory)(TaxYearEnd.apply)(TaxYearEnd.unapply))
}
