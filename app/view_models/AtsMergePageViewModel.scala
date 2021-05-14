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

import config.ApplicationConfig

case class AtsMergePageViewModel(saData: AtsList, payeTaxYearList: List[Int], appConfig: ApplicationConfig) {

  val showSaYearList: Boolean = saData.yearList.nonEmpty
  val showPayeYearList: Boolean = payeTaxYearList.nonEmpty
  val noAtsTaxYearList =
    (appConfig.taxYear - appConfig.maxTaxYearsTobeDisplayed to appConfig.taxYear).toList
      .diff(saData.yearList ++ payeTaxYearList)
  val showNoAtsText = noAtsTaxYearList.filter(_ < 2019).nonEmpty
  val noAtsYearListAvailable = noAtsTaxYearList.filter(_ >= 2019)
  val showNoAtsYearList = noAtsYearListAvailable.nonEmpty

}
