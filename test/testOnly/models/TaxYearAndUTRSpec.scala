/*
 * Copyright 2025 HM Revenue & Customs
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

package testOnly.models

import play.api.libs.json.Json
import utils.BaseSpec

class TaxYearAndUTRSpec extends BaseSpec {

  "TaxYearAndUTR" must {
    "correctly extract values using unapply" in {
      val taxYearAndUTR = TaxYearAndUTR(currentTaxYear, "123456789")
      TaxYearAndUTR.unapply(taxYearAndUTR) mustBe Some((currentTaxYear, "123456789"))
    }

    "correctly serialize to JSON" in {
      val taxYearAndUTR = TaxYearAndUTR(currentTaxYear, "123456789")
      val json          = Json.toJson(taxYearAndUTR)
      json mustBe Json.parse(s"""{"taxYear":$currentTaxYear,"utr":"123456789"}""")
    }

    "correctly deserialize from JSON" in {
      val json = Json.parse(s"""{"taxYear":$currentTaxYear,"utr":"123456789"}""")
      json.as[TaxYearAndUTR] mustBe TaxYearAndUTR(currentTaxYear, "123456789")
    }
  }
}
