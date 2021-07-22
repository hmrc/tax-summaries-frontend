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

package utils

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class BigDecimalUtilsSpec extends AnyWordSpec with BigDecimalUtils with Matchers with ScalaCheckPropertyChecks {

  "BigDecimalUtils" must {

    "return true for === and false for !==" when {

      "two big decimals are exactly equal" in {

        forAll { bd: BigDecimal =>
          assert(bd === bd)
          assert(!(bd !== bd))
        }
      }
    }

    "return false for === and true for !==" when {

      "two big decimals are not the same" in {

        forAll { (bd1: BigDecimal, bd2: BigDecimal) =>
          assert(!(bd1 === bd2))
          assert(bd1 !== bd2)
        }
      }
    }
  }
}
