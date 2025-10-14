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

package views

import controllers.auth.requests
import controllers.auth.requests.AuthenticatedRequest
import models.ActingAsAttorneyFor
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.SaUtr
import utils.TestConstants
import view_models._
import views.html.NicsView
import views.html.total_income_tax_includes._

class NicsViewSpec extends ViewSpecBase with TestConstants with ScalaCheckDrivenPropertyChecks {

  implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] =
    requests.AuthenticatedRequest(
      "userId",
      None,
      Some(SaUtr(testUtr)),
      None,
      isAgentActive = false,
      ConfidenceLevel.L200,
      fakeCredentials,
      FakeRequest()
    )
  lazy val scottishTableView: ScottishTableView                      = inject[ScottishTableView]
  lazy val savingsTableView: SavingsTableView                        = inject[SavingsTableView]
  lazy val nicsView: NicsView                                        = inject[NicsView]

  def view(tax: IncomeTaxAndNI): String =
    nicsView(viewModel = tax, actingAsAttorney = None, includeBRDMessage = false).body

  def view: String = view(testIncomeTaxAndNI)

  def agentView: String =
    nicsView(
      viewModel = testIncomeTaxAndNI,
      actingAsAttorney = Some(ActingAsAttorneyFor(Some("Agent"), Map())),
      includeBRDMessage = false
    ).body

  implicit val arbAmount: Arbitrary[Amount]           = Arbitrary(arbitrary[BigDecimal].flatMap(Amount.gbp))
  implicit val arbRate: Arbitrary[Rate]               = Arbitrary(arbitrary[String].flatMap(s => Rate(s)))
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
      l <- arbitrary[Amount]
      m <- arbitrary[Amount]
      n <- arbitrary[Amount]
      o <- arbitrary[Amount]
    } yield ScottishTax(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o)

    Gen.frequency((1, Gen.const(ScottishTax.empty)), (19, st))
  }

  implicit val arbScottishRates: Arbitrary[ScottishRates] = Arbitrary {
    val sr = for {
      a <- arbitrary[Rate]
      b <- arbitrary[Rate]
      c <- arbitrary[Rate]
      d <- arbitrary[Rate]
      e <- arbitrary[Rate]
      f <- arbitrary[Rate]
      g <- arbitrary[Rate]
    } yield ScottishRates(a, b, c, d, e, f, g)

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
    } yield SavingsTax(a, b, c, d, e, f)

    Gen.frequency((1, Gen.const(SavingsTax.empty)), (19, st))
  }

  implicit val arbSavingsRates: Arbitrary[SavingsRates] = Arbitrary {
    val sr = for {
      a <- arbitrary[Rate]
      b <- arbitrary[Rate]
      c <- arbitrary[Rate]
    } yield SavingsRates(a, b, c)

    Gen.frequency((1, Gen.const(SavingsRates.empty)), (19, sr))
  }

  "view" must {

    "include scottish table" in
      forAll { (tax: ScottishTax, rates: ScottishRates) =>
        val data = testIncomeTaxAndNI.copy(scottishTax = tax, scottishRates = rates)
        view(data) must include(scottishTableView(tax, rates).body)
      }

    "include savings table" in
      forAll { (tax: SavingsTax, rates: SavingsRates) =>
        val data = testIncomeTaxAndNI.copy(savingsTax = tax, savingsRates = rates)
        view(data) must include(savingsTableView(tax, rates).body)
      }

    "include total uk income tax if there are any values (and scottish)" in {
      val data = testIncomeTaxAndNI.copy(
        savingsTax = SavingsTax(
          Amount(BigDecimal(100.11), "GBP"),
          Amount.empty,
          Amount.empty,
          Amount.empty,
          Amount.empty,
          Amount.empty
        ),
        incomeTaxStatus = "0002"
      )
      view(data) must include("total-uk-income-tax-amount")
    }

    "not show total uk income tax if there no values (and scottish)" in {
      val data = testIncomeTaxAndNI
      view(data) mustNot include("total-uk-income-tax-amount")
    }

    "not show total uk income tax if there any values (and not scottish)" in {
      val data = testIncomeTaxAndNI.copy(
        savingsTax = SavingsTax(
          Amount(BigDecimal(100.11), "GBP"),
          Amount.empty,
          Amount.empty,
          Amount.empty,
          Amount.empty,
          Amount.empty
        )
      )
      view(data) mustNot include("total-uk-income-tax-amount")
    }

    "not show account menu for agent" in {

      val result = agentView
      result must not include "hmrc-account-menu"
    }

    "show account menu for non agent users" in {

      val result = view
      result must include("hmrc-account-menu")
    }
  }
}
