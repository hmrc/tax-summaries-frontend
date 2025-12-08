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

package common.views.total_income_tax_includes
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import common.utils.{TestConstants, ViewUtils}
import common.view_models.{Amount, Rate, SavingsRates, SavingsTax}
import common.views.ViewSpecBase
import common.views.html.total_income_tax_includes.SavingsTableView

class SavingsTableSpec extends ViewSpecBase with TestConstants with ScalaCheckDrivenPropertyChecks {

  lazy val savingsTableView: SavingsTableView = inject[SavingsTableView]
  lazy val viewUtils: ViewUtils               = inject[ViewUtils]

  val savingsTaxData: SavingsTax    = SavingsTax.empty
  val savingsRateData: SavingsRates = SavingsRates.empty

  def view(tax: SavingsTax, rates: SavingsRates): String =
    savingsTableView(tax, rates).body

  def view(tax: SavingsTax): String = view(tax, savingsRateData)
  def view: String                  = view(savingsTaxData, savingsRateData)

  implicit val arbAmount: Arbitrary[Amount] = Arbitrary(arbitrary[BigDecimal].flatMap(Amount.gbp))
  implicit val arbRate: Arbitrary[Rate]     = Arbitrary(arbitrary[String].flatMap(s => Rate(s)))

  "view" must {

    "display header" in {
      val taxData = savingsTaxData.copy(savingsLowerRateTax = Amount.gbp(1))

      view(taxData) must include(messages("ats.total_income_tax.savings_income_tax"))
      view(taxData) must include(messages("generic.amount_pounds"))
    }

    "hide header" when {

      "there are no savings taxes" in {

        view must not include messages("ats.total_income_tax.savings_income_tax")
      }
    }

    val rowData: Seq[Tuple2[String, (Amount, Amount, Rate) => Tuple2[SavingsTax, SavingsRates]]] = Seq(
      Tuple2(
        "lower",
        (tax, total, rate) =>
          Tuple2(
            savingsTaxData.copy(savingsLowerRateTax = tax, savingsLowerRateTaxAmount = total),
            savingsRateData.copy(savingsLowerRate = rate)
          )
      ),
      Tuple2(
        "higher",
        (tax, total, rate) =>
          Tuple2(
            savingsTaxData.copy(savingsHigherRateTax = tax, savingsHigherRateTaxAmount = total),
            savingsRateData.copy(savingsHigherRate = rate)
          )
      ),
      Tuple2(
        "additional",
        (tax, total, rate) =>
          Tuple2(
            savingsTaxData.copy(savingsAdditionalRateTax = tax, savingsAdditionalRateTaxAmount = total),
            savingsRateData.copy(savingsAdditionalRate = rate)
          )
      )
    )

    for ((id, func) <- rowData)
      s"display $id tax row" in
        forAll { (tax: Amount, total: Amount, rate: Rate) =>
          val (taxData, rates) = func(tax, total, rate)

          val result = view(taxData, rates)

          tax match {
            case Amount.empty =>
              result must not include viewUtils.toCurrency(tax)

            case _ =>
              result must include(
                messages(
                  s"ats.total_income_tax.savings_income_tax.table.$id",
                  viewUtils.toCurrency(total),
                  rate.percent
                )
              )
              result must include(viewUtils.toCurrency(tax))
          }
        }
  }
}
