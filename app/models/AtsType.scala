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

sealed abstract class AtsType(val name: String)
object AtsType {

  def getByName(name: String): AtsType = name match {
    case "SA"    => SA
    case "PAYE"  => PAYE
    case "NoATS" => NoATS
  }

  case object SA extends AtsType("SA")
  case object PAYE extends AtsType("PAYE")
  case object NoATS extends AtsType("NoATS")
}
