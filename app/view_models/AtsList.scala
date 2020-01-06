/*
 * Copyright 2020 HM Revenue & Customs
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

import play.api.libs.json.{Format, Json}
import utils.GenericViewModel

case class AtsList(utr: String, forename: String, surname: String, yearList: List[TaxYearEnd])
    extends GenericViewModel {
  def getDescendingYearList = yearList.reverse
}

case class TaxYearEnd(year: Option[String]) extends GenericViewModel {
  def taxYearPeriod: String = (year.get.toInt - 1) + "/" + year.get
}

object TaxYearEnd {
  implicit val formats = Json.format[TaxYearEnd]
  implicit val optionFormats = Format.optionWithNull[TaxYearEnd]
}
