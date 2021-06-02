/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.libs.json.{JsString, JsSuccess}
import uk.gov.hmrc.play.test.UnitSpec

class AtsYearChoiceSpec extends UnitSpec {

  val choice = AtsYearChoice(SA, 2015)
  val correctString = "{\"atsType\":\"SA\",\"year\":2015}"

  "AtsYearChoice" when {
    "fromString" should {

      "return the correct object from correct string" in {
        val actual = AtsYearChoice.fromString(correctString)
        actual shouldBe choice
      }

      "throw exception with an incorrect string" in {
        val exception = intercept[Exception] {
          AtsYearChoice.fromString("{\"incorrect\":12}")
        }

        exception.getMessage should include("Could not parse json")
      }
    }

    "toOptionString" should {

      "return json" in {
        val actual = AtsYearChoice.toOptionString(choice)
        actual shouldBe Some(correctString)
      }
    }

    "toString" should {

      "return json" in {
        val actual = AtsYearChoice.toString(choice)
        actual shouldBe correctString
      }
    }

  }
}
