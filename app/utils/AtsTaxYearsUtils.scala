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

package utils

import com.google.inject.Inject
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class AtsTaxYearsUtils @Inject() (config: ServicesConfig) {
  def getTaxYears: List[Int] = {
    val startTaxYear       = config.getInt("taxYear")
    val totalNumberOfYears = config.getInt("max.taxYears.to.display")
    val listOfYears        = startTaxYear until startTaxYear - totalNumberOfYears by -1
    listOfYears.toList
  }
}
