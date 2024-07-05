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

import java.text.NumberFormat
import java.util.Locale
import play.api.libs.json.{Json, OFormat}
import utils.BigDecimalUtils

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

  def unary_- : Amount                    = copy(amount = -this.amount)
  // TODO: Need to define == as calculus is interfering with comparisons wherever == Amount.empty is used
  def isValueEqual(that: Amount): Boolean = this.amount == that.amount && this.currency == that.currency
}

object Amount {
  implicit val formats: OFormat[Amount] = Json.format[Amount]

  val empty: Amount                  = gbp(0)
  def gbp(value: BigDecimal): Amount = Amount(value, "GBP")

  def apply(amount: Option[Amount]): Amount = amount.getOrElse(empty)
}
