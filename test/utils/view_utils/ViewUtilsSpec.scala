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

package utils.view_utils

import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import utils.ViewUtils
import view_models.{Amount, Rate}

class ViewUtilsSpec extends WordSpec with Matchers with ScalaCheckDrivenPropertyChecks {

  val zeroAmount = createAmount(0)
  val zeroRate = Rate("0%")
  lazy val viewUtils = new ViewUtils

  def createAmount(bd: BigDecimal) = Amount(bd, "gbp")

  "toCurrency" should {

    def result(res: String) = s"&pound;$res"

    "return the amount in Pounds" in {

      forAll { bd: BigDecimal =>
        val testAmount = createAmount(bd)
        viewUtils.toCurrency(testAmount) shouldBe result(testAmount.toString)
      }
    }

    "return the amount in Pounds to two decimal places" in {
      forAll { bd: BigDecimal =>
        val testAmount = createAmount(bd)
        viewUtils.toCurrency(testAmount, twoDecimalPlaces = true) shouldBe result(testAmount.toTwoDecimalString)
      }
    }
  }

  "positiveOrZero" should {
    "return the given amount object if the amount is positive" in {

      forAll { dec: BigDecimal =>
        whenever(dec > 0) {
          val amount = createAmount(dec)
          viewUtils.positiveOrZero(amount) shouldBe amount
        }
      }
    }

    "return the given amount object if the amount is zero" in {
      viewUtils.positiveOrZero(zeroAmount) shouldBe zeroAmount
    }

    "return an amount object with zero as the amount if the given amount is negative" in {
      forAll { dec: BigDecimal =>
        whenever(dec < 0) {
          val amount = createAmount(dec)
          viewUtils.positiveOrZero(amount) shouldBe zeroAmount
        }
      }
    }

    "return the given rate if the percentage is positive" in {
      forAll { dec: Double =>
        whenever(dec > 0) {
          val percentage = dec.toString + '%'
          val positiveRate = Rate(percentage)
          viewUtils.positiveOrZero(positiveRate) shouldBe positiveRate
        }
      }
    }

    "return the zero rate if the percentage is zero" in {
      viewUtils.positiveOrZero(zeroRate) shouldBe Rate.empty
    }

    "return the zero rate if the percentage is negative" in {
      forAll { dec: Double =>
        whenever(dec < 0) {
          val percentage = dec.toString + '%'
          val negativeRate = Rate(percentage)
          viewUtils.positiveOrZero(negativeRate) shouldBe Rate.empty
        }
      }
    }
  }
}
