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
    "Value is positive" must {
      val amountValue = Amount(BigDecimal(44.55), "GBP")
      "return correctly when spokenMinus is false and twoDecimalPlaces is false" in {
        val result: String = view(
          amount = amountValue,
          spokenMinus = false,
          twoDecimalPlaces = false
        ).body
        result mustBe "&pound;44"
      }
      "return correctly when spokenMinus is false and twoDecimalPlaces is true" in {
        val result: String = view(
          amount = amountValue,
          spokenMinus = false,
          twoDecimalPlaces = true
        ).body
        result mustBe "&pound;44.55"
      }
      "return correctly when spokenMinus is true and twoDecimalPlaces is false" in {
        val result: String = view(
          amount = amountValue,
          spokenMinus = true,
          twoDecimalPlaces = false
        ).body
        result mustBe "&pound;44"
      }
      "return correctly when spokenMinus is true and twoDecimalPlaces is true" in {
        val result: String = view(
          amount = amountValue,
          spokenMinus = true,
          twoDecimalPlaces = true
        ).body
        result mustBe "&pound;44"
      }
    }

    "Value is negative" must {
      val amountValue = Amount(BigDecimal(-44.55), "GBP")
      "return correctly when spokenMinus is false and twoDecimalPlaces is false" in {
        val result: String = view(
          amount = amountValue,
          spokenMinus = false,
          twoDecimalPlaces = false
        ).body
        result mustBe "&minus;&nbsp;&pound;44"
      }
      "return correctly when spokenMinus is false and twoDecimalPlaces is true" in {
        val result: String = view(
          amount = amountValue,
          spokenMinus = false,
          twoDecimalPlaces = true
        ).body
        result mustBe "&minus;&nbsp;&pound;44.55"
      }
      "return correctly when spokenMinus is true and twoDecimalPlaces is false" in {
        val result: String = view(
          amount = amountValue,
          spokenMinus = true,
          twoDecimalPlaces = false
        ).body
        result mustBe "minus &pound;44"
      }
      "return correctly when spokenMinus is true and twoDecimalPlaces is true" in {
        val result: String = view(
          amount = amountValue,
          spokenMinus = true,
          twoDecimalPlaces = true
        ).body
        result mustBe "minus &pound;44.55"
      }
    }

  }

}
