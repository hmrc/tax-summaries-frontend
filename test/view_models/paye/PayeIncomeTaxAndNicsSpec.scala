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

    "transform to view model with all tax band rates and adjustments" in {
      val incomeTaxData = PayeAtsTestData.totalIncomeTaxData

      val expectedViewModel =  PayeIncomeTaxAndNics(2018,
        List(TaxBand("scottish_starter_rate", Amount(2000, "GBP"),
          Amount(380, "GBP"), Rate("19%")),
          TaxBand("scottish_basic_rate", Amount(10150, "GBP"),
            Amount(2030, "GBP"), Rate("20%")),
          TaxBand("scottish_intermediate_rate", Amount(19430, "GBP"),
            Amount(4080, "GBP"), Rate("21%")),
          TaxBand("scottish_higher_rate", Amount(31570, "GBP"),
            Amount(12943, "GBP"), Rate("41%"))),
        List(TaxBand("basic_rate_income_tax", Amount(3000, "GBP"),
          Amount(480, "GBP"), Rate("21%")),
          TaxBand("higher_rate_income_tax", Amount(20150, "GBP"),
            Amount(3030, "GBP"), Rate("20%")),
          TaxBand("ordinary_rate", Amount(29430, "GBP"),
            Amount(5080, "GBP"), Rate("19%")),
          TaxBand("upper_rate", Amount(41570, "GBP"),
            Amount(22943, "GBP"), Rate("41%"))),
        Amount(19433, "GBP"),  Amount(20224, "GBP"),Amount(10477, "GBP"),
        List(AdjustmentRow("less_tax_adjustment_previous_year", Amount.gbp(350)),
            AdjustmentRow("marriage_allowance_received_amount", Amount.gbp(200)),
            AdjustmentRow("married_couples_allowance_adjustment", Amount.gbp(400)),
            AdjustmentRow("tax_underpaid_previous_year", Amount.gbp(450)))
      )

      val result = PayeIncomeTaxAndNics(incomeTaxData)

      result shouldBe expectedViewModel
    }

    "transform to view model with only non-zero tax band rates" in {
      val incomeTaxData = PayeAtsData(
        2018,
        Some(
          DataHolder(
            Some(
              Map(
                "scottish_starter_rate_amount" -> Amount.gbp(380),
                "scottish_starter_rate" -> Amount.gbp(2000),
                "scottish_basic_rate_amount" -> Amount.gbp(2030),
                "scottish_basic_rate" -> Amount.gbp(10150),
                "scottish_intermediate_rate_amount" -> Amount.gbp(0),
                "scottish_intermediate_rate" -> Amount.gbp(0),
                "scottish_higher_rate_amount" -> Amount.gbp(0),
                "scottish_higher_rate" -> Amount.gbp(0),
                "scottish_total_tax" -> Amount.gbp(19433),
                "basic_rate_income_tax_amount" -> Amount.gbp(480),
                "basic_rate_income_tax" -> Amount.gbp(3000),
                "higher_rate_income_tax_amount" -> Amount.gbp(3030),
                "higher_rate_income_tax" -> Amount.gbp(20150),
                "ordinary_rate_amount" -> Amount.gbp(480),
                "ordinary_rate" -> Amount.gbp(3000),
                "upper_rate_amount" -> Amount.gbp(3030),
                "upper_rate" -> Amount.gbp(20150),
                "total_UK_income_tax" -> Amount.gbp(20224),
                "total_income_tax_2" -> Amount.gbp(10477)
              )
            ),
            Some(
              Map(
                "paye_scottish_starter_rate" -> Rate("19%"),
                "paye_scottish_basic_rate" -> Rate("20%"),
                "paye_scottish_intermediate_rate" -> Rate("0%"),
                "paye_scottish_higher_rate" -> Rate("0%"),
                "paye_ordinary_rate" -> Rate("19%"),
                "paye_higher_rate_income_tax" -> Rate("0%"),
                "paye_basic_rate_income_tax" -> Rate("0%"),
                "paye_upper_rate" -> Rate("41%")
              )
            ), None
          )
        ),
        None, None, None, None
      )

      val expectedViewModel =  PayeIncomeTaxAndNics(2018,List(TaxBand("scottish_starter_rate", Amount(2000, "GBP"),
          Amount(380, "GBP"), Rate("19%")),
          TaxBand("scottish_basic_rate", Amount(10150, "GBP"),
            Amount(2030, "GBP"), Rate("20%"))),
        List(TaxBand("ordinary_rate", Amount(3000, "GBP"),
          Amount(480, "GBP"), Rate("19%")),
          TaxBand("upper_rate", Amount(20150, "GBP"),
            Amount(3030, "GBP"), Rate("41%"))),Amount(19433, "GBP"),Amount(20224, "GBP"),Amount(10477, "GBP"),
        List.empty
      )

      val result = PayeIncomeTaxAndNics(incomeTaxData)

      result shouldBe expectedViewModel
    }

    "transform to view model with an empty tax bands list with no payments in any tax band" in {
      val incomeTaxData = PayeAtsData(
        2018,
        Some(
          DataHolder(
            Some(
              Map(
                "scottish_starter_rate_amount" -> Amount.gbp(380),
                "scottish_starter_rate" -> Amount.gbp(2000),
                "scottish_basic_rate_amount" -> Amount.gbp(2030),
                "scottish_basic_rate" -> Amount.gbp(10150),
                "scottish_intermediate_rate_amount" -> Amount.gbp(0),
                "scottish_intermediate_rate" -> Amount.gbp(0),
                "scottish_higher_rate_amount" -> Amount.gbp(0),
                "scottish_higher_rate" -> Amount.gbp(0),
                "scottish_total_tax" -> Amount.gbp(19433),
                "basic_rate_income_tax_amount" -> Amount.gbp(480),
                "basic_rate_income_tax" -> Amount.gbp(3000),
                "higher_rate_income_tax_amount" -> Amount.gbp(3030),
                "higher_rate_income_tax" -> Amount.gbp(20150),
                "ordinary_rate_amount" -> Amount.gbp(0),
                "ordinary_rate" -> Amount.gbp(0),
                "upper_rate_amount" -> Amount.gbp(0),
                "upper_rate" -> Amount.gbp(0),
                "total_UK_income_tax" -> Amount.gbp(20224),
                "total_income_tax_2" -> Amount.gbp(10477)
              )
            ),
            Some(
              Map(
                "paye_scottish_starter_rate" -> Rate("0%"),
                "paye_scottish_basic_rate" -> Rate("0%"),
                "paye_scottish_intermediate_rate" -> Rate("0%"),
                "paye_scottish_higher_rate" -> Rate("0%"),
                "paye_ordinary_rate" -> Rate("0%"),
                "paye_higher_rate_income_tax" -> Rate("0%"),
                "paye_basic_rate_income_tax" -> Rate("0%"),
                "paye_upper_rate" -> Rate("0%")
              )
            ), None
          )
        ),
        None, None, None, None
      )

      val expectedViewModel =  PayeIncomeTaxAndNics(2018, List.empty, List.empty, Amount(19433, "GBP"), Amount(20224, "GBP"),Amount(10477, "GBP"), List.empty)

      val result = PayeIncomeTaxAndNics(incomeTaxData)

      result shouldBe expectedViewModel
    }

    "transform to view model with empty tax bands with no income and tax data" in {
      val incomeTaxData = PayeAtsData(
        2018,
        None,
        None, None, None, None
      )

      val expectedViewModel =  PayeIncomeTaxAndNics(2018,List.empty, List.empty,Amount.empty,Amount.empty,Amount.empty, List.empty)

      val result = PayeIncomeTaxAndNics(incomeTaxData)

      result shouldBe expectedViewModel
    }

    "transform to view model with empty tax bands when no amounts are present" in {
      val incomeTaxData = PayeAtsData(
        2018,
        Some(DataHolder(Some(Map()), Some(Map()), None)),
        None, None, None, None
      )

      val expectedViewModel =  PayeIncomeTaxAndNics(2018, List.empty, List.empty ,Amount.empty,Amount.empty,Amount.empty, List.empty)

      val result = PayeIncomeTaxAndNics(incomeTaxData)

      result shouldBe expectedViewModel
    }
  }
}
