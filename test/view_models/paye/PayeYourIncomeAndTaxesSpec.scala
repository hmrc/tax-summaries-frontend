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

package view_models.paye

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import services.atsData.PayeAtsTestData
import uk.gov.hmrc.play.test.UnitSpec
import utils.JsonUtil
import view_models.Amount

class PayeYourIncomeAndTaxesSpec
    extends UnitSpec with MockitoSugar with JsonUtil with GuiceOneAppPerTest with ScalaFutures
    with IntegrationPatience {

  val taxYear: Int = 2019

  "PayeYourIncomeAndTaxesData" should {

    "successfully Transform PayeAtsData to view model" in {

      val yourIncomeAndTaxesData = PayeAtsTestData.yourIncomeAndTaxesData

      val expectedViewModel = Some(
        PayeYourIncomeAndTaxes(
          taxYear,
          Amount(500, "GBP"),
          Amount(9740, "GBP"),
          Amount(200, "GBP"),
          Amount(1100, "GBP"),
          "20"))

      val result = PayeYourIncomeAndTaxes.buildViewModel(yourIncomeAndTaxesData, taxYear)

      result shouldBe expectedViewModel
    }

    "return incomeBeforeTaxAmount as zero if total_income_before_tax key is not present in PayeAtsData" in {

      val yourIncomeAndTaxesData = PayeAtsTestData.malformedYourIncomeAndTaxesData

      val expectedViewModel = Some(
        PayeYourIncomeAndTaxes(
          taxYear,
          Amount(0, "GBP"),
          Amount(9740, "GBP"),
          Amount(200, "GBP"),
          Amount(1100, "GBP"),
          "20"))

      val result = PayeYourIncomeAndTaxes.buildViewModel(yourIncomeAndTaxesData, taxYear)

      result shouldBe expectedViewModel
    }

    "return correct data for total_tax_free_amount if total_tax_free_amount is not present in PayeAtsData" in {

      val yourIncomeAndTaxesData = PayeAtsTestData.YourIncomeAndTaxesDataWithMissingTotalTaxFreeAmount

      val expectedViewModel = Some(
        PayeYourIncomeAndTaxes(
          taxYear,
          Amount(500, "GBP"),
          Amount(9740, "GBP"),
          Amount(200, "GBP"),
          Amount(1100, "GBP"),
          "20"))

      val result = PayeYourIncomeAndTaxes.buildViewModel(yourIncomeAndTaxesData, taxYear)

      result shouldBe expectedViewModel
    }

    "return correct data for totalIncomeTax if employee_nic_amount is not present in PayeAtsData" in {

      val yourIncomeAndTaxesData = PayeAtsTestData.YourIncomeAndTaxesDataWithMissingEmployeeNicAmount

      val expectedViewModel = Some(
        PayeYourIncomeAndTaxes(
          taxYear,
          Amount(500, "GBP"),
          Amount(9740, "GBP"),
          Amount(200, "GBP"),
          Amount(1100, "GBP"),
          "20"))

      val result = PayeYourIncomeAndTaxes.buildViewModel(yourIncomeAndTaxesData, taxYear)

      result shouldBe expectedViewModel
    }
  }
}
