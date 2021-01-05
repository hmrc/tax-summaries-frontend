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

package view_models

import org.scalatest.prop.PropertyChecks
import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec

import scala.util.matching.UnanchoredRegex

class AmountSpec extends UnitSpec with PropertyChecks {

  val testCurrency: String = "GBP"

  "Amount" when {

    "toString is called" should {

      "format with two decimal places" in {
        val testValue: BigDecimal = 1000.00
        val testAmount: Amount = new Amount(testValue, testCurrency)
        testAmount.toString() shouldEqual "1,000"
      }

      "formats the decimal places to zero" in {
        val testValue: BigDecimal = 1000.63
        val testAmount: Amount = new Amount(testValue, testCurrency)
        testAmount.toString() shouldEqual "1,000"
      }

      "produces a comma for thousands" in {
        val testValue: BigDecimal = 1000.00
        val testAmount: Amount = new Amount(testValue, testCurrency)
        testAmount.toString() shouldEqual "1,000"
      }
    }

    "parsed as JSON" should {
      "carry out JSON transformation" in {
        val amountText = """{"amount":1.0,"currency":"GBP"}"""
        val jsonFromText = Json.parse(amountText)
        val amountObject = Amount(1.0, testCurrency)
        val jsonFromObject = Json.toJson(amountObject)
        assert(jsonFromText equals jsonFromObject)
      }
    }

    "toHalfRoundedUpAmount is called" should {
      "can round up if testValue is Greater than or 1000.5" in {
        val testValue: BigDecimal = 1000.5
        val testAmount: Amount = new Amount(testValue, testCurrency)
        testAmount.toHalfRoundedUpAmount shouldEqual "1,001"
      }

      "can round down if testValue is less than 1000.4" in {
        val testValue: BigDecimal = 1000.4
        val testAmount: Amount = new Amount(testValue, testCurrency)
        testAmount.toHalfRoundedUpAmount shouldEqual "1,000"
      }
    }

    "isZero is called" should {

      "return true" when {
        "the amount is zero" in {
          val amount = Amount(0, testCurrency)
          amount.isZero shouldBe true
        }
      }

      "return false" when {
        "the amount is greater than zero" in {
          forAll { bd: BigDecimal =>
            whenever(bd > 0) {
              val amount = Amount(bd, testCurrency)
              amount.isZero shouldBe false
            }
          }
        }

        "the amount is less than zero" in {
          forAll { bd: BigDecimal =>
            whenever(bd < 0) {
              val amount = Amount(bd, testCurrency)
              amount.isZero shouldBe false
            }
          }
        }
      }
    }

    "isZeroOrLess is called" should {

      "return true" when {
        "the amount is zero" in {
          val amount = Amount(0, testCurrency)
          amount.isZeroOrLess shouldBe true
        }

        "the amount is less than zero" in {
          forAll { bd: BigDecimal =>
            whenever(bd < 0) {
              val amount = Amount(bd, testCurrency)
              amount.isZeroOrLess shouldBe true
            }
          }
        }
      }

      "return false" when {
        "the amount is greater than zero" in {
          forAll { bd: BigDecimal =>
            whenever(bd > 0) {
              val amount = Amount(bd, testCurrency)
              amount.isZeroOrLess shouldBe false
            }
          }
        }
      }
    }

    "nonZero is called" should {

      "return true" when {
        "the amount is not zero" in {
          forAll { bd: BigDecimal =>
            whenever(bd != 0) {
              val amount = Amount(bd, testCurrency)
              amount.nonZero shouldBe true
            }
          }
        }
      }

      "return false" when {
        "the amount is zero" in {
          val amount = Amount(0, testCurrency)
          amount.nonZero shouldBe false
        }
      }
    }

    "toCreditString is called" should {
      "can round up if it's a credit" in {
        val testValue: BigDecimal = 1000.01
        val testAmount: Amount = new Amount(testValue, testCurrency)
        testAmount.toCreditString shouldEqual "1,001"
      }
    }

    "toTwoDecimalString is called" should {
      "have a comma and two decimal places for government spend values" in {
        val testValue: BigDecimal = 1000.01
        val testAmount: Amount = new Amount(testValue, testCurrency)
        testAmount.toTwoDecimalString shouldEqual "1,000.01"
      }

      "not round up government spend values" in {
        val testValue: BigDecimal = 1000.99
        val testAmount: Amount = new Amount(testValue, testCurrency)
        testAmount.toTwoDecimalString shouldEqual "1,000.99"
      }

      "round down government spend values after the second decimal place" in {
        val testValue: BigDecimal = 1000.018796799
        val testAmount: Amount = new Amount(testValue, testCurrency)
        testAmount.toTwoDecimalString shouldEqual "1,000.01"
      }
    }

    "toHundredthsString is called" should {
      "give the amount as a string with the amount multiplied by 100" in {
        val amt = Amount(BigDecimal(123.45678), testCurrency)
        amt.toHundredthsString shouldBe "12,345.67"
      }
    }

    "unary '-' is called" should {
      "turn the amount negative" in {

        forAll { bd: BigDecimal =>
          whenever(bd > 1) {
            val result = -Amount(bd, testCurrency)
            result.amount shouldBe -bd
          }
        }
      }
    }
  }
}
