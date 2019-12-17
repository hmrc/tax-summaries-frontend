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
import models.{AtsData, DataHolder, UserData}
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
    val emptyAmount = Amount(0.0, "GBP")
    val emptyRate = Rate("0")
    val emptyUserData = UserData(Some(Map("title" -> "", "forename" -> "", "surname" ->" ")))
    val incomeTaxData: DataHolder = atsData.income_tax.get
    val rates: Map[String, Rate] = incomeTaxData.rates.getOrElse(Map())
    val taxPayerData: UserData  = atsData.taxPayerData.getOrElse(emptyUserData)


    TotalIncomeTax(atsData.taxYear,
      atsData.utr.getOrElse(""),
      incomeTaxData.payload.get.getOrElse("starting_rate_for_savings", emptyAmount),
      incomeTaxData.payload.get.getOrElse("starting_rate_for_savings_amount", emptyAmount),
      incomeTaxData.payload.get.getOrElse("basic_rate_income_tax", emptyAmount),
      incomeTaxData.payload.get.getOrElse("basic_rate_income_tax_amount", emptyAmount),
      incomeTaxData.payload.get.getOrElse("higher_rate_income_tax", emptyAmount),
      incomeTaxData.payload.get.getOrElse("higher_rate_income_tax_amount", emptyAmount),
      incomeTaxData.payload.get.getOrElse("additional_rate_income_tax", emptyAmount),
      incomeTaxData.payload.get.getOrElse("additional_rate_income_tax_amount", emptyAmount),
      incomeTaxData.payload.get.getOrElse("ordinary_rate", emptyAmount),
      incomeTaxData.payload.get.getOrElse("ordinary_rate_amount", emptyAmount),
      incomeTaxData.payload.get.getOrElse("upper_rate", emptyAmount),
      incomeTaxData.payload.get.getOrElse("upper_rate_amount", emptyAmount),
      incomeTaxData.payload.get.getOrElse("additional_rate", emptyAmount),
      incomeTaxData.payload.get.getOrElse("additional_rate_amount", emptyAmount),
      incomeTaxData.payload.get.getOrElse("other_adjustments_increasing", emptyAmount),
      incomeTaxData.payload.get.getOrElse("marriage_allowance_received_amount", emptyAmount),
      incomeTaxData.payload.get.getOrElse("other_adjustments_reducing", emptyAmount),
      incomeTaxData.payload.get.getOrElse("total_income_tax", emptyAmount),
      incomeTaxData.payload.get.getOrElse("scottish_income_tax", emptyAmount),
      incomeTaxData.incomeTaxStatus.getOrElse(""),
      rates.getOrElse("starting_rate_for_savings_rate", emptyRate),
      rates.getOrElse("basic_rate_income_tax_rate", emptyRate),
      rates.getOrElse("higher_rate_income_tax_rate", emptyRate),
      rates.getOrElse("additional_rate_income_tax_rate", emptyRate),
      rates.getOrElse("ordinary_rate_tax_rate", emptyRate),
      rates.getOrElse("upper_rate_rate", emptyRate),
      rates.getOrElse("additional_rate_rate", emptyRate),
      taxPayerData.taxpayer_name.get("title"),
      taxPayerData.taxpayer_name.get("forename"),
      taxPayerData.taxpayer_name.get("surname")
    )
  }
}
