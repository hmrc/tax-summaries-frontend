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

class SwapDataUtilsSpec extends UnitSpec {

  "SwapDataUtils" should {

    "swap the data for SA" in {

      val collection = Map("A" -> 1.5, "B" -> 1.5, "C" -> 1.5, "D" -> 1.5).toList

      val expectedResponse = Map("A" -> 1.5, "C" -> 1.5, "B" -> 1.5, "D" -> 1.5).toList

      val result = SwapDataUtils.swapDataForSa(collection, "B", "C")

      result shouldBe expectedResponse
    }

    "swap the data for PAYE" in {

      val collection = List(
        SpendRow("A", SpendData(Amount(15, "GBP"), 1.5)),
        SpendRow("B", SpendData(Amount(15, "GBP"), 1.5)),
        SpendRow("C", SpendData(Amount(15, "GBP"), 1.5)),
        SpendRow("D", SpendData(Amount(15, "GBP"), 1.5))
      )

      val expectedResponse = List(
        SpendRow("A", SpendData(Amount(15, "GBP"), 1.5)),
        SpendRow("C", SpendData(Amount(15, "GBP"), 1.5)),
        SpendRow("B", SpendData(Amount(15, "GBP"), 1.5)),
        SpendRow("D", SpendData(Amount(15, "GBP"), 1.5))
      )

      val result =
        SwapDataUtils.swapDataForPaye(collection, "B", "C")

      result shouldBe expectedResponse
    }
  }

}
