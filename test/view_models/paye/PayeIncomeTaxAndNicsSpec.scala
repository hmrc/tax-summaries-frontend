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

import models.{DataHolder, PayeAtsData, TaxBand}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import services.atsData.PayeAtsTestData
import uk.gov.hmrc.play.test.UnitSpec
import utils.JsonUtil
import view_models.{Amount, Rate}

class PayeIncomeTaxAndNicsSpec extends UnitSpec with GuiceOneAppPerTest {

  "PayeYourIncomeAndTaxesData" should {

    "transform to view model with all Scottish rates" in {
      val incomeTaxData = PayeAtsTestData.totalIncomeTaxData

      val expectedViewModel =  PayeIncomeTaxAndNics(2018,
        scottishTaxBands = List(TaxBand("scottish_starter_rate", Amount(2000, "GBP"),
          Amount(380, "GBP"), Rate("19%")),
          TaxBand("scottish_basic_rate", Amount(10150, "GBP"),
            Amount(2030, "GBP"), Rate("20%")),
          TaxBand("scottish_intermediate_rate", Amount(19430, "GBP"),
            Amount(4080, "GBP"), Rate("21%")),
          TaxBand("scottish_higher_rate", Amount(31570, "GBP"),
            Amount(12943, "GBP"), Rate("41%"))),
        totalScottishIncomeTax = Amount(19433, "GBP")
      )

      val result = PayeIncomeTaxAndNics(incomeTaxData)

      result shouldBe expectedViewModel
    }

    "transform to view model with only non-zero Scottish rates" in {
      val incomeTaxData = PayeAtsData(
        2018,
        Some(
          DataHolder(
            Some(
              Map(
                "scottish_starter_rate_income_tax_amount" -> Amount.gbp(380),
                "scottish_starter_rate_income_tax" -> Amount.gbp(2000),
                "scottish_basic_rate_income_tax_amount" -> Amount.gbp(2030),
                "scottish_basic_rate_income_tax" -> Amount.gbp(10150),
                "scottish_intermediate_rate_income_tax_amount" -> Amount.gbp(0),
                "scottish_intermediate_rate_income_tax" -> Amount.gbp(0),
                "scottish_higher_rate_income_tax_amount" -> Amount.gbp(0),
                "scottish_higher_rate_income_tax" -> Amount.gbp(0),
                "scottish_total_tax" -> Amount.gbp(19433)
              )
            ),
            Some(
              Map(
                "paye_scottish_starter_rate" -> Rate("19%"),
                "paye_scottish_basic_rate" -> Rate("20%"),
                "paye_scottish_intermediate_rate" -> Rate("0%"),
                "paye_scottish_higher_rate" -> Rate("0%")
              )
            ), None
          )
        ),
        None, None, None, None
      )

      val expectedViewModel =  PayeIncomeTaxAndNics(2018,
        scottishTaxBands = List(TaxBand("scottish_starter_rate", Amount(2000, "GBP"),
          Amount(380, "GBP"), Rate("19%")),
          TaxBand("scottish_basic_rate", Amount(10150, "GBP"),
            Amount(2030, "GBP"), Rate("20%"))),
        totalScottishIncomeTax = Amount(19433, "GBP")
      )

      val result = PayeIncomeTaxAndNics(incomeTaxData)

      result shouldBe expectedViewModel
    }

    "transform to view model with an empty tax bands list with no payments in any scottish tax bands" in {
      val incomeTaxData = PayeAtsData(
        2018,
        Some(
          DataHolder(
            Some(
              Map(
                "scottish_starter_rate_income_tax_amount" -> Amount.gbp(380),
                "scottish_starter_rate_income_tax" -> Amount.gbp(2000),
                "scottish_basic_rate_income_tax_amount" -> Amount.gbp(2030),
                "scottish_basic_rate_income_tax" -> Amount.gbp(10150),
                "scottish_intermediate_rate_income_tax_amount" -> Amount.gbp(0),
                "scottish_intermediate_rate_income_tax" -> Amount.gbp(0),
                "scottish_higher_rate_income_tax_amount" -> Amount.gbp(0),
                "scottish_higher_rate_income_tax" -> Amount.gbp(0),
                "scottish_total_tax" -> Amount.gbp(19433)
              )
            ),
            Some(
              Map(
                "paye_scottish_starter_rate" -> Rate("0%"),
                "paye_scottish_basic_rate" -> Rate("0%"),
                "paye_scottish_intermediate_rate" -> Rate("0%"),
                "paye_scottish_higher_rate" -> Rate("0%")
              )
            ), None
          )
        ),
        None, None, None, None
      )

      val expectedViewModel =  PayeIncomeTaxAndNics(2018,
        scottishTaxBands = List(),
        totalScottishIncomeTax = Amount(19433, "GBP")
      )

      val result = PayeIncomeTaxAndNics(incomeTaxData)

      result shouldBe expectedViewModel
    }

    "transform to view model with empty tax bands with no income and tax data" in {
      val incomeTaxData = PayeAtsData(
        2018,
        None,
        None, None, None, None
      )

      val expectedViewModel =  PayeIncomeTaxAndNics(2018,
        scottishTaxBands = List(),
        totalScottishIncomeTax = Amount.empty
      )

      val result = PayeIncomeTaxAndNics(incomeTaxData)

      result shouldBe expectedViewModel
    }

    "transform to view model with empty tax bands when no amounts are present" in {
      val incomeTaxData = PayeAtsData(
        2018,
        Some(DataHolder(Some(Map()), Some(Map()), None)),
        None, None, None, None
      )

      val expectedViewModel =  PayeIncomeTaxAndNics(2018,
        scottishTaxBands = List(),
        totalScottishIncomeTax = Amount.empty
      )

      val result = PayeIncomeTaxAndNics(incomeTaxData)

      result shouldBe expectedViewModel
    }
  }
}
