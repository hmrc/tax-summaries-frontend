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
import models.AtsData
import uk.gov.hmrc.http.HeaderCarrier
import utils.GenericViewModel
import view_models._

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

    TotalIncomeTax(
      atsData.taxYear,
      atsData.utr.getOrElse(""),
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
      payload("other_adjustments_reducing"),
      scottishTax,
      payload("total_income_tax"),
      payload("scottish_income_tax"),
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
      payload("less_tax_adjustment_previous_year"),
      payload("tax_underpaid_previous_year"),
      taxpayerName("title"),
      taxpayerName("forename"),
      taxpayerName("surname")
    )
  }
}
