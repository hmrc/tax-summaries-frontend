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

    def payload(key: String): Amount =
      atsData.summary_data.flatMap(_.payload.flatMap(_.get(key))).getOrElse(Amount.empty)

    def rates(key: String): Rate =
      atsData.summary_data.flatMap(_.rates.flatMap(_.get(key))).getOrElse(Rate.empty)

    def taxpayerName(key: String): String =
      atsData.taxPayerData.flatMap(_.taxpayer_name.flatMap(_.get(key))).getOrElse("")

    Summary(atsData.taxYear,
      atsData.utr.getOrElse(""),
      payload("employee_nic_amount"),
      payload("employer_nic_amount"),
      payload("total_income_tax_and_nics"),
      payload("your_total_tax"),
      payload("personal_tax_free_amount"),
      payload("total_tax_free_amount"),
      payload("total_income_before_tax"),
      payload("total_income_tax"),
      payload("total_cg_tax"),
      payload("taxable_gains"),
      payload("cg_tax_per_currency_unit"),
      payload("nics_and_tax_per_currency_unit"),
      payload("income_after_tax_and_nics"),
      payload("nics_and_tax_rate_amount"),
      rates("total_cg_tax_rate"),
      rates("nics_and_tax_rate"),
      taxpayerName("title"),
      taxpayerName("forename"),
      taxpayerName("surname")
    )
  }
}
