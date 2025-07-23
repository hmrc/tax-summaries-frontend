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

package view_models

import play.api.i18n.Messages
import play.api.libs.json.{Json, OFormat}
import utils.BigDecimalUtils

import java.text.NumberFormat
import java.util.Locale

case class Amount(amount: BigDecimal, currency: String, calculus: Option[String] = None) extends BigDecimalUtils {

  private def format(
    decimalNumber: Int,
    roundingMode: BigDecimal.RoundingMode.Value,
    amount: BigDecimal = this.amount
  ) = {
    val formatter = NumberFormat.getNumberInstance(Locale.UK)
    formatter.setMinimumFractionDigits(decimalNumber)
    formatter.format(amount.setScale(decimalNumber, roundingMode))
  }

  override def toString: String = format(0, BigDecimal.RoundingMode.DOWN)

  def toHalfRoundedUpAmount: String = format(0, BigDecimal.RoundingMode.HALF_UP)

  def isZero: Boolean = amount.compareTo(BigDecimal(0)) == 0

  def isZeroOrLess: Boolean = isZero || amount < BigDecimal(0)

  val nonZero: Boolean = !isZero

  def toCreditString: String = format(0, BigDecimal.RoundingMode.UP)

  def toTwoDecimalString: String = format(2, BigDecimal.RoundingMode.DOWN)

  def toHundredthsString: String = format(2, BigDecimal.RoundingMode.DOWN, amount = amount * 100)

  def unary_- : Amount = copy(amount = -this.amount)

  def isValueEqual(that: Amount): Boolean = this.amount == that.amount && this.currency == that.currency

  def isValueNotEqual(that: Amount): Boolean = !isValueEqual(that)

  def renderCurrencyValueAsHtml(poundsOnly: Boolean = false, spoken: Boolean = false)(implicit
    messages: Messages
  ): String = {
    val absAmountAsBigDecimal = amount.abs
    val isNegative            = amount < 0
    lazy val poundsPart       = absAmountAsBigDecimal.setScale(0, BigDecimal.RoundingMode.DOWN)
    if (spoken) {
      val prefix = if (isNegative) s"""${messages("generic.minus")} """ else ""
      if (poundsOnly) {
        s"$prefix$poundsPart ${messages("generic.pounds")}"
      } else {
        val pencePart = ((absAmountAsBigDecimal - poundsPart) * 100).toInt
        s"$prefix$poundsPart ${messages("generic.pounds")} $pencePart ${messages("generic.pence")}"
      }
    } else {
      def positiveAmount = Amount(absAmountAsBigDecimal, currency, calculus)
      (isNegative, poundsOnly) match {
        case (false, false) => s"&pound;${positiveAmount.toTwoDecimalString}"
        case (false, true)  => s"&pound;$poundsPart"
        case (true, false)  => s"&minus;&nbsp;&pound;${positiveAmount.toTwoDecimalString}"
        case (true, true)   => s"&minus;&nbsp;&pound;$poundsPart"
      }
    }
  }
}

object Amount {
  implicit val formats: OFormat[Amount] = Json.format[Amount]

  val empty: Amount                  = gbp(0)
  def gbp(value: BigDecimal): Amount = Amount(value, "GBP")

  def apply(amount: Option[Amount]): Amount = amount.getOrElse(empty)
}
