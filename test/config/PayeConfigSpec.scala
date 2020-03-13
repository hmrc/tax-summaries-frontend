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

package config

import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec

class PayeConfigSpec extends UnitSpec with MockitoSugar {

  "PayeConfig" should {
    "retrieve spend categories in order for a valid year" in {
      val config = new PayeConfig {
        override protected val configPath: String = "paye.conf"
        override val payeYear: Int = 2018
      }

      val expected = List(
        "Welfare",
        "Health",
        "StatePensions",
        "Education",
        "NationalDebtInterest",
        "Defence",
        "Transport",
        "PublicOrderAndSafety",
        "BusinessAndIndustry",
        "GovernmentAdministration",
        "HousingAndUtilities",
        "Environment",
        "Culture",
        "OverseasAid",
        "UkContributionToEuBudget"
      )

      config.spendCategories shouldBe expected
    }

    "retrieve scottish tax band keys in order for a valid year" in {
      val config = new PayeConfig {
        override protected val configPath: String = "paye.conf"
        override val payeYear: Int = 2018
      }

      val expected = List(
        "scottish_starter_rate",
        "scottish_basic_rate",
        "scottish_intermediate_rate",
        "scottish_higher_rate"
      )

      config.scottishTaxBandKeys shouldBe expected
    }

    "retrieve UK tax band keys in order for a valid year" in {
      val config = new PayeConfig {
        override protected val configPath: String = "paye.conf"
        override val payeYear: Int = 2018
      }

      val expected = List(
        "ordinary_rate",
        "higher_rate_income_tax",
        "basic_rate_income_tax",
        "upper_rate"
      )

      config.ukTaxBandKeys shouldBe expected
    }

    "throw an exception for an invalid year" in {
      val config = new PayeConfig {
        override protected val configPath: String = "paye.conf"
        override val payeYear: Int = 2019
      }

      assertThrows[RuntimeException]{config.spendCategories}
    }
  }

}
