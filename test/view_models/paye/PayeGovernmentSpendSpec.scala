/*
 * Copyright 2022 HM Revenue & Customs
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

import config.ApplicationConfig
import models.DataHolder
import org.mockito.Mockito.when
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Configuration
import play.api.test.Injecting
import services.atsData.PayeAtsTestData
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.{BaseSpec, JsonUtil, TestConstants}
import view_models.Amount

class PayeGovernmentSpendSpec
    extends AnyWordSpec with Matchers with MockitoSugar with JsonUtil with GuiceOneAppPerTest with ScalaFutures
    with IntegrationPatience with Injecting {

  lazy val payeAtsTestData = inject[PayeAtsTestData]

  val taxYear: Int = 2021

  class FakeAppConfig extends ApplicationConfig(inject[ServicesConfig], inject[Configuration]) {
    override lazy val taxYear: Int = taxYear
  }

  implicit lazy val appConfig: FakeAppConfig = new FakeAppConfig

  "PayeGovernmentSpend" must {

    "Transform PayeAtsData to view model" when {

      "Scottish income is not present" in {

        val payeGovSpendingData = payeAtsTestData.govSpendingData
        val result = PayeGovernmentSpend(payeGovSpendingData, appConfig)

        result.orderedSpendRows.map(_.spendData.percentage) mustBe TestConstants.expectedPercentageOrder2021
        result mustBe payeAtsTestData.payeGovernmentSpendViewModel2021
      }

      "Scottish income is present and greater than 0" in {
        val payeGovSpendingData = payeAtsTestData.govSpendingData.copy(
          income_tax = Some(DataHolder(Some(Map("scottish_total_tax" -> Amount.gbp(500.00))), None, None))
        )
        val result = PayeGovernmentSpend(payeGovSpendingData, appConfig)

        result.orderedSpendRows.map(_.spendData.percentage) mustBe TestConstants.expectedPercentageOrder2021
        result mustBe payeAtsTestData.payeGovernmentSpendViewModel2021.copy(
          isScottish = true
        )
      }

      "Scottish income is present and equal to 0" in {
        val payeGovSpendingData = payeAtsTestData.govSpendingData.copy(
          income_tax = Some(DataHolder(Some(Map("scottish_total_tax" -> Amount.gbp(0.00))), None, None))
        )
        val result = PayeGovernmentSpend(payeGovSpendingData, appConfig)

        result.orderedSpendRows.map(_.spendData.percentage) mustBe TestConstants.expectedPercentageOrder2021
        result mustBe payeAtsTestData.payeGovernmentSpendViewModel2021.copy(
          isScottish = false
        )
      }

    }

    "reorder categories for tax year 2020" in {

      class FakeAppConfig extends ApplicationConfig(inject[ServicesConfig], inject[Configuration]) {
        override lazy val taxYear: Int = 2020
      }

      implicit lazy val appConfig: FakeAppConfig = new FakeAppConfig

      val payeGovSpendingData = payeAtsTestData.govSpendingDataFor2020
      val result = PayeGovernmentSpend(payeGovSpendingData, appConfig)

      result.orderedSpendRows.map(_.spendData.percentage) mustBe TestConstants.expectedPercentageOrder2020
      result.orderedSpendRows.map(_.category) mustBe TestConstants.expectedCategoryOrderfor2020
    }

    "reorder categories for tax year 2019" in {

      val payeGovSpendingData = payeAtsTestData.govSpendingDataFor2019
      val result = PayeGovernmentSpend(payeGovSpendingData, appConfig)

      result.orderedSpendRows.map(_.spendData.percentage) mustBe TestConstants.expectedPercentageOrder2019
      result.orderedSpendRows.map(_.category) mustBe TestConstants.expectedCategoryOrderFor2019
    }
  }
}
