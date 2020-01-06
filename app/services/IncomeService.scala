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

import controllers.auth.AuthenticatedRequest
import models.{AtsData, DataHolder, UserData}
import uk.gov.hmrc.http.HeaderCarrier
import utils.GenericViewModel
import view_models.{Amount, IncomeBeforeTax}

import scala.concurrent.Future

object IncomeService extends IncomeService {
  override val atsService = AtsService
  override val atsYearListService = AtsYearListService
}

trait IncomeService {
  def atsService: AtsService
  def atsYearListService: AtsYearListService

  def getIncomeData(taxYear:Int)(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[GenericViewModel] = {
    atsService.createModel(taxYear, createIncomeConverter)
  }

  private[services] def createIncomeConverter(atsData: AtsData): IncomeBeforeTax = {
      val incomeData: DataHolder = atsData.income_data.get
      val taxPayerData: UserData = atsData.taxPayerData.getOrElse(UserData(Some(Map("title"-> "Mr", "forename" -> "Joe", "surname" -> "Bloggs"))))
      val emptyAmount = Amount(0, "GBP")
      IncomeBeforeTax(atsData.taxYear,
        atsData.utr.getOrElse(""),
        incomeData.payload.get.getOrElse("self_employment_income", emptyAmount),
        incomeData.payload.get("income_from_employment"),
        incomeData.payload.get("state_pension"),
        incomeData.payload.get("other_pension_income"),
        incomeData.payload.get.getOrElse("taxable_state_benefits", emptyAmount),
        incomeData.payload.get("other_income"),
        incomeData.payload.get.getOrElse("benefits_from_employment", emptyAmount),
        incomeData.payload.get("total_income_before_tax"),
        taxPayerData.taxpayer_name.get("title"),
        taxPayerData.taxpayer_name.get("forename"),
        taxPayerData.taxpayer_name.get("surname")
      )
    }
}
