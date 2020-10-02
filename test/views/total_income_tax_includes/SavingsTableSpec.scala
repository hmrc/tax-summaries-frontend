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

package views.total_income_tax_includes
import com.softwaremill.quicklens.modify
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.prop.PropertyChecks
import org.scalatestplus.play.OneAppPerSuite
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestConstants
import utils.ViewUtils.toCurrency
import view_models.{Amount, Rate, SavingsRates, SavingsTax, ScottishRates, ScottishTax}
import views.html.total_income_tax_includes.SavingsTableView

class SavingsTableSpec extends UnitSpec with GuiceOneAppPerSuite with TestConstants with PropertyChecks {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val messages: Messages = MessagesImpl(Lang("en"), messagesApi)
  lazy val savingsTableView = app.injector.instanceOf[SavingsTableView]

  val savingsTaxData = SavingsTax.empty
  val savingsRateData = SavingsRates.empty

  def view(tax: SavingsTax, rates: SavingsRates): String =
    savingsTableView(tax, rates).body

  def view(tax: SavingsTax): String = view(tax, savingsRateData)
  def view: String = view(savingsTaxData, savingsRateData)

  implicit val arbAmount: Arbitrary[Amount] = Arbitrary(arbitrary[BigDecimal].flatMap(Amount.gbp))
  implicit val arbRate: Arbitrary[Rate]     = Arbitrary(arbitrary[String].flatMap(s => Rate(s)))

  "view" should {

    "display header" in {
      val taxData = savingsTaxData.copy(savingsLowerRateTax = Amount.gbp(1))

      view(taxData) should include(messages("ats.total_income_tax.savings_income_tax"))
      view(taxData) should include(messages("generic.amount_pounds"))
    }

    "hide header" when {

      "there are no savings taxes" in {

        view should not include messages("ats.total_income_tax.savings_income_tax")
      }
    }

    val rowData = List(
      ("lower", modify[SavingsTax](_.savingsLowerRateTax), modify[SavingsTax](_.savingsLowerRateTaxAmount), modify[SavingsRates](_.savingsLowerRate)),
      ("higher", modify[SavingsTax](_.savingsHigherRateTax), modify[SavingsTax](_.savingsHigherRateTaxAmount), modify[SavingsRates](_.savingsHigherRate)),
      ("additional", modify[SavingsTax](_.savingsAdditionalRateTax), modify[SavingsTax](_.savingsAdditionalRateTaxAmount), modify[SavingsRates](_.savingsAdditionalRate))
    )

    for ((id, taxLens, totalLens, rateLens) <- rowData) {
      s"display $id tax row" in {

        forAll { (tax: Amount, total: Amount, rate: Rate) =>
          val taxData = (taxLens.setTo(tax) andThen totalLens.setTo(total))(savingsTaxData)
          val rates   = rateLens.setTo(rate)(savingsRateData)

          val result = view(taxData, rates)

          tax match {
            case Amount.empty =>
              result should not include toCurrency(tax)

            case _ =>
              result should include(
                messages(s"ats.total_income_tax.savings_income_tax.table.$id", toCurrency(total), rate.percent))
              result should include(toCurrency(tax))
          }
        }
      }
    }
  }
}