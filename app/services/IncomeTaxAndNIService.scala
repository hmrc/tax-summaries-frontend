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
import view_models._

import scala.concurrent.Future

class IncomeTaxAndNIService @Inject() (atsService: AtsService) {

  def getIncomeAndNIData(
    taxYear: Int
  )(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[GenericViewModel] =
    atsService.createModel(taxYear, totalIncomeConverter)

  // scalastyle:off method.length
  private[services] def totalIncomeConverter(atsData: AtsData): IncomeTaxAndNI = {
    def getFromIncomeTaxPayload(key: String): Amount =
      atsData.income_tax.flatMap(_.payload.flatMap(_.get(key))).getOrElse(Amount.empty)

    val summaryData: DataHolder = atsData.summary_data.fold(DataHolder(None, None, None))(identity)

    def getFromSummaryPayload(key: String): Amount = summaryData.payload
      .flatMap(_.get(key))
      .getOrElse(Amount.empty)

    def getFromSummaryRates(key: String): Rate =
      atsData.summary_data.flatMap(_.rates.flatMap(_.get(key))).getOrElse(Rate.empty)

    def getFromIncomeTaxRates(key: String): Rate =
      atsData.income_tax.flatMap(_.rates.flatMap(_.get(key))).getOrElse(Rate.empty)

    def taxpayerName(key: String): String =
      atsData.taxPayerData.getOrElse(key, "")

    val scottishTax = ScottishTax(
      getFromIncomeTaxPayload("scottish_starter_rate_tax"),
      getFromIncomeTaxPayload("scottish_starter_income"),
      getFromIncomeTaxPayload("scottish_basic_rate_tax"),
      getFromIncomeTaxPayload("scottish_basic_income"),
      getFromIncomeTaxPayload("scottish_intermediate_rate_tax"),
      getFromIncomeTaxPayload("scottish_intermediate_income"),
      getFromIncomeTaxPayload("scottish_higher_rate_tax"),
      getFromIncomeTaxPayload("scottish_higher_income"),
      getFromIncomeTaxPayload("scottish_advanced_rate_tax"),
      getFromIncomeTaxPayload("scottish_advanced_income"),
      getFromIncomeTaxPayload("scottish_additional_rate_tax"),
      getFromIncomeTaxPayload("scottish_additional_income"),
      getFromIncomeTaxPayload("scottish_top_rate_tax"),
      getFromIncomeTaxPayload("scottish_top_income"),
      getFromIncomeTaxPayload("scottish_total_tax")
    )

    val savingsTax = SavingsTax(
      getFromIncomeTaxPayload("savings_lower_rate_tax"),
      getFromIncomeTaxPayload("savings_lower_income"),
      getFromIncomeTaxPayload("savings_higher_rate_tax"),
      getFromIncomeTaxPayload("savings_higher_income"),
      getFromIncomeTaxPayload("savings_additional_rate_tax"),
      getFromIncomeTaxPayload("savings_additional_income")
    )

    val scottishRates = ScottishRates(
      getFromIncomeTaxRates("scottish_starter_rate"),
      getFromIncomeTaxRates("scottish_basic_rate"),
      getFromIncomeTaxRates("scottish_intermediate_rate"),
      getFromIncomeTaxRates("scottish_higher_rate"),
      getFromIncomeTaxRates("scottish_advanced_rate"),
      getFromIncomeTaxRates("scottish_additional_rate"),
      getFromIncomeTaxRates("scottish_top_rate")
    )

    val savingsRates = SavingsRates(
      getFromIncomeTaxRates("savings_lower_rate"),
      getFromIncomeTaxRates("savings_higher_rate"),
      getFromIncomeTaxRates("savings_additional_rate")
    )

    IncomeTaxAndNI(
      year = atsData.taxYear,
      utr = atsData.utr.get,
      employeeNicAmount = getFromSummaryPayload("employee_nic_amount"),
      totalIncomeTaxAndNics = getFromSummaryPayload("total_income_tax_and_nics"),
      yourTotalTax = getFromSummaryPayload("your_total_tax"),
      totalTaxFree = getFromSummaryPayload("personal_tax_free_amount"),
      totalTaxFreeAllowance = getFromSummaryPayload("total_tax_free_amount"),
      yourIncomeBeforeTax = getFromSummaryPayload("total_income_before_tax"),
      totalIncomeTaxAmount = getFromSummaryPayload("total_income_tax"),
      totalCapitalGainsTax = getFromSummaryPayload("total_cg_tax"),
      taxableGains = getFromSummaryPayload("taxable_gains"),
      cgTaxPerCurrencyUnit = getFromSummaryPayload("cg_tax_per_currency_unit"),
      nicsAndTaxPerCurrencyUnit = getFromSummaryPayload("nics_and_tax_per_currency_unit"),
      totalCgTaxRate = getFromSummaryRates("total_cg_tax_rate"),
      nicsAndTaxRate = getFromSummaryRates("nics_and_tax_rate"),
      startingRateForSavings = getFromIncomeTaxPayload("starting_rate_for_savings"),
      startingRateForSavingsAmount = getFromIncomeTaxPayload("starting_rate_for_savings_amount"),
      basicRateIncomeTax = getFromIncomeTaxPayload("basic_rate_income_tax"),
      basicRateIncomeTaxAmount = getFromIncomeTaxPayload("basic_rate_income_tax_amount"),
      higherRateIncomeTax = getFromIncomeTaxPayload("higher_rate_income_tax"),
      higherRateIncomeTaxAmount = getFromIncomeTaxPayload("higher_rate_income_tax_amount"),
      additionalRateIncomeTax = getFromIncomeTaxPayload("additional_rate_income_tax"),
      additionalRateIncomeTaxAmount = getFromIncomeTaxPayload("additional_rate_income_tax_amount"),
      ordinaryRate = getFromIncomeTaxPayload("ordinary_rate"),
      ordinaryRateAmount = getFromIncomeTaxPayload("ordinary_rate_amount"),
      upperRate = getFromIncomeTaxPayload("upper_rate"),
      upperRateAmount = getFromIncomeTaxPayload("upper_rate_amount"),
      additionalRate = getFromIncomeTaxPayload("additional_rate"),
      additionalRateAmount = getFromIncomeTaxPayload("additional_rate_amount"),
      otherAdjustmentsIncreasing = getFromIncomeTaxPayload("other_adjustments_increasing"),
      marriageAllowanceReceivedAmount = getFromIncomeTaxPayload("marriage_allowance_received_amount"),
      otherAdjustmentsReducing = -getFromIncomeTaxPayload("other_adjustments_reducing"),
      scottishTax = scottishTax,
      totalIncomeTax = getFromIncomeTaxPayload("total_income_tax"),
      scottishIncomeTax = getFromIncomeTaxPayload("scottish_income_tax"),
      welshIncomeTax = getFromIncomeTaxPayload("welsh_income_tax"),
      savingsTax = savingsTax,
      incomeTaxStatus = atsData.income_tax.flatMap(_.incomeTaxStatus).getOrElse(""),
      startingRateForSavingsRateRate = getFromIncomeTaxRates("starting_rate_for_savings_rate"),
      basicRateIncomeTaxRateRate = getFromIncomeTaxRates("basic_rate_income_tax_rate"),
      higherRateIncomeTaxRateRate = getFromIncomeTaxRates("higher_rate_income_tax_rate"),
      additionalRateIncomeTaxRateRate = getFromIncomeTaxRates("additional_rate_income_tax_rate"),
      ordinaryRateTaxRateRate = getFromIncomeTaxRates("ordinary_rate_tax_rate"),
      upperRateRateRate = getFromIncomeTaxRates("upper_rate_rate"),
      additionalRateRateRate = getFromIncomeTaxRates("additional_rate_rate"),
      scottishRates = scottishRates,
      savingsRates = savingsRates,
      title = taxpayerName("title"),
      forename = taxpayerName("forename"),
      surname = taxpayerName("surname"),
      includeBRDMessage = getFromIncomeTaxPayload("brdReduction").amount > 0 ||
        getFromIncomeTaxPayload("brdCharge").amount > 0
    )
  }
}
