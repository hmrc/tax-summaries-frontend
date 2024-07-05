/*
 * Copyright 2024 HM Revenue & Customs
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

import utils.BaseSpec

class FieldInfoSpec extends BaseSpec {
  "fieldNameCamelCase" must {
    "convert correctly for string with underscores" in {
      FieldInfo("abc_def_ehi", BigDecimal(0), "").fieldNameCamelCase mustBe "AbcDefEhi"
    }
    "convert correctly for string without underscores" in {
      FieldInfo("abcdefehi", BigDecimal(0), "").fieldNameCamelCase mustBe "Abcdefehi"
    }
    "handle empty string" in {
      FieldInfo("", BigDecimal(0), "").fieldNameCamelCase mustBe ""
    }
  }
}
