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

import com.softwaremill.quicklens._
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

    val rowData = List(
      (
        "starter",
        modifyLens[ScottishTax](_.scottishStarterIncomeTax),
        modifyLens[ScottishTax](_.scottishStarterIncomeTaxAmount),
        modifyLens[ScottishRates](_.scottishStarterRate)
      ),
      (
        "basic",
        modifyLens[ScottishTax](_.scottishBasicIncomeTax),
        modifyLens[ScottishTax](_.scottishBasicIncomeTaxAmount),
        modifyLens[ScottishRates](_.scottishBasicRate)
      ),
      (
        "intermediate",
        modifyLens[ScottishTax](_.scottishIntermediateIncomeTax),
        modifyLens[ScottishTax](_.scottishIntermediateIncomeTaxAmount),
        modifyLens[ScottishRates](_.scottishIntermediateRate)
      ),
      (
        "higher",
        modifyLens[ScottishTax](_.scottishHigherIncomeTax),
        modifyLens[ScottishTax](_.scottishHigherIncomeTaxAmount),
        modifyLens[ScottishRates](_.scottishHigherRate)
      ),
      (
        "additional",
        modifyLens[ScottishTax](_.scottishAdditionalIncomeTax),
        modifyLens[ScottishTax](_.scottishAdditionalIncomeTaxAmount),
        modifyLens[ScottishRates](_.scottishAdditionalRate)
      )
    )

    for ((id, taxLens, totalLens, rateLens) <- rowData)
      s"display $id tax row" in {

        forAll { (tax: Amount, total: Amount, rate: Rate) =>
          val taxData = (taxLens.setTo(tax) andThen totalLens.setTo(total))(scottishTaxData)
          val rates   = rateLens.setTo(rate)(scottishRateData)

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
