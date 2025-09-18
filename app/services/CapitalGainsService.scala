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
import controllers.auth.requests.AuthenticatedRequest
import models.{AtsData, DataHolder}
import uk.gov.hmrc.http.HeaderCarrier
import utils.GenericViewModel
import view_models.CapitalGains

import scala.concurrent.Future

class CapitalGainsService @Inject() (atsService: AtsService) {

  def getCapitalGains(
    taxYear: Int
  )(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[GenericViewModel] =
    atsService.createModel(taxYear, capitalGains)

  private[services] def capitalGains(atsData: AtsData): CapitalGains = {
    val capitalGainsData: DataHolder = atsData.capital_gains_data.get

    CapitalGains(
      taxYear = atsData.taxYear,
      utr = atsData.utr.get,
      taxableGains = capitalGainsData.payload.get("taxable_gains"),
      lessTaxFreeAmount = -capitalGainsData.payload.get("less_tax_free_amount"),
      payCgTaxOn = capitalGainsData.payload.get("pay_cg_tax_on"),
      entrepreneursReliefRateBefore = capitalGainsData.payload.get("amount_at_entrepreneurs_rate"),
      entrepreneursReliefRateAmount = capitalGainsData.payload.get("amount_due_at_entrepreneurs_rate"),
      ordinaryRateBefore = capitalGainsData.payload.get("amount_at_ordinary_rate"),
      ordinaryRateAmount = capitalGainsData.payload.get("amount_due_at_ordinary_rate"),
      upperRateBefore = capitalGainsData.payload.get("amount_at_higher_rate"),
      upperRateAmount = capitalGainsData.payload.get("amount_due_at_higher_rate"),
      rpciLowerTax = capitalGainsData.payload.get("amount_due_rpci_lower_rate"),
      rpciLowerTotalAmount = capitalGainsData.payload.get("amount_at_rpci_lower_rate"),
      rpciHigherTax = capitalGainsData.payload.get("amount_due_rpci_higher_rate"),
      rpciHigherTotalAmount = capitalGainsData.payload.get("amount_at_rpci_higher_rate"),
      ciLowerTax = capitalGainsData.payload.get("amount_due_ci_lower_rate"),
      ciLowerTotalAmount = capitalGainsData.payload.get("amount_at_ci_lower_rate"),
      ciHigherTax = capitalGainsData.payload.get("amount_due_ci_higher_rate"),
      ciHigherTotalAmount = capitalGainsData.payload.get("amount_at_ci_higher_rate"),
      rpLowerTax = capitalGainsData.payload.get("amount_due_rp_lower_rate"),
      rpLowerTotalAmount = capitalGainsData.payload.get("amount_at_rp_lower_rate"),
      rpHigherTax = capitalGainsData.payload.get("amount_due_rp_higher_rate"),
      rpHigherTotalAmount = capitalGainsData.payload.get("amount_at_rp_higher_rate"),
      adjustmentsAmount = capitalGainsData.payload.get("adjustments"),
      totalCapitalGainsTaxAmount = capitalGainsData.payload.get("total_cg_tax"),
      cgTaxPerCurrencyUnit = capitalGainsData.payload.get("cg_tax_per_currency_unit"),
      entrepreneursReliefRateRate = capitalGainsData.rates.get("cg_entrepreneurs_rate"),
      ordinaryRateRate = capitalGainsData.rates.get("cg_ordinary_rate"),
      upperRateRate = capitalGainsData.rates.get("cg_upper_rate"),
      rpciLowerRate = capitalGainsData.rates.get("prop_interest_rate_lower_rate"),
      rpciHigherRate = capitalGainsData.rates.get("prop_interest_rate_higher_rate"),
      ciLowerRate = capitalGainsData.rates.get("ci_interest_rate_lower_rate"),
      ciHigherRate = capitalGainsData.rates.get("ci_interest_rate_higher_rate"),
      rpLowerRate = capitalGainsData.rates.get("rp_interest_rate_lower_rate"),
      rpHigherRate = capitalGainsData.rates.get("rp_interest_rate_higher_rate"),
      totalCgTaxRate = capitalGainsData.rates.get("total_cg_tax_rate"),
      title = atsData.taxPayerData.getOrElse("title", ""),
      forename = atsData.taxPayerData.getOrElse("forename", ""),
      surname = atsData.taxPayerData.getOrElse("surname", "")
    )
  }
}
