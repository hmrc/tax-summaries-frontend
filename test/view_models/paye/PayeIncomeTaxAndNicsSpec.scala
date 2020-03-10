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

import models.TaxBand
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import services.atsData.PayeAtsTestData
import uk.gov.hmrc.play.test.UnitSpec
import utils.JsonUtil
import view_models.{Amount, Rate}

class PayeIncomeTaxAndNicsSpec extends UnitSpec with MockitoSugar with JsonUtil with GuiceOneAppPerTest with ScalaFutures with IntegrationPatience {

  "PayeYourIncomeAndTaxesData" should {

    "successfully Transform PayeAtsData to view model" in {

      val incomeTaxData = PayeAtsTestData.totalIncomeTaxData

      val expectedViewModel =  PayeIncomeTaxAndNics(2018,
        List(TaxBand(Amount(2000, "GBP"),
          Amount(380, "GBP"), Rate("19%")),
          TaxBand(Amount(10150, "GBP"),
            Amount(2030, "GBP"), Rate("20%")),
          TaxBand(Amount(19430, "GBP"),
            Amount(4080, "GBP"), Rate("21%")),
          TaxBand(Amount(31570, "GBP"),
            Amount(12943, "GBP"), Rate("41%"))),
        totalScottishIncomeTax = Amount(19433, "GBP")
      )

      val result = PayeIncomeTaxAndNics(incomeTaxData)

      result shouldBe expectedViewModel
    }
  }
}
