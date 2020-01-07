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

package views
import config.AppFormPartialRetriever
import controllers.auth.AuthenticatedRequest
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.prop.PropertyChecks
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestConstants
import view_models.{Amount, Rate, SavingsRates, SavingsTax, ScottishRates, ScottishTax, TotalIncomeTax}
import views.html.total_income_tax_includes.{savings_table, scottish_table}

class SavingsTableSpec extends UnitSpec with OneAppPerSuite with TestConstants with PropertyChecks {

  implicit val messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]
  implicit val messages: Messages = Messages(Lang("en"), messagesApi)
  implicit val request = AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest())
  implicit lazy val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever

  def view(tax: TotalIncomeTax): String =
    views.html.total_income_tax(tax).body

  def view: String = view(testTotalIncomeTax)

  implicit val arbAmount: Arbitrary[Amount] = Arbitrary(arbitrary[BigDecimal].flatMap(Amount.gbp))
  implicit val arbRate: Arbitrary[Rate]     = Arbitrary(arbitrary[String].flatMap(s => Rate(s)))
  implicit val arbScottishTax: Arbitrary[ScottishTax] = Arbitrary {
    val st = for {
      a <- arbitrary[Amount]
      b <- arbitrary[Amount]
      c <- arbitrary[Amount]
      d <- arbitrary[Amount]
      e <- arbitrary[Amount]
      f <- arbitrary[Amount]
      g <- arbitrary[Amount]
      h <- arbitrary[Amount]
      i <- arbitrary[Amount]
      j <- arbitrary[Amount]
      k <- arbitrary[Amount]
    } yield {
      ScottishTax(a, b, c, d, e, f, g, h, i, j, k)
    }

    Gen.frequency((1, Gen.const(ScottishTax.empty)), (19, st))
  }

  implicit val arbScottishRates: Arbitrary[ScottishRates] = Arbitrary {
    val sr = for {
      a <- arbitrary[Rate]
      b <- arbitrary[Rate]
      c <- arbitrary[Rate]
      d <- arbitrary[Rate]
      e <- arbitrary[Rate]
    } yield {
      ScottishRates(a, b, c, d, e)
    }

    Gen.frequency((1, Gen.const(ScottishRates.empty)), (19, sr))
  }

  implicit val arbSavingsTax: Arbitrary[SavingsTax] = Arbitrary {
    val st = for {
      a <- arbitrary[Amount]
      b <- arbitrary[Amount]
      c <- arbitrary[Amount]
      d <- arbitrary[Amount]
      e <- arbitrary[Amount]
      f <- arbitrary[Amount]
    } yield {
      SavingsTax(a, b, c, d, e, f)
    }

    Gen.frequency((1, Gen.const(SavingsTax.empty)), (19, st))
  }

  implicit val arbSavingsRates: Arbitrary[SavingsRates] = Arbitrary {
    val sr = for {
      a <- arbitrary[Rate]
      b <- arbitrary[Rate]
      c <- arbitrary[Rate]
    } yield {
      SavingsRates(a, b, c)
    }

    Gen.frequency((1, Gen.const(SavingsRates.empty)), (19, sr))
  }

  "view" should {

    "include scottish table" in {

      forAll { (tax: ScottishTax, rates: ScottishRates) =>

        val data = testTotalIncomeTax.copy(scottishTax = tax, scottishRates = rates)
        view(data) should include(scottish_table(tax, rates).body)
      }
    }

    "include savings table" in {

      forAll { (tax: SavingsTax, rates: SavingsRates) =>

        val data = testTotalIncomeTax.copy(savingsTax = tax, savingsRates = rates)
        view(data) should include(savings_table(tax, rates).body)
      }
    }

    "include total uk income tax if there are any values (and scottish)" in {
      val data = testTotalIncomeTax.copy(savingsTax = SavingsTax(Amount(BigDecimal(100.11), "GBP"), Amount.empty,
        Amount.empty, Amount.empty, Amount.empty, Amount.empty), incomeTaxStatus = "0002")
      view(data) should include("total-uk-income-tax-amount")
    }

    "not show total uk income tax if there no values (and scottish)" in {
      val data = testTotalIncomeTax
      view(data) shouldNot include("total-uk-income-tax-amount")
    }

    "not show total uk income tax if there any values (and not scottish)" in {
      val data = testTotalIncomeTax.copy(savingsTax = SavingsTax(Amount(BigDecimal(100.11), "GBP"), Amount.empty,
        Amount.empty, Amount.empty, Amount.empty, Amount.empty))
      view(data) shouldNot include("total-uk-income-tax-amount")
    }
  }
}