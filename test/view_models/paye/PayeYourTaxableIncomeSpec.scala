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

package view_models.paye

import models.{DataHolder, PayeAtsData}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import uk.gov.hmrc.play.test.UnitSpec
import utils.JsonUtil
import view_models.Amount



class PayeYourTaxableIncomeSpec extends UnitSpec with MockitoSugar with JsonUtil with GuiceOneAppPerTest with ScalaFutures with IntegrationPatience {
  val incomeTaxDataStatePensionAndOther = Some(DataHolder(
    payload = Some(Map(
      "self_employment_income" -> Amount(100, "GBP"),
      "income_from_employment" -> Amount(200, "GBP"),
      "state_pension" -> Amount(300, "GBP"),
      "other_pension_income" -> Amount(400, "GBP"),
      "taxable_state_benefits" -> Amount(500, "GBP"),
      "other_income" -> Amount(600, "GBP"),
      "benefits_from_employment" -> Amount(700, "GBP"),
      "total_income_before_tax" -> Amount(800, "GBP")
    )),
    rates = None,
    incomeTaxStatus = None
  ))

  val incomeTaxDataStatePensionNoIncomeFromEmployment = Some(DataHolder(
    payload = Some(Map(
      "self_employment_income" -> Amount(100, "GBP"),
      "income_from_employment" -> Amount(0, "GBP"),
      "state_pension" -> Amount(300, "GBP"),
      "other_pension_income" -> Amount(400, "GBP"),
      "taxable_state_benefits" -> Amount(500, "GBP"),
      "other_income" -> Amount(600, "GBP"),
      "benefits_from_employment" -> Amount(700, "GBP"),
      "total_income_before_tax" -> Amount(800, "GBP")
      )),
    rates = None,
    incomeTaxStatus = None
    ))

  val IncomeTaxJustOtherPension = Some(DataHolder(
    payload = Some(Map(
      "self_employment_income" -> Amount(100, "GBP"),
      "income_from_employment" -> Amount(200, "GBP"),
      "other_pension_income" -> Amount(400, "GBP"),
      "taxable_state_benefits" -> Amount(500, "GBP"),
      "other_income" -> Amount(600, "GBP"),
      "benefits_from_employment" -> Amount(700, "GBP"),
      "total_income_before_tax" -> Amount(800, "GBP")
      )),
    rates = None,
    incomeTaxStatus = None
    ))
  
  def incomeTaxPayeAtsData(incomeTax: Option[DataHolder]): PayeAtsData = {
      PayeAtsData(2018, None, None, incomeTax, None, None)
    }

    "PayeYourTaxableIncome" should {
      "Transform income tax data just other pension to view model" in {
        val expectedIncomeeTaxRows = List(IncomeTaxRow("ats.income_before_tax.table.line1", Amount(100, "GBP")),
                                          IncomeTaxRow("ats.income_before_tax.table.line2", Amount(200, "GBP")),
                                          IncomeTaxRow("ats.income_before_tax.table.line3", Amount(300, "GBP")),
                                          IncomeTaxRow("ats.income_before_tax.table.line4", Amount(400, "GBP")),
                                          IncomeTaxRow("ats.income_before_tax.table.line5", Amount(500, "GBP")),
                                          IncomeTaxRow("paye.ats.income_before_tax.table.line6", Amount(600, "GBP")),
                                          IncomeTaxRow("ats.income_before_tax.table.line7", Amount(700, "GBP")))
        val expectedIncomeBeforeTaxTotal = Amount(800, "GBP")

        val payeAtsData = incomeTaxPayeAtsData(incomeTaxDataStatePensionAndOther)
        val viewModel = PayeYourTaxableIncome.buildViewModel(payeAtsData)

        viewModel.get.incomeTaxRows shouldBe (expectedIncomeeTaxRows)
        viewModel.get.incomeBeforeTaxTaxTotal shouldBe expectedIncomeBeforeTaxTotal
      }

      "Transform income tax data, No income from employment, to view model" in {
        val expectedIncomeeTaxRows = List(IncomeTaxRow("ats.income_before_tax.table.line1", Amount(100, "GBP")),
                                          IncomeTaxRow("ats.income_before_tax.table.line3", Amount(300, "GBP")),
                                          IncomeTaxRow("ats.income_before_tax.table.line4", Amount(400, "GBP")),
                                          IncomeTaxRow("ats.income_before_tax.table.line5", Amount(500, "GBP")),
                                          IncomeTaxRow("paye.ats.income_before_tax.table.line6", Amount(600, "GBP")),
                                          IncomeTaxRow("ats.income_before_tax.table.line7", Amount(700, "GBP")))

        val payeAtsData = incomeTaxPayeAtsData(incomeTaxDataStatePensionNoIncomeFromEmployment)
        val viewModel = PayeYourTaxableIncome.buildViewModel(payeAtsData)

        viewModel.get.incomeTaxRows shouldBe (expectedIncomeeTaxRows)
      }

      "Transform income tax data, state and other pension, to view model" in {
        val expectedIncomeeTaxRows = List(IncomeTaxRow("ats.income_before_tax.table.line1", Amount(100, "GBP")),
                                          IncomeTaxRow("ats.income_before_tax.table.line2", Amount(200, "GBP")),
                                          IncomeTaxRow("paye.ats.income_before_tax.table.line4", Amount(400, "GBP")),
                                          IncomeTaxRow("ats.income_before_tax.table.line5", Amount(500, "GBP")),
                                          IncomeTaxRow("paye.ats.income_before_tax.table.line6", Amount(600, "GBP")),
                                          IncomeTaxRow("ats.income_before_tax.table.line7", Amount(700, "GBP")))

        val payeAtsData = incomeTaxPayeAtsData(IncomeTaxJustOtherPension)
        val viewModel = PayeYourTaxableIncome.buildViewModel(payeAtsData)

        viewModel.get.incomeTaxRows shouldBe (expectedIncomeeTaxRows)
      }
    }
}
