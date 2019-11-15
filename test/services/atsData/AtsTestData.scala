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

package services.atsData

import models.{AtsData, DataHolder, UserData}
import view_models.Amount

object AtsTestData {

  val atsAllowancesData = AtsData(
    2019,
    Some("1111111111"),
    None,
    None,
    None,
    Some(
      DataHolder(
        Some(
          Map(
            "personal_tax_free_amount" -> Amount(100, "GBP"),
            "marriage_allowance_transferred_amount" -> Amount(200, "GBP"),
            "other_allowances_amount" -> Amount(300, "GBP"),
            "total_tax_free_amount" -> Amount(400, "GBP")
          )
        ),
        None,
        None
      )
    ),
    None,
    None,
    Some(
      UserData(
        Some(
          Map(
            "title" -> "Mr",
            "forename" -> "John",
            "surname" -> "Smith"
          )
        )
      )
    ),
    None
  )

}
