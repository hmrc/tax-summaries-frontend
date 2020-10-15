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

package models

import play.api.libs.json.Json

case class PayeAtsData(
  taxYear: Int,
  income_tax: Option[DataHolder],
  summary_data: Option[DataHolder],
  income_data: Option[DataHolder],
  allowance_data: Option[DataHolder],
  gov_spending: Option[GovernmentSpendingOutputWrapper]) {

  //using scottish_income_tax to identify WelshTaxPayer is not a bug and we expect the field to be changed in the backend in 2021.
  def isWelshTaxPayer: Boolean =
    income_data
      .flatMap(incomeData => incomeData.payload.flatMap(_.get("scottish_income_tax"))) isDefined
}

object PayeAtsData {
  implicit val reads = Json.reads[PayeAtsData]
}
