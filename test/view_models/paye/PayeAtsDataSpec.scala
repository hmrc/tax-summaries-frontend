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

import services.atsData.PayeAtsTestData
import uk.gov.hmrc.play.test.UnitSpec

class PayeAtsDataSpec extends UnitSpec {

  "PayeAtsData" should {

    "Transform PayeAtsData to view model" when {

      "the user is welsh tax payer since scottish_income_tax exists in the payload" in {
        val payeAtsData = PayeAtsTestData.incomeData
        payeAtsData.isWelshTaxPayer shouldBe true
      }

      "the user is not welsh tax payer since scottish_income_tax does not exist in the payload" in {
        val payeAtsData = PayeAtsTestData.incomeDataWithoutScottishIncomeTax
        payeAtsData.isWelshTaxPayer shouldBe false
      }

    }
  }
}
