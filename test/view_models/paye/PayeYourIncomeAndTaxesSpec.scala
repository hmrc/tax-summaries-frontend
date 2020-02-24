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
import view_models.{Amount, Rate}

class PayeYourIncomeAndTaxesSpec extends UnitSpec with MockitoSugar with JsonUtil with GuiceOneAppPerTest with ScalaFutures with IntegrationPatience {

  "PayeYourIncomeAndTaxesData" should {

    "Transform PayeAtsData to view model" in {

      val yourIncomeAndTaxesData = PayeAtsTestData.yourIncomeAndTaxesData

      val expectedViewModel =  PayeYourIncomeAndTaxes(2019, true,
        Amount(500, "GBP"),Some(Amount(400, "GBP")),Amount(600, "GBP"),
        Amount(200, "GBP"),Amount(1100, "GBP"),Rate("20%"),Amount(400, "GBP"))

      val result = PayeYourIncomeAndTaxes.buildViewModel(yourIncomeAndTaxesData)

      result shouldBe expectedViewModel
    }
  }
}
