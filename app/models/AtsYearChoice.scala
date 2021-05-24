/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.libs.json.{JsPath, JsString, JsValue, Json, Reads, Writes}
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class AtsYearChoice(atsType: AtsType, year: Int)

object AtsYearChoice {

  implicit val writes = new Writes[AtsYearChoice] {
    override def writes(o: AtsYearChoice): JsValue =
      Json.obj("atsType" -> JsString(o.atsType.name), "year" -> o.year)
  }

  implicit val reads: Reads[AtsYearChoice] = (
    (JsPath \ "atsType").read[String].map(AtsType.getByName) and
      (JsPath \ "year").read[Int]
  )(AtsYearChoice.apply _)

  def fromString(value: String): AtsYearChoice = {
    val json = Json.parse(value)
    Json.fromJson[AtsYearChoice](json).get
  }

  def toOptionString(choice: AtsYearChoice): Option[String] = Some(Json.stringify(Json.toJson(choice)))

  def toString(choice: AtsYearChoice): String = Json.stringify(Json.toJson(choice))
}
