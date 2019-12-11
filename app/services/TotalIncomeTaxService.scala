/*
 * Copyright 2019 HM Revenue & Customs
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
import models.{AtsData, DataHolder}
import uk.gov.hmrc.http.HeaderCarrier
import utils.GenericViewModel
import view_models.TotalIncomeTax

import scala.concurrent.Future

object TotalIncomeTaxService extends TotalIncomeTaxService {
  override val atsService = AtsService
  override val atsYearListService = AtsYearListService
}

trait TotalIncomeTaxService {
  def atsService: AtsService
  def atsYearListService: AtsYearListService

  def getIncomeData(taxYear:Int)(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[GenericViewModel] = {
    atsService.createModel(taxYear, totalIncomeConverter)
  }

  private[services] def totalIncomeConverter(atsData: AtsData): TotalIncomeTax = {
      val incomeTaxData: DataHolder = atsData.income_tax.get

      TotalIncomeTax(atsData.taxYear,
        atsData.utr.getOrElse(""),
        incomeTaxData.payload.get("starting_rate_for_savings"),
        incomeTaxData.payload.get("starting_rate_for_savings_amount"),
        incomeTaxData.payload.get("basic_rate_income_tax"),
        incomeTaxData.payload.get("basic_rate_income_tax_amount"),
        incomeTaxData.payload.get("higher_rate_income_tax"),
        incomeTaxData.payload.get("higher_rate_income_tax_amount"),
        incomeTaxData.payload.get("additional_rate_income_tax"),
        incomeTaxData.payload.get("additional_rate_income_tax_amount"),
        incomeTaxData.payload.get("ordinary_rate"),
        incomeTaxData.payload.get("ordinary_rate_amount"),
        incomeTaxData.payload.get("upper_rate"),
        incomeTaxData.payload.get("upper_rate_amount"),
        incomeTaxData.payload.get("additional_rate"),
        incomeTaxData.payload.get("additional_rate_amount"),
        incomeTaxData.payload.get("other_adjustments_increasing"),
        incomeTaxData.payload.get("marriage_allowance_received_amount"),
        incomeTaxData.payload.get("other_adjustments_reducing"),
        incomeTaxData.payload.get("total_income_tax"),
        incomeTaxData.payload.get("scottish_income_tax"),
        incomeTaxData.incomeTaxStatus.get,
        incomeTaxData.rates.get("starting_rate_for_savings_rate"),
        incomeTaxData.rates.get("basic_rate_income_tax_rate"),
        incomeTaxData.rates.get("higher_rate_income_tax_rate"),
        incomeTaxData.rates.get("additional_rate_income_tax_rate"),
        incomeTaxData.rates.get("ordinary_rate_tax_rate"),
        incomeTaxData.rates.get("upper_rate_rate"),
        incomeTaxData.rates.get("additional_rate_rate"),
        atsData.taxPayerData.get.taxpayer_name.get("title"),
        atsData.taxPayerData.get.taxpayer_name.get("forename"),
        atsData.taxPayerData.get.taxpayer_name.get("surname")
      )
    }
}
