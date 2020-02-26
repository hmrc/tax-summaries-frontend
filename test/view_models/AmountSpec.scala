/*
 * Copyright 2020 HM Revenue & Customs
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

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec



class AmountSpec extends UnitSpec with GuiceOneAppPerSuite  {

  val testCurrency: String = "GBP"

  "Amount" should {

    "not change constructor parameter values" in {
      val testValue: BigDecimal = 1.0
      val testAmount: Amount = new Amount(testValue, testCurrency)
      assert(testValue equals testAmount.amount)
      assert(testCurrency equals testAmount.currency)
    }

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

    "can round up if it's a credit" in {
      val testValue: BigDecimal = 1000.01
      val testAmount: Amount = new Amount(testValue, testCurrency)
      testAmount.toCreditString shouldEqual "1,001"
    }

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

    "produce correct inverse amount" in {
      val testValue: BigDecimal = 1000.00
      val testAmount: Amount = -Amount(testValue, testCurrency)
      testAmount.toString() shouldEqual "-1,000"
    }

    "carry out JSON transformation" in {
      val amountText = """{"amount":1.0,"currency":"GBP"}"""
      val jsonFromText = Json.parse(amountText)
      val amountObject = Amount(1.0, testCurrency)
      val jsonFromObject = Json.toJson(amountObject)
      assert(jsonFromText equals jsonFromObject)
    }

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
}
