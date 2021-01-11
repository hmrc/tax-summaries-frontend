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

package views.total_income_tax_includes

import com.softwaremill.quicklens._
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary._
import org.scalatest.prop.PropertyChecks
import utils.TestConstants
import utils.ViewUtils._
import view_models.{Amount, Rate, ScottishRates, ScottishTax}
import views.ViewSpecBase
import views.html.total_income_tax_includes.ScottishTableView

class ScottishTableSpec extends ViewSpecBase with TestConstants with PropertyChecks {

  val scottishTaxData = ScottishTax.empty
  val scottishRateData = ScottishRates.empty
  lazy val scottishTableView = inject[ScottishTableView]

  def view(tax: ScottishTax, rates: ScottishRates): String =
    scottishTableView(tax, rates).body

  def view(tax: ScottishTax): String = view(tax, scottishRateData)
  def view: String = view(scottishTaxData, scottishRateData)

  implicit val arbAmount: Arbitrary[Amount] = Arbitrary(arbitrary[BigDecimal].flatMap(Amount.gbp))
  implicit val arbRate: Arbitrary[Rate] = Arbitrary(arbitrary[String].flatMap(s => Rate(s)))

  "view" should {

    "display header" in {
      val taxData = scottishTaxData.copy(scottishStarterIncomeTax = Amount.gbp(1))

      view(taxData) should include(messages("ats.total_income_tax.scottish_income_tax"))
      view(taxData) should include(messages("generic.amount_pounds"))
    }

    "hide header" when {

      "there are no scottish taxes" in {

        view should not include messages("ats.total_income_tax.scottish_income_tax")
      }
    }

    val rowData = List(
      (
        "starter",
        modify[ScottishTax](_.scottishStarterIncomeTax),
        modify[ScottishTax](_.scottishStarterIncomeTaxAmount),
        modify[ScottishRates](_.scottishStarterRate)),
      (
        "basic",
        modify[ScottishTax](_.scottishBasicIncomeTax),
        modify[ScottishTax](_.scottishBasicIncomeTaxAmount),
        modify[ScottishRates](_.scottishBasicRate)),
      (
        "intermediate",
        modify[ScottishTax](_.scottishIntermediateIncomeTax),
        modify[ScottishTax](_.scottishIntermediateIncomeTaxAmount),
        modify[ScottishRates](_.scottishIntermediateRate)),
      (
        "higher",
        modify[ScottishTax](_.scottishHigherIncomeTax),
        modify[ScottishTax](_.scottishHigherIncomeTaxAmount),
        modify[ScottishRates](_.scottishHigherRate)),
      (
        "additional",
        modify[ScottishTax](_.scottishAdditionalIncomeTax),
        modify[ScottishTax](_.scottishAdditionalIncomeTaxAmount),
        modify[ScottishRates](_.scottishAdditionalRate))
    )

    for ((id, taxLens, totalLens, rateLens) <- rowData) {
      s"display $id tax row" in {

        forAll { (tax: Amount, total: Amount, rate: Rate) =>
          val taxData = (taxLens.setTo(tax) andThen totalLens.setTo(total))(scottishTaxData)
          val rates = rateLens.setTo(rate)(scottishRateData)

          val result = view(taxData, rates)

          tax match {
            case Amount.empty =>
              result should not include toCurrency(tax)

            case _ =>
              result should include(
                messages(s"ats.total_income_tax.scottish_income_tax.table.$id", toCurrency(total), rate.percent))
              result should include(toCurrency(tax))
          }
        }
      }
    }

    "show total row" in {

      forAll { total: Amount =>
        val taxData = scottishTaxData.copy(scottishTotalTax = total)
        val result = view(taxData)

        total match {
          case Amount.empty =>
            result should not include messages("ats.total_income_tax.scottish_income_tax.table.total")

          case _ =>
            result should include(messages("ats.total_income_tax.scottish_income_tax.table.total"))
            result should include(toCurrency(total))
        }
      }
    }
  }
}
