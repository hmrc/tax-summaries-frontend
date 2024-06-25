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

package models.testOnly

import utils.BaseSpec

class CountryAndODSValuesSpec extends BaseSpec {
  private val newLine = sys.props("line.separator")
  "stringToKeyValuePairs" must {
    "return map when valid string passed in" in {
      val odsValues = """abc 22.33
def 24.44"""
      CountryAndODSValues.stringToKeyValuePairs(odsValues) mustBe Map(
        "abc" -> "22.33",
        "def" -> "24.44"
      )
    }

    "return map when valid string passed in with missing second value" in {
      val odsValues = """abc 
def 24.44"""
      CountryAndODSValues.stringToKeyValuePairs(odsValues) mustBe Map(
        "abc" -> "",
        "def" -> "24.44"
      )
    }
  }

  "keyValuePairsToString" must {
    "convert map correctly to string when valid map" in {
      CountryAndODSValues.keyValuePairsToString(
        Map(
          "abc" -> "33.44",
          "def" -> "24.44"
        )
      ) mustBe s"""abc 33.44${newLine}def 24.44"""
    }

    "convert map correctly to string when valid mapdd" in {
      CountryAndODSValues.keyValuePairsToString(
        Map(
          "abc" -> "",
          "def" -> "24.44"
        )
      ) mustBe s"""abc${newLine}def 24.44"""
    }
  }

}
