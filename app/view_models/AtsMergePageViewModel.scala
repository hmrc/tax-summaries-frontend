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
import controllers.auth.AuthenticatedRequest
import models.{AtsType, AtsYearChoice, NoATS, PAYE, SA}
import uk.gov.hmrc.auth.core.ConfidenceLevel

case class AtsMergePageViewModel(saData: AtsList, payeTaxYearList: List[Int], appConfig: ApplicationConfig)(
  implicit request: AuthenticatedRequest[_]) {

  val showSaYearList: Boolean = saData.yearList.nonEmpty

  val showPayeYearList: Boolean = payeTaxYearList.nonEmpty

  val saAndPayeTaxYearList = saData.yearList ::: payeTaxYearList

  val totalTaxYearList = ((appConfig.taxYear - appConfig.maxTaxYearsTobeDisplayed) to appConfig.taxYear).toList

  val noAtsTaxYearList = totalTaxYearList.filterNot(saAndPayeTaxYearList.toSet)

  val showNoAtsText = noAtsTaxYearList.filter(_ < 2019).nonEmpty

  val noAtsYearListAvailable = noAtsTaxYearList.filter(_ >= 2019)

  val showNoAtsYearList = noAtsYearListAvailable.nonEmpty

  val showIvUpliftLink = showPayeYearList && (request.confidenceLevel.compare(ConfidenceLevel.L200) < 0)

  val showContinueButton = (showSaYearList || (showPayeYearList && !showIvUpliftLink) || showNoAtsYearList)

  val saDataYearChoiceList = saData.getDescendingYearList.map(year => AtsYearChoice(SA, year))

  val payeTaxYearChoiceList = payeTaxYearList.map(year => AtsYearChoice(PAYE, year))

  val noAtsYearChoiceList = noAtsYearListAvailable.map(year => AtsYearChoice(NoATS, year))

  val completeYearList =
    (saDataYearChoiceList ::: payeTaxYearChoiceList ::: noAtsYearChoiceList).sortBy(_.year)(Ordering.Int.reverse)

}
