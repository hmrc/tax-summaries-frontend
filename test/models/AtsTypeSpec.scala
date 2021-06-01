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

class AtsTypeSpec extends UnitSpec {
  "AtsType" when {

    "writes" should {

      "return the correct JS string for SA" in {
        val expectedJsStringSA = JsString("SA")
        val actual = AtsType.writes.writes(SA)
        actual shouldBe expectedJsStringSA
      }

      "return the correct JS string for PAYE" in {
        val expectedJsStringSA = JsString("PAYE")
        val actual = AtsType.writes.writes(PAYE)
        actual shouldBe expectedJsStringSA
      }

      "return the correct JS string for NoATS" in {
        val expectedJsStringSA = JsString("NoATS")
        val actual = AtsType.writes.writes(NoATS)
        actual shouldBe expectedJsStringSA
      }
    }

    "AtsType" when {

      "reads" should {

        "return the correct JS success for SA" in {
          val expected = JsSuccess(SA)
          val actual = AtsType.reads.reads(JsString("SA"))
          actual shouldBe expected
        }

        "return the correct JS success for PAYE" in {
          val expected = JsSuccess(PAYE)
          val actual = AtsType.reads.reads(JsString("PAYE"))
          actual shouldBe expected
        }

        "return the correct JS success for NoATS" in {
          val expected = JsSuccess(NoATS)
          val actual = AtsType.reads.reads(JsString("NoATS"))
          actual shouldBe expected
        }
      }

    }
  }
}
