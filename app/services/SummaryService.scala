/*
 * Copyright 2020 HM Revenue & Customs
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

import controllers.auth.AuthenticatedRequest
import models.{AtsData, DataHolder, UserData}
import uk.gov.hmrc.http.HeaderCarrier
import utils.GenericViewModel
import view_models.{Amount, Rate, Summary}

import scala.concurrent.Future

object SummaryService extends SummaryService {
  override val atsService = AtsService
  override val atsYearListService = AtsYearListService
}

trait SummaryService {

  def atsService: AtsService
  def atsYearListService: AtsYearListService

  def getSummaryData(taxYear:Int)(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[GenericViewModel] = {
    atsService.createModel(taxYear, summaryConverter)
  }

  private[services] def summaryConverter(atsData: AtsData): Summary = {

    val emptyAmount = Amount(0.0, "GBP")
    val emptyRate = Rate("0")
    val emptyUserData = UserData(Some(Map("title" -> "", "forename" -> "", "surname" ->" ")))

    val summaryData: DataHolder = atsData.summary_data.get
    val taxPayerData: UserData  = atsData.taxPayerData.getOrElse(emptyUserData)
    val rates: Map[String, Rate] = summaryData.rates.getOrElse(Map())
    Summary(atsData.taxYear,
      atsData.utr.getOrElse(""),
      summaryData.payload.get.getOrElse("employee_nic_amount", emptyAmount),
      summaryData.payload.get.getOrElse("employer_nic_amount", emptyAmount),
      summaryData.payload.get.getOrElse("total_income_tax_and_nics", emptyAmount),
      summaryData.payload.get.getOrElse("your_total_tax", emptyAmount),
      summaryData.payload.get.getOrElse("personal_tax_free_amount", emptyAmount),
      summaryData.payload.get.getOrElse("total_tax_free_amount", emptyAmount),
      summaryData.payload.get.getOrElse("total_income_before_tax", emptyAmount),
      summaryData.payload.get.getOrElse("total_income_tax", emptyAmount),
      summaryData.payload.get.getOrElse("total_cg_tax", emptyAmount),
      summaryData.payload.get.getOrElse("taxable_gains", emptyAmount),
      summaryData.payload.get.getOrElse("cg_tax_per_currency_unit", emptyAmount),
      summaryData.payload.get.getOrElse("nics_and_tax_per_currency_unit", emptyAmount),
      summaryData.payload.get.getOrElse("income_after_tax_and_nics", emptyAmount),
      rates.getOrElse("total_cg_tax_rate", emptyRate),
      summaryData.payload.get.getOrElse("nics_and_tax_rate", emptyAmount),
      taxPayerData.taxpayer_name.get("title"),
      taxPayerData.taxpayer_name.get("forename"),
      taxPayerData.taxpayer_name.get("surname")
    )
  }
}
