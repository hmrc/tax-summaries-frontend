/*
 * Copyright 2025 HM Revenue & Customs
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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.libs.json.Json
import utils.TestConstants.inject

class AmountSpec extends AnyWordSpec with Matchers with ScalaCheckDrivenPropertyChecks {

  val testCurrency: String = "GBP"

  "Amount" when {

    "toString is called" must {

      "format with two decimal places" in {
        val testValue: BigDecimal = 1000.00
        val testAmount: Amount    = new Amount(testValue, testCurrency)
        testAmount.toString() mustEqual "1,000"
      }

      "formats the decimal places to zero" in {
        val testValue: BigDecimal = 1000.63
        val testAmount: Amount    = new Amount(testValue, testCurrency)
        testAmount.toString() mustEqual "1,000"
      }

      "produces a comma for thousands" in {
        val testValue: BigDecimal = 1000.00
        val testAmount: Amount    = new Amount(testValue, testCurrency)
        testAmount.toString() mustEqual "1,000"
      }
    }

    "renderCurrencyValueAsHtml is called with spoken = true" must {
      implicit val messagesApi: MessagesApi = inject[MessagesApi]
      implicit val messages: Messages       = MessagesImpl(Lang("en"), messagesApi)

      "format positive amount correctly as pounds and pence" in {
        val testValue: BigDecimal = 1000.44
        val testAmount: Amount    = new Amount(testValue, testCurrency)
        testAmount.renderCurrencyValueAsHtml(spoken = true) mustEqual "&pound;1,000.44"
      }

      "format positive amount of £1.44 correctly as pound and pence" in {
        val testValue: BigDecimal = 1.44
        val testAmount: Amount    = new Amount(testValue, testCurrency)
        testAmount.renderCurrencyValueAsHtml(spoken = true) mustEqual "&pound;1.44"
      }

      "format negative amount correctly as pounds and pence" in {
        val testValue: BigDecimal = -1000.44
        val testAmount: Amount    = new Amount(testValue, testCurrency)
        testAmount.renderCurrencyValueAsHtml(spoken = true) mustEqual "minus &pound;1,000.44"
      }
      "format negative amount with > 2 decimal places correctly as pounds and pence" in {
        val testValue: BigDecimal = -1000.445
        val testAmount: Amount    = new Amount(testValue, testCurrency)
        testAmount.renderCurrencyValueAsHtml(spoken = true) mustEqual "minus &pound;1,000.44"
      }

      "format positive amount correctly as pounds" in {
        val testValue: BigDecimal = 1000.44
        val testAmount: Amount    = new Amount(testValue, testCurrency)
        testAmount.renderCurrencyValueAsHtml(poundsOnly = true, spoken = true) mustEqual "&pound;1000"
      }
      "format negative amount correctly as pounds" in {
        val testValue: BigDecimal = -1000.44
        val testAmount: Amount    = new Amount(testValue, testCurrency)
        testAmount.renderCurrencyValueAsHtml(poundsOnly = true, spoken = true) mustEqual "minus &pound;1000"
      }

    }
    "renderCurrencyValueAsHtml is called with spoken = false" must {
      implicit val messagesApi: MessagesApi = inject[MessagesApi]
      implicit val messages: Messages       = MessagesImpl(Lang("en"), messagesApi)

      "format positive amount correctly as pounds and pence" in {
        val testValue: BigDecimal = 1000.44
        val testAmount: Amount    = new Amount(testValue, testCurrency)
        testAmount.renderCurrencyValueAsHtml() mustEqual "&pound;1,000.44"
      }

      "format positive amount with > 2 decimal places correctly as pounds and pence" in {
        val testValue: BigDecimal = 1000.445
        val testAmount: Amount    = new Amount(testValue, testCurrency)
        testAmount.renderCurrencyValueAsHtml() mustEqual "&pound;1,000.44"
      }

      "format negative amount correctly as pounds and pence" in {
        val testValue: BigDecimal = -1000.44
        val testAmount: Amount    = new Amount(testValue, testCurrency)
        testAmount.renderCurrencyValueAsHtml() mustEqual "&minus;&nbsp;&pound;1,000.44"
      }

      "format positive amount correctly as pounds" in {
        val testValue: BigDecimal = 1000.44
        val testAmount: Amount    = new Amount(testValue, testCurrency)
        testAmount.renderCurrencyValueAsHtml(poundsOnly = true) mustEqual "&pound;1000"
      }
      "format negative amount correctly as pounds" in {
        val testValue: BigDecimal = -1000.44
        val testAmount: Amount    = new Amount(testValue, testCurrency)
        testAmount.renderCurrencyValueAsHtml(poundsOnly = true) mustEqual "&minus;&nbsp;&pound;1000"
      }

    }

    "parsed as JSON" must {
      "carry out JSON transformation" in {
        val amountText     = """{"amount":1.0,"currency":"GBP"}"""
        val jsonFromText   = Json.parse(amountText)
        val amountObject   = Amount(1.0, testCurrency)
        val jsonFromObject = Json.toJson(amountObject)
        assert(jsonFromText equals jsonFromObject)
      }
    }

    "toHalfRoundedUpAmount is called" must {
      "can round up if testValue is Greater than or 1000.5" in {
        val testValue: BigDecimal = 1000.5
        val testAmount: Amount    = new Amount(testValue, testCurrency)
        testAmount.toHalfRoundedUpAmount mustEqual "1,001"
      }

      "can round down if testValue is less than 1000.4" in {
        val testValue: BigDecimal = 1000.4
        val testAmount: Amount    = new Amount(testValue, testCurrency)
        testAmount.toHalfRoundedUpAmount mustEqual "1,000"
      }
    }

    "isZero is called" must {

      "return true" when {
        "the amount is zero" in {
          val amount = Amount(0, testCurrency)
          amount.isZero mustBe true
        }
      }

      "return false" when {
        "the amount is greater than zero" in {
          forAll { (bd: BigDecimal) =>
            whenever(bd > 0) {
              val amount = Amount(bd, testCurrency)
              amount.isZero mustBe false
            }
          }
        }

        "the amount is less than zero" in {
          forAll { (bd: BigDecimal) =>
            whenever(bd < 0) {
              val amount = Amount(bd, testCurrency)
              amount.isZero mustBe false
            }
          }
        }
      }
    }

    "isZeroOrLess is called" must {

      "return true" when {
        "the amount is zero" in {
          val amount = Amount(0, testCurrency)
          amount.isZeroOrLess mustBe true
        }

        "the amount is less than zero" in {
          forAll { (bd: BigDecimal) =>
            whenever(bd < 0) {
              val amount = Amount(bd, testCurrency)
              amount.isZeroOrLess mustBe true
            }
          }
        }
      }

      "return false" when {
        "the amount is greater than zero" in {
          forAll { (bd: BigDecimal) =>
            whenever(bd > 0) {
              val amount = Amount(bd, testCurrency)
              amount.isZeroOrLess mustBe false
            }
          }
        }
      }
    }

    "nonZero is called" must {

      "the amount is not zero" in {
        forAll { (bd: BigDecimal) =>
          whenever(bd.compareTo(0) != 0) {
            val amount = Amount(bd, testCurrency)
            amount.nonZero mustBe true
          }
        }
      }
    }

    "return false" when {
      "the amount is zero" in {
        val amount = Amount(0, testCurrency)
        amount.nonZero mustBe false
      }
    }
  }

  "toCreditString is called" must {
    "can round up if it's a credit" in {
      val testValue: BigDecimal = 1000.01
      val testAmount: Amount    = new Amount(testValue, testCurrency)
      testAmount.toCreditString mustEqual "1,001"
    }
  }

  "toTwoDecimalString is called" must {
    "have a comma and two decimal places for government spend values" in {
      val testValue: BigDecimal = 1000.01
      val testAmount: Amount    = new Amount(testValue, testCurrency)
      testAmount.toTwoDecimalString mustEqual "1,000.01"
    }

    "not round up government spend values" in {
      val testValue: BigDecimal = 1000.99
      val testAmount: Amount    = new Amount(testValue, testCurrency)
      testAmount.toTwoDecimalString mustEqual "1,000.99"
    }

    "round down government spend values after the second decimal place" in {
      val testValue: BigDecimal = 1000.018796799
      val testAmount: Amount    = new Amount(testValue, testCurrency)
      testAmount.toTwoDecimalString mustEqual "1,000.01"
    }
  }

  "toHundredthsString is called" must {
    "give the amount as a string with the amount multiplied by 100" in {
      val amt = Amount(BigDecimal(123.45678), testCurrency)
      amt.toHundredthsString mustBe "12,345.67"
    }
  }

  "unary '-' is called" ignore {
    "turn the amount negative" in {
      forAll { (bd: BigDecimal) =>
        whenever(bd > 1) {
          val result = -Amount(bd, testCurrency)
          result.amount mustBe -bd
        }
      }
    }
  }

  "isValueEqual is called" must {
    "return true" when {
      "when values are equal but calculi have different values" in {
        Amount(BigDecimal(34.43), "GBP", Some("calculus1")).isValueEqual(
          Amount(BigDecimal(34.43), "GBP", Some("calculus2"))
        ) mustBe true
      }
    }
    "return false" when {
      "when values are not equal" in {
        Amount(BigDecimal(34.43), "GBP", Some("calculus1")).isValueEqual(
          Amount(BigDecimal(34.44), "GBP", Some("calculus2"))
        ) mustBe false
      }
    }
  }

  "isValueNotEqual is called" must {
    "return false" when {
      "when values are equal but calculi have different values" in {
        Amount(BigDecimal(34.43), "GBP", Some("calculus1")).isValueNotEqual(
          Amount(BigDecimal(34.43), "GBP", Some("calculus2"))
        ) mustBe false
      }
    }

    "return true" when {
      "when values are not equal" in {
        Amount(BigDecimal(34.43), "GBP", Some("calculus1")).isValueNotEqual(
          Amount(BigDecimal(34.44), "GBP", Some("calculus2"))
        ) mustBe true
      }
    }
  }
}
