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

import com.google.inject.Inject
import controllers.auth.AuthenticatedRequest
import models.{AtsData, DataHolder}
import uk.gov.hmrc.http.HeaderCarrier
import utils.GenericViewModel
import view_models.CapitalGains

import scala.concurrent.Future

class CapitalGainsService @Inject()() {
  def atsService: AtsService = AtsService
  def atsYearListService: AtsYearListService = AtsYearListService

  def getCapitalGains(taxYear: Int)(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[GenericViewModel] = {
    atsService.createModel(taxYear, capitalGains)
  }

  private[services] def capitalGains(atsData: AtsData): CapitalGains = {
      val capitalGainsData: DataHolder = atsData.capital_gains_data.get
      CapitalGains(atsData.taxYear,
        atsData.utr.get,
        capitalGainsData.payload.get("taxable_gains"),
        capitalGainsData.payload.get("less_tax_free_amount"),
        capitalGainsData.payload.get("pay_cg_tax_on"),
        capitalGainsData.payload.get("amount_at_entrepreneurs_rate"),
        capitalGainsData.payload.get("amount_due_at_entrepreneurs_rate"),
        capitalGainsData.payload.get("amount_at_ordinary_rate"),
        capitalGainsData.payload.get("amount_due_at_ordinary_rate"),
        capitalGainsData.payload.get("amount_at_higher_rate"),
        capitalGainsData.payload.get("amount_due_at_higher_rate"),
        capitalGainsData.payload.get("amount_due_rpci_lower_rate"),
        capitalGainsData.payload.get("amount_at_rpci_lower_rate"),
        capitalGainsData.payload.get("amount_due_rpci_higher_rate"),
        capitalGainsData.payload.get("amount_at_rpci_higher_rate"),
        capitalGainsData.payload.get("adjustments"),
        capitalGainsData.payload.get("total_cg_tax"),
        capitalGainsData.payload.get("cg_tax_per_currency_unit"),
        capitalGainsData.rates.get("cg_entrepreneurs_rate"),
        capitalGainsData.rates.get("cg_ordinary_rate"),
        capitalGainsData.rates.get("cg_upper_rate"),
        capitalGainsData.rates.get("prop_interest_rate_lower_rate"),
        capitalGainsData.rates.get("prop_interest_rate_higher_rate"),
        capitalGainsData.rates.get("total_cg_tax_rate"),
        atsData.taxPayerData.get.taxpayer_name.get("title"),
        atsData.taxPayerData.get.taxpayer_name.get("forename"),
        atsData.taxPayerData.get.taxpayer_name.get("surname")
      )
    }
}
