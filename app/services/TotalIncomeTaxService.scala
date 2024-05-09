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

class TotalIncomeTaxService @Inject() (atsService: AtsService) {

  def getIncomeData(
    taxYear: Int
  )(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[GenericViewModel] =
    atsService.createModel(taxYear, totalIncomeConverter)

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
      atsData.taxPayerData.get.taxpayer_name.get("title"),
      atsData.taxPayerData.get.taxpayer_name.get("forename"),
      atsData.taxPayerData.get.taxpayer_name.get("surname")
    )
  }

  private[services] def totalIncomeConverter(atsData: AtsData): TotalIncomeTax = {
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
    val summary                 = Summary(
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
      atsData.taxPayerData.get.taxpayer_name.get("title"),
      atsData.taxPayerData.get.taxpayer_name.get("forename"),
      atsData.taxPayerData.get.taxpayer_name.get("surname")
    )

    TotalIncomeTax(
      summary,
      payload("starting_rate_for_savings"),
      payload("starting_rate_for_savings_amount"),
      payload("basic_rate_income_tax"),
      payload("basic_rate_income_tax_amount"),
      payload("higher_rate_income_tax"),
      payload("higher_rate_income_tax_amount"),
      payload("additional_rate_income_tax"),
      payload("additional_rate_income_tax_amount"),
      payload("ordinary_rate"),
      payload("ordinary_rate_amount"),
      payload("upper_rate"),
      payload("upper_rate_amount"),
      payload("additional_rate"),
      payload("additional_rate_amount"),
      payload("other_adjustments_increasing"),
      payload("marriage_allowance_received_amount"),
      -payload("other_adjustments_reducing"),
      scottishTax,
      payload("total_income_tax"),
      payload("scottish_income_tax"),
      payload("welsh_income_tax"),
      savingsTax,
      atsData.income_tax.flatMap(_.incomeTaxStatus).getOrElse(""),
      rates("starting_rate_for_savings_rate"),
      rates("basic_rate_income_tax_rate"),
      rates("higher_rate_income_tax_rate"),
      rates("additional_rate_income_tax_rate"),
      rates("ordinary_rate_tax_rate"),
      rates("upper_rate_rate"),
      rates("additional_rate_rate"),
      scottishRates,
      savingsRates,
      taxpayerName("title"),
      taxpayerName("forename"),
      taxpayerName("surname")
    )
  }
}
