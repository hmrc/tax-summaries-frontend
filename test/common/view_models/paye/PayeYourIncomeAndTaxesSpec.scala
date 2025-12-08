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

package common.view_models.paye

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.test.Injecting
import common.services.atsData.PayeAtsTestData
import common.utils.{JsonUtil, TaxYearForTesting}
import common.view_models.Amount
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
class PayeYourIncomeAndTaxesSpec
    extends AnyWordSpec
    with Matchers
    with JsonUtil
    with GuiceOneAppPerSuite
    with ScalaFutures
    with IntegrationPatience
    with Injecting
    with TaxYearForTesting {

  lazy val payeAtsTestData = inject[PayeAtsTestData]

  "PayeYourIncomeAndTaxesData" must {

    "successfully Transform PayeAtsData to view model" in {

      val yourIncomeAndTaxesData = payeAtsTestData.yourIncomeAndTaxesData

      val expectedViewModel = Some(
        PayeYourIncomeAndTaxes(
          currentTaxYearSA,
          Amount(500, "GBP"),
          Amount(9740, "GBP"),
          Amount(200, "GBP"),
          Amount(1100, "GBP"),
          "20"
        )
      )

      val result = PayeYourIncomeAndTaxes.buildViewModel(yourIncomeAndTaxesData, currentTaxYearSA)

      result mustBe expectedViewModel
    }

    "return incomeBeforeTaxAmount as zero if total_income_before_tax key is not present in PayeAtsData" in {

      val yourIncomeAndTaxesData = payeAtsTestData.malformedYourIncomeAndTaxesData

      val expectedViewModel = Some(
        PayeYourIncomeAndTaxes(
          currentTaxYearSA,
          Amount(0, "GBP"),
          Amount(9740, "GBP"),
          Amount(200, "GBP"),
          Amount(1100, "GBP"),
          "20"
        )
      )

      val result = PayeYourIncomeAndTaxes.buildViewModel(yourIncomeAndTaxesData, currentTaxYearSA)

      result mustBe expectedViewModel
    }

    "return correct data for total_tax_free_amount if total_tax_free_amount is not present in PayeAtsData" in {

      val yourIncomeAndTaxesData = payeAtsTestData.YourIncomeAndTaxesDataWithMissingTotalTaxFreeAmount

      val expectedViewModel = Some(
        PayeYourIncomeAndTaxes(
          currentTaxYearSA,
          Amount(500, "GBP"),
          Amount(9740, "GBP"),
          Amount(200, "GBP"),
          Amount(1100, "GBP"),
          "20"
        )
      )

      val result = PayeYourIncomeAndTaxes.buildViewModel(yourIncomeAndTaxesData, currentTaxYearSA)

      result mustBe expectedViewModel
    }

    "return correct data for totalIncomeTax if employee_nic_amount is not present in PayeAtsData" in {

      val yourIncomeAndTaxesData = payeAtsTestData.YourIncomeAndTaxesDataWithMissingEmployeeNicAmount

      val expectedViewModel = Some(
        PayeYourIncomeAndTaxes(
          currentTaxYearSA,
          Amount(500, "GBP"),
          Amount(9740, "GBP"),
          Amount(200, "GBP"),
          Amount(1100, "GBP"),
          "20"
        )
      )

      val result = PayeYourIncomeAndTaxes.buildViewModel(yourIncomeAndTaxesData, currentTaxYearSA)

      result mustBe expectedViewModel
    }
  }
}
