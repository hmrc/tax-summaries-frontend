/*
 * Copyright 2019 HM Revenue & Customs
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

import java.text.SimpleDateFormat

import org.joda.time.LocalDate
import play.api.libs.json.Json

case class AtsData(
  taxYear: Int,
  utr: Option[String],
  nino: Option[String],
  income_tax: Option[DataHolder],
  summary_data: Option[DataHolder],
  income_data: Option[DataHolder],
  allowance_data: Option[DataHolder],
  capital_gains_data: Option[DataHolder],
  gov_spending: Option[GovernmentSpendingOutputWrapper],
  taxPayerData: Option[UserData],
  errors: Option[IncomingAtsError])

object AtsData {
  implicit val formats = Json.format[AtsData]
}

case class TupleDate(day: String, month: String, year: String) {
  lazy val localDate = new LocalDate(year.toInt, month.toInt, day.toInt)

  lazy val date = localDate.toDate

  def toString(format: String) = new SimpleDateFormat(format).format(date)
}
