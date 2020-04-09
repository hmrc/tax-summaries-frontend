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

import org.scalatestplus.play.PlaySpec
import utils.TestConstants

class GovernmentSpendSpec extends PlaySpec with TestConstants {

  "GovernmentSpend" when {

    "sortedSpendData is called" must {

      "filter out total government spend figure" in {
        fakeGovernmentSpend.sortedSpendData mustNot contain(govSpendTotalTuple)
      }

      "sort from highest percentage to lowest" in {
        fakeGovernmentSpend.sortedSpendData.map(_._2.percentage) mustBe expectedPercentageOrder
      }
    }

    "filteredDataWithHigherTransport" must {

      "sort transport above Public Order" in {
        fakeGovernmentSpend.filteredDataWithHigherTransport.map(_._1) mustBe expectedCategoryOrder
      }
    }

    "taxYearInterval" must {
      "return a string in the correct format" in {
        fakeGovernmentSpend.taxYearInterval mustBe "2018-19"
      }
    }
    "taxYearFrom" must {
      "return the previous year" in {
        fakeGovernmentSpend.taxYearFrom mustBe (fakeTaxYear - 1).toString
      }
    }
  }
}