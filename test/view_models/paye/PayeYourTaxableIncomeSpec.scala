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

package view_models.paye

import models.{DataHolder, PayeAtsData}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import utils.JsonUtil
import view_models.Amount

class PayeYourTaxableIncomeSpec
    extends AnyWordSpec
    with Matchers
    with JsonUtil
    with GuiceOneAppPerTest
    with ScalaFutures
    with IntegrationPatience {
  val incomeTaxDataStatePensionAndOther = Some(
    DataHolder(
      payload = Some(
        Map(
          "self_employment_income"   -> Amount(100, "GBP"),
          "income_from_employment"   -> Amount(200, "GBP"),
          "state_pension"            -> Amount(300, "GBP"),
          "other_pension_income"     -> Amount(400, "GBP"),
          "taxable_state_benefits"   -> Amount(500, "GBP"),
          "other_income"             -> Amount(600, "GBP"),
          "benefits_from_employment" -> Amount(700, "GBP"),
          "total_income_before_tax"  -> Amount(800, "GBP")
        )
      ),
      rates = None,
      incomeTaxStatus = None
    )
  )

  val incomeTaxDataStatePensionNoIncomeFromEmployment = Some(
    DataHolder(
      payload = Some(
        Map(
          "self_employment_income"   -> Amount(100, "GBP"),
          "income_from_employment"   -> Amount(0, "GBP"),
          "state_pension"            -> Amount(300, "GBP"),
          "other_pension_income"     -> Amount(400, "GBP"),
          "taxable_state_benefits"   -> Amount(500, "GBP"),
          "other_income"             -> Amount(600, "GBP"),
          "benefits_from_employment" -> Amount(700, "GBP"),
          "total_income_before_tax"  -> Amount(800, "GBP")
        )
      ),
      rates = None,
      incomeTaxStatus = None
    )
  )

  val IncomeTaxJustOtherPension = Some(
    DataHolder(
      payload = Some(
        Map(
          "self_employment_income"   -> Amount(100, "GBP"),
          "income_from_employment"   -> Amount(200, "GBP"),
          "other_pension_income"     -> Amount(400, "GBP"),
          "taxable_state_benefits"   -> Amount(500, "GBP"),
          "other_income"             -> Amount(600, "GBP"),
          "benefits_from_employment" -> Amount(700, "GBP"),
          "total_income_before_tax"  -> Amount(800, "GBP")
        )
      ),
      rates = None,
      incomeTaxStatus = None
    )
  )

  def incomeTaxPayeAtsData(incomeTax: Option[DataHolder]): PayeAtsData =
    PayeAtsData(2022, None, None, incomeTax, None, None)

  "PayeYourTaxableIncome" must {
    "Transform income tax data just other pension to view model" in {
      val expectedIncomeeTaxRows       = List(
        IncomeTaxRow("self_employment_income", Amount(100, "GBP")),
        IncomeTaxRow("income_from_employment", Amount(200, "GBP")),
        IncomeTaxRow("state_pension", Amount(300, "GBP")),
        IncomeTaxRow("other_pension_income", Amount(400, "GBP")),
        IncomeTaxRow("taxable_state_benefits", Amount(500, "GBP")),
        IncomeTaxRow("other_income", Amount(600, "GBP")),
        IncomeTaxRow("benefits_from_employment", Amount(700, "GBP"))
      )
      val expectedIncomeBeforeTaxTotal = Amount(800, "GBP")

      val payeAtsData = incomeTaxPayeAtsData(incomeTaxDataStatePensionAndOther)
      val viewModel   = PayeYourTaxableIncome.buildViewModel(payeAtsData)

      viewModel.incomeTaxRows mustBe expectedIncomeeTaxRows
      viewModel.totalIncomeBeforeTax mustBe expectedIncomeBeforeTaxTotal
    }

    "Transform income tax data, No income from employment, to view model" in {
      val expectedIncomeeTaxRows = List(
        IncomeTaxRow("self_employment_income", Amount(100, "GBP")),
        IncomeTaxRow("state_pension", Amount(300, "GBP")),
        IncomeTaxRow("other_pension_income", Amount(400, "GBP")),
        IncomeTaxRow("taxable_state_benefits", Amount(500, "GBP")),
        IncomeTaxRow("other_income", Amount(600, "GBP")),
        IncomeTaxRow("benefits_from_employment", Amount(700, "GBP"))
      )

      val payeAtsData = incomeTaxPayeAtsData(incomeTaxDataStatePensionNoIncomeFromEmployment)
      val viewModel   = PayeYourTaxableIncome.buildViewModel(payeAtsData)

      viewModel.incomeTaxRows mustBe expectedIncomeeTaxRows
    }

    "Transform income tax data, state and other pension, to view model" in {
      val expectedIncomeeTaxRows = List(
        IncomeTaxRow("self_employment_income", Amount(100, "GBP")),
        IncomeTaxRow("income_from_employment", Amount(200, "GBP")),
        IncomeTaxRow("personal_pension_income", Amount(400, "GBP")),
        IncomeTaxRow("taxable_state_benefits", Amount(500, "GBP")),
        IncomeTaxRow("other_income", Amount(600, "GBP")),
        IncomeTaxRow("benefits_from_employment", Amount(700, "GBP"))
      )

      val payeAtsData = incomeTaxPayeAtsData(IncomeTaxJustOtherPension)
      val viewModel   = PayeYourTaxableIncome.buildViewModel(payeAtsData)

      viewModel.incomeTaxRows mustBe expectedIncomeeTaxRows
    }
  }
}
