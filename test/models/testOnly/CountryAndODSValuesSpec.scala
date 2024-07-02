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

  "findDuplicateFields" must {
    "find duplicates when present" in {
      CountryAndODSValues.findDuplicateFields("""abc 1,122.33
bde 24.44
abc 424.44
bde 624.44
def 24.44""") mustBe Seq("abc", "bde")
    }
  }

  "stringToKeyValuePairs" must {
    "return map when valid string passed in, filtering out any commas" in {
      val odsValues = """abc 1,122.33
def 24.44"""
      CountryAndODSValues.stringToKeyValuePairs(odsValues) mustBe Map(
        "abc" -> "1122.33",
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

  "keyValuePairsToArray" must {
    "convert key value pairs correctly to array + return right" in {
      CountryAndODSValues.keyValuePairsToEitherSeqODSValue(
        Map(
          "abc" -> "33.44",
          "def" -> "24.44"
        )
      ) mustBe Right(
        Seq(
          OdsValue("abc", BigDecimal(33.44)),
          OdsValue("def", BigDecimal(24.44))
        )
      )
    }

    "return left when fields not convertable to big decimals" in {
      CountryAndODSValues.keyValuePairsToEitherSeqODSValue(
        Map(
          "abc" -> "33.44",
          "def" -> "invalid1",
          "ghi" -> "77.88",
          "ghi" -> "invalid2"
        )
      ) mustBe Left(Seq("def", "ghi"))
    }
  }

}
