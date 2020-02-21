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

package services.atsData

import models._
import view_models.{Amount, Rate}

object PayeAtsTestData {

  val govSpendingData = PayeAtsData(
    2019,
    None,
    None,
    None,
    None,
    Some(
      GovernmentSpendingOutputWrapper(
        2019,
        Some(
          Map(
            "UkContributionToEuBudget" -> SpendData(Amount(19.00, "GBP"), 1.00),
            "Welfare" -> SpendData(Amount(451.00, "GBP"), 23.5),
            "GovernmentAdministration" -> SpendData(Amount(40.00, "GBP"), 2.10),
            "Education" -> SpendData(Amount(226.00, "GBP"), 11.80),
            "StatePensions" -> SpendData(Amount(246.00, "GBP"), 12.80),
            "NationalDebtInterest" -> SpendData(Amount(98.00, "GBP"), 5.10),
            "Defence" -> SpendData(Amount(102.00, "GBP"), 5.30),
            "PublicOrderAndSafety" -> SpendData(Amount(83.00, "GBP"), 4.30),
            "Transport" -> SpendData(Amount(83.00, "GBP"), 4.30),
            "BusinessAndIndustry" -> SpendData(Amount(69.00, "GBP"), 3.60),
            "Culture" -> SpendData(Amount(29.00, "GBP"), 1.50),
            "HousingAndUtilities" -> SpendData(Amount(31.00, "GBP"), 1.60),
            "Environment" -> SpendData(Amount(29.00, "GBP"), 1.50),
            "OverseasAid" ->  SpendData(Amount(23.00, "GBP"), 1.20),
            "Health" -> SpendData(Amount(388.00, "GBP"), 20.2)
          )
        ),
        Amount(200,"GBP"),
        None
      )
    )
  )
}
