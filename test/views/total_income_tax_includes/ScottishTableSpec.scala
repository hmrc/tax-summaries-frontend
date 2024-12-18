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

package views.total_income_tax_includes

import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary._
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import utils.{TestConstants, ViewUtils}
import view_models.{Amount, Rate, ScottishRates, ScottishTax}
import views.ViewSpecBase
import views.html.total_income_tax_includes.ScottishTableView

class ScottishTableSpec extends ViewSpecBase with TestConstants with ScalaCheckDrivenPropertyChecks {

  val scottishTaxData: ScottishTax              = ScottishTax.empty
  val scottishRateData: ScottishRates           = ScottishRates.empty
  lazy val scottishTableView: ScottishTableView = inject[ScottishTableView]
  lazy val viewUtils: ViewUtils                 = inject[ViewUtils]

  def view(tax: ScottishTax, rates: ScottishRates): String =
    scottishTableView(tax, rates).body

  def view(tax: ScottishTax): String = view(tax, scottishRateData)
  def view: String                   = view(scottishTaxData, scottishRateData)

  implicit val arbAmount: Arbitrary[Amount] = Arbitrary(arbitrary[BigDecimal].flatMap(Amount.gbp))
  implicit val arbRate: Arbitrary[Rate]     = Arbitrary(arbitrary[String].flatMap(s => Rate(s)))

  "view" must {

    "display header" in {
      val taxData = scottishTaxData.copy(scottishStarterIncomeTax = Amount.gbp(1))

      view(taxData) must include(messages("ats.total_income_tax.scottish_income_tax"))
      view(taxData) must include(messages("generic.amount_pounds"))
    }

    "hide header" when {

      "there are no scottish taxes" in {

        view must not include messages("ats.total_income_tax.scottish_income_tax")
      }
    }

    val rowData: Seq[Tuple2[String, (Amount, Amount, Rate) => Tuple2[ScottishTax, ScottishRates]]] = Seq(
      Tuple2(
        "starter",
        (tax, total, rate) =>
          Tuple2(
            scottishTaxData.copy(scottishStarterIncomeTax = tax, scottishStarterIncomeTaxAmount = total),
            scottishRateData.copy(scottishStarterRate = rate)
          )
      ),
      Tuple2(
        "basic",
        (tax, total, rate) =>
          Tuple2(
            scottishTaxData.copy(scottishBasicIncomeTax = tax, scottishBasicIncomeTaxAmount = total),
            scottishRateData.copy(scottishBasicRate = rate)
          )
      ),
      Tuple2(
        "intermediate",
        (tax, total, rate) =>
          Tuple2(
            scottishTaxData.copy(scottishIntermediateIncomeTax = tax, scottishIntermediateIncomeTaxAmount = total),
            scottishRateData.copy(scottishIntermediateRate = rate)
          )
      ),
      Tuple2(
        "higher",
        (tax, total, rate) =>
          Tuple2(
            scottishTaxData.copy(scottishHigherIncomeTax = tax, scottishHigherIncomeTaxAmount = total),
            scottishRateData.copy(scottishHigherRate = rate)
          )
      ),
      Tuple2(
        "additional",
        (tax, total, rate) =>
          Tuple2(
            scottishTaxData.copy(scottishAdditionalIncomeTax = tax, scottishAdditionalIncomeTaxAmount = total),
            scottishRateData.copy(scottishAdditionalRate = rate)
          )
      )
    )

    for ((id, func) <- rowData)
      s"display $id tax row" in {

        forAll { (tax: Amount, total: Amount, rate: Rate) =>
          val (taxData, rates) = func(tax, total, rate)

          val result = view(taxData, rates)

          tax match {
            case Amount.empty =>
              result must not include viewUtils.toCurrency(tax)

            case _            =>
              result must include(
                messages(
                  s"ats.total_income_tax.scottish_income_tax.table.$id",
                  viewUtils.toCurrency(total),
                  rate.percent
                )
              )
              result must include(viewUtils.toCurrency(tax))
          }
        }
      }

    "show total row" in {

      forAll { total: Amount =>
        val taxData = scottishTaxData.copy(scottishTotalTax = total)
        val result  = view(taxData)

        total match {
          case Amount.empty =>
            result must not include messages("ats.total_income_tax.scottish_income_tax.table.total")

          case _            =>
            result must include(messages("ats.total_income_tax.scottish_income_tax.table.total"))
            result must include(viewUtils.toCurrency(total))
        }
      }
    }
  }
}
