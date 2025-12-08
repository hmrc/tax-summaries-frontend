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

import com.google.inject.Inject
import models.requests.AuthenticatedRequest
import models.{AtsData, DataHolder}
import uk.gov.hmrc.http.HeaderCarrier
import utils.GenericViewModel
import view_models.Summary

import scala.concurrent.Future

class SummaryService @Inject() (atsService: AtsService) {

  def getSummaryData(
    taxYear: Int
  )(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[GenericViewModel] =
    atsService.createModel(taxYear, summaryConverter)

  private[services] def summaryConverter(atsData: AtsData): Summary = {
    val summaryData: DataHolder = atsData.summary_data.get

    Summary(
      atsData.taxYear,
      atsData.utr.get,
      summaryData.payload.get("employee_nic_amount"),
      summaryData.payload.get("total_income_tax_and_nics"),
      summaryData.payload.get("your_total_tax"),
      summaryData.payload.get("personal_tax_free_amount"),
      summaryData.payload.get("total_tax_free_amount"),
      summaryData.payload.get("total_income_before_tax"),
      summaryData.payload.get("total_income_tax"),
      summaryData.payload.get("total_cg_tax"),
      summaryData.payload.get("taxable_gains"),
      summaryData.payload.get("cg_tax_per_currency_unit"),
      summaryData.payload.get("nics_and_tax_per_currency_unit"),
      summaryData.rates.get("total_cg_tax_rate"),
      summaryData.rates.get("nics_and_tax_rate"),
      atsData.taxPayerData.getOrElse("title", ""),
      atsData.taxPayerData.getOrElse("forename", ""),
      atsData.taxPayerData.getOrElse("surname", "")
    )
  }
}
