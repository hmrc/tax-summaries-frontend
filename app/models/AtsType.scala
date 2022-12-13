/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.libs.json._

sealed abstract class AtsType
object AtsType {

  implicit val writes = new Writes[AtsType] {
    override def writes(o: AtsType): JsValue =
      o match {
        case SA   => JsString("SA")
        case PAYE => JsString("PAYE")
        case _    => JsString("NoATS")
      }
  }

  implicit val reads = new Reads[AtsType] {
    override def reads(json: JsValue): JsResult[AtsType] =
      json match {
        case JsString("SA")   => JsSuccess(SA)
        case JsString("PAYE") => JsSuccess(PAYE)
        case _                => JsSuccess(NoATS)
      }
  }
}

case object SA extends AtsType
case object PAYE extends AtsType
case object NoATS extends AtsType
