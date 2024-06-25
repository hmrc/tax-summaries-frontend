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

package models.testOnly

import play.api.libs.json._

case class OdsValue(fieldName: String, amount: Double)

case class SAODSModel(utr: String, taxYear: Int, country: String, odsValues: List[OdsValue])

object OdsValue {
  implicit val format: OFormat[OdsValue] = Json.format[OdsValue]
}

object SAODSModel {
  implicit val format: OFormat[SAODSModel] = Json.format[SAODSModel]
}
