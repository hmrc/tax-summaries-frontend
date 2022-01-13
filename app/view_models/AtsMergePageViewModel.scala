/*
 * Copyright 2022 HM Revenue & Customs
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
import models.{AtsYearChoice, NoATS, PAYE, SA}
import uk.gov.hmrc.auth.core.ConfidenceLevel

case class AtsMergePageViewModel(
  saData: AtsList,
  payeTaxYearList: List[Int],
  appConfig: ApplicationConfig,
  confidenceLevel: ConfidenceLevel) {

  private val saAndPayeTaxYearList = saData.yearList ::: payeTaxYearList

  private val totalTaxYearList = ((appConfig.taxYear - appConfig.maxTaxYearsTobeDisplayed) to appConfig.taxYear).toList

  private val showSaYearList: Boolean = saData.yearList.nonEmpty

  private val showPayeYearList: Boolean = payeTaxYearList.nonEmpty

  private val onlyPaye: Boolean = showPayeYearList && !showSaYearList && !showNoAtsYearList

  val showIvUpliftLink: Boolean = showPayeYearList && (confidenceLevel.compare(ConfidenceLevel.L200) < 0)
  val completeYearList: List[AtsYearChoice] = {
    val saDataYearChoiceList = saData.getDescendingYearList.map(year => AtsYearChoice(SA, year))
    val payeTaxYearChoiceList = payeTaxYearList.map(year => AtsYearChoice(PAYE, year))
    val noAtsYearChoiceList =
      totalTaxYearList.filterNot(saAndPayeTaxYearList.toSet).filter(_ >= 2019).map(year => AtsYearChoice(NoATS, year))

    if (showIvUpliftLink) {
      (saDataYearChoiceList ::: noAtsYearChoiceList).sortBy(_.year)(Ordering.Int.reverse)
    } else {
      (saDataYearChoiceList ::: payeTaxYearChoiceList ::: noAtsYearChoiceList).sortBy(_.year)(Ordering.Int.reverse)
    }
  }
  val showNoAtsText: Boolean = totalTaxYearList.filterNot(saAndPayeTaxYearList.toSet).filter(_ < 2019).nonEmpty
  val showNoAtsYearList: Boolean = totalTaxYearList.filterNot(saAndPayeTaxYearList.toSet).filter(_ >= 2019).nonEmpty
  val showContinueButton: Boolean = (showSaYearList || (showPayeYearList && !showIvUpliftLink) || showNoAtsYearList)
  val name = s"${saData.forename} ${saData.surname}"
  val titleMsg = if (onlyPaye && showIvUpliftLink) { "merge.page.paye.ivuplift.header" } else {
    "merge.page.ats.select_tax_year.title"
  }
  val subtitleMsg = if (onlyPaye && showIvUpliftLink) { "merge.page.paye.ivuplift.header" } else {
    "merge.page.ats.select_tax_year.title"
  }
}
