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

package models

import utils.BaseSpec

class AtsYearChoiceSpec extends BaseSpec {

  val choice        = AtsYearChoice(SA, currentTaxYear)
  val correctString = s"{\"atsType\":\"SA\",\"year\":$currentTaxYear}"

  "AtsYearChoice" when {
    "fromString" must {

      "return the correct object from correct string" in {
        val actual = AtsYearChoice.fromString(Some(correctString))
        actual mustBe choice
      }

      "throw exception with an incorrect string" in {
        val exception = intercept[Exception] {
          AtsYearChoice.fromString(Some("{\"incorrect\":12}"))
        }

        exception.getMessage must include("Could not parse json")
      }
    }

    "toOptionString" must {

      "return json" in {
        val actual = AtsYearChoice.toOptionString(choice)
        actual mustBe Some(Some(correctString))
      }
    }

    "toString" must {

      "return json" in {
        val actual = AtsYearChoice.toString(choice)
        actual mustBe correctString
      }
    }

  }
}
