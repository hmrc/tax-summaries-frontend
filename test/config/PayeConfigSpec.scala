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

package config

import controllers.paye.AppConfigBaseSpec
import org.scalatestplus.mockito.MockitoSugar

class PayeConfigSpec extends AppConfigBaseSpec with MockitoSugar {

  implicit val config: PayeConfig = inject[PayeConfig]

  "PayeConfig" should {

    "retrieve scottish tax band keys in order for a valid year" in {

      val expected = List(
        "scottish_starter_rate",
        "scottish_basic_rate",
        "scottish_intermediate_rate",
        "scottish_higher_rate"
      )

      config.scottishTaxBandKeys shouldBe expected
    }

    "retrieve UK tax band keys in order for a valid year" in {

      val expected = List(
        "basic_rate_income_tax",
        "higher_rate_income_tax",
        "ordinary_rate",
        "upper_rate"
      )

      config.ukTaxBandKeys shouldBe expected
    }

    "retrieve adjustment keys in order for a valid year" in {

      val expected = List(
        "less_tax_adjustment_previous_year",
        "marriage_allowance_received_amount",
        "married_couples_allowance_adjustment",
        "tax_underpaid_previous_year"
      )

      config.adjustmentsKeys shouldBe expected
    }
  }

}
