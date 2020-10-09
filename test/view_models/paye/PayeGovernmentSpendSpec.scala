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

import models.{DataHolder, GovernmentSpendingOutputWrapper, SpendData}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import uk.gov.hmrc.play.test.UnitSpec
import utils.JsonUtil
import view_models.Amount
import services.atsData.PayeAtsTestData

class PayeGovernmentSpendSpec
    extends UnitSpec with MockitoSugar with JsonUtil with GuiceOneAppPerTest with ScalaFutures
    with IntegrationPatience {

  "PayeGovernmentSpend" should {

    "Transform PayeAtsData to view model" when {

      "Scottish income is not present" in {
        val payeGovSpendingData = PayeAtsTestData.govSpendingData
        val result = PayeGovernmentSpend(payeGovSpendingData)

        result.orderedSpendRows.map(_._2.percentage) shouldBe PayeAtsTestData.expectedPercentageOrder
        result shouldBe PayeAtsTestData.payeGovernmentSpendViewModel
      }

      "Scottish income is present and greater than 0" in {
        val payeGovSpendingData = PayeAtsTestData.govSpendingData.copy(
          income_tax = Some(DataHolder(Some(Map("scottish_total_tax" -> Amount.gbp(500.00))), None, None))
        )
        val result = PayeGovernmentSpend(payeGovSpendingData)

        result.orderedSpendRows.map(_._2.percentage) shouldBe PayeAtsTestData.expectedPercentageOrder
        result shouldBe PayeAtsTestData.payeGovernmentSpendViewModel.copy(
          isScottish = true
        )
      }

      "Scottish income is present and equal to 0" in {
        val payeGovSpendingData = PayeAtsTestData.govSpendingData.copy(
          income_tax = Some(DataHolder(Some(Map("scottish_total_tax" -> Amount.gbp(0.00))), None, None))
        )
        val result = PayeGovernmentSpend(payeGovSpendingData)

        result.orderedSpendRows.map(_._2.percentage) shouldBe PayeAtsTestData.expectedPercentageOrder
        result shouldBe PayeAtsTestData.payeGovernmentSpendViewModel.copy(
          isScottish = false
        )
      }

    }

    "sort transport above Public Order" in {

      val payeGovSpendingData = PayeAtsTestData.govSpendingData
      val result = PayeGovernmentSpend(payeGovSpendingData)

      result.orderedSpendRows.map(_._2.percentage) shouldBe PayeAtsTestData.expectedPercentageOrder
      result.orderedSpendRows.map(_._1) shouldBe PayeAtsTestData.expectedCategoryOrder
    }
  }
}
