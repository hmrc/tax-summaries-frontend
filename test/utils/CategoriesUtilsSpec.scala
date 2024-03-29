/*
 * Copyright 2023 HM Revenue & Customs
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

import config.ApplicationConfig

class CategoriesUtilsSpec extends BaseSpec {

  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]

  "reorderCategories" must {
    "sort data given order from appConfig" in {

      val orderingList = List("A", "C", "B", "D")

      val spendData        = List("A" -> 1.5, "B" -> 1.20, "C" -> 1.20, "D" -> 0.5)
      val expectedResponse = List("A" -> 1.5, "C" -> 1.20, "B" -> 1.20, "D" -> 0.5)
      val result           = CategoriesUtils.reorderCategories(orderingList, spendData)

      result mustBe expectedResponse
    }
  }
}
