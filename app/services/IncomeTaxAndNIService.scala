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
import view_models._

import scala.concurrent.Future

class IncomeTaxAndNIService @Inject() (atsService: AtsService) {

  def getIncomeAndNIData(
    taxYear: Int
  )(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[GenericViewModel] =
    atsService.createModel(taxYear, totalIncomeConverter)

  // scalastyle:off method.length
  private[services] def totalIncomeConverter(atsData: AtsData): IncomeTaxAndNI = {
    def payload(key: String): Amount =
      atsData.income_tax.flatMap(_.payload.flatMap(_.get(key))).getOrElse(Amount.empty)

    def rates(key: String): Rate =
      atsData.income_tax.flatMap(_.rates.flatMap(_.get(key))).getOrElse(Rate.empty)

    def taxpayerName(key: String): String =
      atsData.taxPayerData.flatMap(_.taxpayer_name.flatMap(_.get(key))).getOrElse("")

    val scottishTax = ScottishTax(
      payload("scottish_starter_rate_tax"),
      payload("scottish_starter_income"),
      payload("scottish_basic_rate_tax"),
      payload("scottish_basic_income"),
      payload("scottish_intermediate_rate_tax"),
      payload("scottish_intermediate_income"),
      payload("scottish_higher_rate_tax"),
      payload("scottish_higher_income"),
      payload("scottish_additional_rate_tax"),
      payload("scottish_additional_income"),
      payload("scottish_total_tax")
    )

    val savingsTax = SavingsTax(
      payload("savings_lower_rate_tax"),
      payload("savings_lower_income"),
      payload("savings_higher_rate_tax"),
      payload("savings_higher_income"),
      payload("savings_additional_rate_tax"),
      payload("savings_additional_income")
    )

    val scottishRates = ScottishRates(
      rates("scottish_starter_rate"),
      rates("scottish_basic_rate"),
      rates("scottish_intermediate_rate"),
      rates("scottish_higher_rate"),
      rates("scottish_additional_rate")
    )

    val savingsRates = SavingsRates(
      rates("savings_lower_rate"),
      rates("savings_higher_rate"),
      rates("savings_additional_rate")
    )

    val summaryData: DataHolder = atsData.summary_data.get
    IncomeTaxAndNI(
      year = atsData.taxYear,
      utr = atsData.utr.get,
      employeeNicAmount = summaryData.payload.get("employee_nic_amount"),
      totalIncomeTaxAndNics = summaryData.payload.get("total_income_tax_and_nics"),
      yourTotalTax = summaryData.payload.get("your_total_tax"),
      totalTaxFree = summaryData.payload.get("personal_tax_free_amount"),
      totalTaxFreeAllowance = summaryData.payload.get("total_tax_free_amount"),
      yourIncomeBeforeTax = summaryData.payload.get("total_income_before_tax"),
      totalIncomeTaxAmount = summaryData.payload.get("total_income_tax"),
      totalCapitalGainsTax = summaryData.payload.get("total_cg_tax"),
      taxableGains = summaryData.payload.get("taxable_gains"),
      cgTaxPerCurrencyUnit = summaryData.payload.get("cg_tax_per_currency_unit"),
      nicsAndTaxPerCurrencyUnit = summaryData.payload.get("nics_and_tax_per_currency_unit"),
      totalCgTaxRate = summaryData.rates.get("total_cg_tax_rate"),
      nicsAndTaxRate = summaryData.rates.get("nics_and_tax_rate"),
      startingRateForSavings = payload("starting_rate_for_savings"),
      startingRateForSavingsAmount = payload("starting_rate_for_savings_amount"),
      basicRateIncomeTax = payload("basic_rate_income_tax"),
      basicRateIncomeTaxAmount = payload("basic_rate_income_tax_amount"),
      higherRateIncomeTax = payload("higher_rate_income_tax"),
      higherRateIncomeTaxAmount = payload("higher_rate_income_tax_amount"),
      additionalRateIncomeTax = payload("additional_rate_income_tax"),
      additionalRateIncomeTaxAmount = payload("additional_rate_income_tax_amount"),
      ordinaryRate = payload("ordinary_rate"),
      ordinaryRateAmount = payload("ordinary_rate_amount"),
      upperRate = payload("upper_rate"),
      upperRateAmount = payload("upper_rate_amount"),
      additionalRate = payload("additional_rate"),
      additionalRateAmount = payload("additional_rate_amount"),
      otherAdjustmentsIncreasing = payload("other_adjustments_increasing"),
      marriageAllowanceReceivedAmount = payload("marriage_allowance_received_amount"),
      otherAdjustmentsReducing = -payload("other_adjustments_reducing"),
      scottishTax = scottishTax,
      totalIncomeTax = payload("total_income_tax"),
      scottishIncomeTax = payload("scottish_income_tax"),
      welshIncomeTax = payload("welsh_income_tax"),
      savingsTax = savingsTax,
      incomeTaxStatus = atsData.income_tax.flatMap(_.incomeTaxStatus).getOrElse(""),
      startingRateForSavingsRateRate = rates("starting_rate_for_savings_rate"),
      basicRateIncomeTaxRateRate = rates("basic_rate_income_tax_rate"),
      higherRateIncomeTaxRateRate = rates("higher_rate_income_tax_rate"),
      additionalRateIncomeTaxRateRate = rates("additional_rate_income_tax_rate"),
      ordinaryRateTaxRateRate = rates("ordinary_rate_tax_rate"),
      upperRateRateRate = rates("upper_rate_rate"),
      additionalRateRateRate = rates("additional_rate_rate"),
      scottishRates = scottishRates,
      savingsRates = savingsRates,
      title = taxpayerName("title"),
      forename = taxpayerName("forename"),
      surname = taxpayerName("surname")
    )
  }
}
