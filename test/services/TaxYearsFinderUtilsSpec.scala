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

package services

import org.mockito.Mockito._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.{AtsTaxYearsUtils, BaseSpec}

class TaxYearsFinderUtilsSpec extends BaseSpec {

  val mockServicesConfig: ServicesConfig = mock[ServicesConfig]

  val taxYearsFindersUtils = new AtsTaxYearsUtils(mockServicesConfig)
  "checkUtr" must {

    "return true when an SA User has a matching utr and no agent token is passed" in {
      when(mockServicesConfig.getInt("taxYear")).thenReturn(2023)
      when(mockServicesConfig.getInt("max.taxYears.to.display")).thenReturn(5)

      val taxYears = taxYearsFindersUtils.getTaxYears
      taxYears.size mustBe 5
      taxYears mustBe List(2023, 2022, 2021, 2020, 2019)
    }
  }

}
