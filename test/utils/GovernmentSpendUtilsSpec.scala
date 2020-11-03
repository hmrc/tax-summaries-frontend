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

package utils

import models.SpendData
import uk.gov.hmrc.play.test.UnitSpec
import view_models.Amount
import view_models.paye.SpendRow

class GovernmentSpendUtilsSpec extends UnitSpec {

  "GovernmentSpendUtils" should {

    "reorder data based on provided categories for SA" in {

      val categories = Map("Welfare" -> 23.4, "Environment" -> 5.5, "Culture" -> 5.5, "Transport" -> 3.5).toList

      val expectedBody = Map("Welfare" -> 23.4, "Culture" -> 5.5, "Environment" -> 5.5, "Transport" -> 3.5).toList

      val result = GovernmentSpendUtils.reorderDataBasedOnCategories(categories, "Culture", "Environment")

      result shouldBe expectedBody
    }

    "reorder data based on provided categories for PAYE" in {

      val categories = List(
        SpendRow("Welfare", SpendData(Amount(451, "GBP"), 23.5)),
        SpendRow("Transport", SpendData(Amount(45, "GBP"), 3.5)),
        SpendRow("PublicOrderAndSafety", SpendData(Amount(45, "GBP"), 3.5)),
        SpendRow("overseas_aid", SpendData(Amount(25, "GBP"), 1.5))
      )

      val expectedBody = List(
        SpendRow("Welfare", SpendData(Amount(451, "GBP"), 23.5)),
        SpendRow("PublicOrderAndSafety", SpendData(Amount(45, "GBP"), 3.5)),
        SpendRow("Transport", SpendData(Amount(45, "GBP"), 3.5)),
        SpendRow("overseas_aid", SpendData(Amount(25, "GBP"), 1.5))
      )

      val result =
        GovernmentSpendUtils.reorderPayeDataBasedOnCategories(categories, "Transport", "PublicOrderAndSafety")

      result shouldBe expectedBody
    }
  }

}
