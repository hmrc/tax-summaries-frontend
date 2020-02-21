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

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import services.atsData.PayeAtsTestData
import uk.gov.hmrc.play.test.UnitSpec
import utils.JsonUtil
import view_models.Amount

class PayeGovernmentSpendSpec extends UnitSpec with MockitoSugar with JsonUtil with GuiceOneAppPerTest with ScalaFutures with IntegrationPatience {

  "PayeGovernmentSpend" should {

    "Transform PayeAtsData to view model" in {

      val payeGovSpendingData = PayeAtsTestData.govSpendingData

      val expectedViewModel =  PayeGovernmentSpend(2019, List(
        SpendRow("Welfare", 23.5, Amount(451, "GBP")),
        SpendRow("Health", 20.2, Amount(388, "GBP")),
        SpendRow("StatePensions", 12.8, Amount(246, "GBP")),
        SpendRow("Education", 11.8, Amount(226, "GBP")),
        SpendRow("Defence", 5.3, Amount(102, "GBP")),
        SpendRow("NationalDebtInterest", 5.1, Amount(98, "GBP")),
        SpendRow("Transport", 4.3, Amount(83, "GBP")),
        SpendRow("PublicOrderAndSafety", 4.3, Amount(83, "GBP")),
        SpendRow("BusinessAndIndustry", 3.6, Amount(69, "GBP")),
        SpendRow("GovernmentAdministration", 2.1, Amount(40, "GBP")),
        SpendRow("HousingAndUtilities", 1.6, Amount(31, "GBP")),
        SpendRow("Environment", 1.5, Amount(29, "GBP")),
        SpendRow("Culture", 1.5, Amount(29, "GBP")),
        SpendRow("OverseasAid", 1.2, Amount(23, "GBP")),
        SpendRow("UkContributionToEuBudget", 1, Amount(19, "GBP")))
        , totalAmount = Amount(200,"GBP"))

      val result = PayeGovernmentSpend.buildViewModel(payeGovSpendingData)

      result.orderedSpendRows.size shouldBe PayeGovernmentSpend.orderedSpendCategories.size

      result shouldBe expectedViewModel
    }
  }
}
