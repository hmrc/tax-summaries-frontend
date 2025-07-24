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

package views.includes

import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import utils.TestConstants
import view_models.*
import views.ViewSpecBase
import views.html.includes.CurrencyAmountView

class CurrencyAmountViewSpec extends ViewSpecBase with TestConstants with ScalaCheckDrivenPropertyChecks {
  lazy val view: CurrencyAmountView = inject[CurrencyAmountView]
  "view" when {
    "Value is zero"     must {
      val amountValue = Amount(BigDecimal(0.00), "GBP")
      "return correctly when spoken is false and twoDecimalPlaces is false" in {
        val result: String = view(
          amount = amountValue,
          spoken = false,
          twoDecimalPlaces = false
        ).body
        result.stripTrailing() mustBe "&pound;0"
      }
      "return correctly when spoken is false and twoDecimalPlaces is true" in {
        val result: String = view(
          amount = amountValue,
          spoken = false,
          twoDecimalPlaces = true
        ).body
        result.stripTrailing() mustBe "&pound;0.00"
      }
      "return correctly when spoken is true and twoDecimalPlaces is false" in {
        val result: String = view(
          amount = amountValue,
          spoken = true,
          twoDecimalPlaces = false
        ).body
        result.stripTrailing() mustBe "&pound;0"
      }
      "return correctly when spoken is true and twoDecimalPlaces is true" in {
        val result: String = view(
          amount = amountValue,
          spoken = true,
          twoDecimalPlaces = true
        ).body
        result.stripTrailing() mustBe "&pound;0.00"
      }
    }
    "Value is positive" must {
      val amountValue = Amount(BigDecimal(1044.55), "GBP")
      "return correctly when spoken is false and twoDecimalPlaces is false" in {
        val result: String = view(
          amount = amountValue,
          spoken = false,
          twoDecimalPlaces = false
        ).body
        result.stripTrailing() mustBe "&pound;1,044"
      }
      "return correctly when spoken is false and twoDecimalPlaces is true" in {
        val result: String = view(
          amount = amountValue,
          spoken = false,
          twoDecimalPlaces = true
        ).body
        result.stripTrailing() mustBe "&pound;1,044.55"
      }
      "return correctly when spoken is true and twoDecimalPlaces is false" in {
        val result: String = view(
          amount = amountValue,
          spoken = true,
          twoDecimalPlaces = false
        ).body
        result.stripTrailing() mustBe "&pound;1,044"
      }
      "return correctly when spoken is true and twoDecimalPlaces is true" in {
        val result: String = view(
          amount = amountValue,
          spoken = true,
          twoDecimalPlaces = true
        ).body
        result.stripTrailing() mustBe "&pound;1,044.55"
      }
    }

    "Value is negative" must {
      val amountValue = Amount(BigDecimal(-1044.55), "GBP")
      "return correctly when spoken is false and twoDecimalPlaces is false" in {
        val result: String = view(
          amount = amountValue,
          spoken = false,
          twoDecimalPlaces = false
        ).body
        result.stripTrailing() mustBe "&minus;&nbsp;&pound;1,044"
      }
      "return correctly when spoken is false and twoDecimalPlaces is true" in {
        val result: String = view(
          amount = amountValue,
          spoken = false,
          twoDecimalPlaces = true
        ).body
        result.stripTrailing() mustBe "&minus;&nbsp;&pound;1,044.55"
      }
      "return correctly when spoken is true and twoDecimalPlaces is false" in {
        val result: String = view(
          amount = amountValue,
          spoken = true,
          twoDecimalPlaces = false
        ).body
        result.stripTrailing() mustBe "minus &pound;1,044"
      }
      "return correctly when spoken is true and twoDecimalPlaces is true" in {
        val result: String = view(
          amount = amountValue,
          spoken = true,
          twoDecimalPlaces = true
        ).body
        result.stripTrailing() mustBe "minus &pound;1,044.55"
      }
    }

  }

}
