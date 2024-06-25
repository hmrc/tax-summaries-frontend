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

import modules.testOnly.TaxYearAndUTR
import play.api.data.FormError
import utils.BaseSpec

class EnterSearchFormProviderSpec extends BaseSpec {

  private val form = new EnterSearchFormProvider().apply()

  private val utr = "000000000"

  "form" must {
    "must bind valid data" in {
      val expectedResult = TaxYearAndUTR(taxYear, utr)

      val data = Map(
        "taxYear" -> taxYear.toString,
        "utr"     -> utr
      )

      val result = form.bind(data)
      result.value mustBe Some(expectedResult)
    }

    "must display errors for missing tax year" in {
      val data = Map(
        "utr" -> "999999"
      )

      val result = form.bind(data)
      result.errors mustBe Seq(FormError("taxYear", List("No tax year specified")))
    }

    "must display errors for non-numeric tax year" in {
      val data = Map(
        "taxYear" -> "ABC",
        "utr"     -> "999999"
      )

      val result = form.bind(data)
      result.errors mustBe Seq(FormError("taxYear", List("Non numeric tax year specified")))
    }

    "must display errors for whole number as tax year" in {
      val data = Map(
        "taxYear" -> "223.3",
        "utr"     -> "999999"
      )

      val result = form.bind(data)
      result.errors mustBe Seq(FormError("taxYear", List("Non numeric tax year specified")))
    }

    "must display errors for missing utr" in {
      val data = Map(
        "taxYear" -> taxYear.toString
      )

      val result = form.bind(data)
      result.errors mustBe Seq(FormError("utr", List("error.required")))
    }
  }
}
