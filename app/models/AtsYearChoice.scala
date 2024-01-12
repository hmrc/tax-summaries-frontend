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

package models

import play.api.i18n.Messages
import play.api.libs.json.{Json, OFormat}

case class AtsYearChoice(atsType: AtsType, year: Int) {
  def getLabel(implicit messages: Messages): String = atsType match {
    case SA   => messages("ats.select_tax_year.sa.label", (year - 1).toString, s"$year")
    case PAYE => messages("ats.select_tax_year.paye.label", (year - 1).toString, s"$year")
    case _    => messages("ats.select_tax_year.no_ats.label", (year - 1).toString, s"$year")
  }
}

object AtsYearChoice {

  implicit val formats: OFormat[AtsYearChoice] = Json.format[AtsYearChoice]

  def fromString(value: Option[String]): AtsYearChoice =
    value match {
      case Some(v) =>
        val json = Json.parse(v)
        Json
          .fromJson[AtsYearChoice](json)
          .getOrElse(
            throw new Exception(s"Could not parse json $value to AtsYearChoice")
          )
      case _       => throw new Exception(s"Could not parse json $value to AtsYearChoice")
    }

  def toOptionString(choice: AtsYearChoice): Option[Option[String]] = Some(Some(Json.stringify(Json.toJson(choice))))
  def toString(choice: AtsYearChoice): String                       = Json.stringify(Json.toJson(choice))
}
