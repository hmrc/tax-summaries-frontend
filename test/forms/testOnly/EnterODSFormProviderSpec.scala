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

package forms.testOnly

import modules.testOnly.CountryAndODSValues
import play.api.data.FormError
import utils.BaseSpec

class EnterODSFormProviderSpec extends BaseSpec {

  private val form = new EnterODSFormProvider().apply()

  private val odsValues = "abc"

  "form" must {
    "must bind valid data (England)" in {
      val expectedResult = CountryAndODSValues("0001", odsValues)

      val data = Map(
        "country"   -> "0001",
        "odsValues" -> odsValues
      )

      val result = form.bind(data)
      result.value mustBe Some(expectedResult)
    }

    "must bind valid data (Scotland)" in {
      val expectedResult = CountryAndODSValues("0003", odsValues)

      val data = Map(
        "country"   -> "0003",
        "odsValues" -> odsValues
      )

      val result = form.bind(data)
      result.value mustBe Some(expectedResult)
    }

    "must bind valid data (Wales)" in {
      val expectedResult = CountryAndODSValues("0002", odsValues)

      val data = Map(
        "country"   -> "0002",
        "odsValues" -> odsValues
      )

      val result = form.bind(data)
      result.value mustBe Some(expectedResult)
    }

    "must display errors for missing country" in {
      val data = Map(
        "odsValues" -> odsValues
      )

      val result = form.bind(data)
      result.errors mustBe Seq(FormError("country", List("error.required")))
    }

    "must display errors for invalid country" in {
      val data = Map(
        "country"   -> "ABC",
        "odsValues" -> odsValues
      )

      val result = form.bind(data)
      result.errors mustBe Seq(FormError("country", List("Invalid country specified")))
    }

    "must display errors for missing odsValues" in {
      val data = Map(
        "country" -> "0001"
      )

      val result = form.bind(data)
      result.errors mustBe Seq(FormError("odsValues", List("error.required")))
    }
  }
}
