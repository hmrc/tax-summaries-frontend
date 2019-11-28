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
import view_models.{Amount, Rate, TotalIncomeTax}

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
        payload("total_income_tax"),
        payload("scottish_income_tax"),
        atsData.income_tax.flatMap(_.incomeTaxStatus).getOrElse(""),
        rates("starting_rate_for_savings_rate"),
        rates("basic_rate_income_tax_rate"),
        rates("higher_rate_income_tax_rate"),
        rates("additional_rate_income_tax_rate"),
        rates("ordinary_rate_tax_rate"),
        rates("upper_rate_rate"),
        rates("additional_rate_rate"),
        taxpayerName("title"),
        taxpayerName("forename"),
        taxpayerName("surname")
      )
    }
}
