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

package view_models

import models.{AtsYearChoice, SA}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.data.FormError
import utils.{BaseSpec, TaxYearUtil}

class AtsFormsSpec extends BaseSpec {

  private val mockTaxYearUtil = mock[TaxYearUtil]
  private val atsForms        = new AtsForms(mockTaxYearUtil)

  "atsYearFormMapping" must {
    "map correctly for a valid year option" in {
      when(mockTaxYearUtil.isValidTaxYear(any())).thenReturn(true)
      val result = atsForms.atsYearFormMapping.bind(Map("year" -> s"SA-$currentTaxYearSA"))
      result.value mustBe Some(AtsYearChoice(SA, currentTaxYearSA))
    }
    "return invalid response for an invalid year option (invalid int)" in {
      when(mockTaxYearUtil.isValidTaxYear(any())).thenReturn(true)
      val result = atsForms.atsYearFormMapping.bind(Map("year" -> s"SA-$currentTaxYearSA)"))
      result.errors mustBe Seq(FormError("year", List("ats.select_tax_year.required"), List()))
    }
  }

}
