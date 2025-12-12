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

package common.utils

class HtmlFormatterSpec extends BaseSpec {

  lazy val htmlFormatter: HtmlFormatter = inject[HtmlFormatter]

  "toHtmlNonBroken" must {
    "replace spaces with non breaking spaces" in {
      val stringBeforeReplace = s"6 April $currentTaxYearSA to 5 April $currentTaxYearSA"

      val expectedString =
        s"6&nbsp;April&nbsp;$currentTaxYearSA&nbsp;to&nbsp;5&nbsp;April&nbsp;$currentTaxYearSA"

      htmlFormatter.toHtmlNonBroken(stringBeforeReplace) mustBe expectedString
    }
  }
}
