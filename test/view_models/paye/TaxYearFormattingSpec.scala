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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TaxYearFormattingSpec extends AnyWordSpec with Matchers {

  val instance = new TaxYearFormatting {
    val taxYear = 2023
  }

  "TaxYearFormatting" must {

    "Calculate valid start year" in {
      instance.taxYearFrom mustBe "2022"
    }

    "Calculate valid end year" in {
      instance.taxYearTo mustBe "2023"
    }

  }

}
