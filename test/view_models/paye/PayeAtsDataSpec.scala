/*
 * Copyright 2023 HM Revenue & Customs
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

import models.DataHolder
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import services.atsData.PayeAtsTestData
import view_models.Amount

class PayeAtsDataSpec extends AnyWordSpec with Matchers {

  lazy val payeAtsTestData = new PayeAtsTestData

  "PayeAtsData" must {

    "Transform PayeAtsData to view model" when {

      "the user is welsh tax payer since scottish_income_tax exists with non zero value in the payload" in {
        val data = payeAtsTestData.incomeData
        data.isWelshTaxPayer mustBe true
      }

      "the user is not welsh tax payer since scottish_income_tax exists with zero value in the payload" in {
        val data = payeAtsTestData.incomeData.copy(
          income_data = Some(DataHolder(Some(Map("scottish_income_tax" -> Amount.gbp(0))), None, None))
        )
        data.isWelshTaxPayer mustBe false
      }

      "the user is not welsh tax payer since scottish_income_tax exists with negative value in the payload" in {
        val data = payeAtsTestData.incomeData.copy(
          income_data = Some(DataHolder(Some(Map("scottish_income_tax" -> Amount.gbp(-2550))), None, None))
        )
        data.isWelshTaxPayer mustBe false
      }

      "the user is not welsh tax payer since scottish_income_tax does not exist in the payload" in {
        val data = payeAtsTestData.incomeDataWithoutScottishIncomeTax
        data.isWelshTaxPayer mustBe false
      }

    }
  }
}
