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

package services.paye

import controllers.auth.AuthenticatedRequest
import models.AtsData
import uk.gov.hmrc.http.HeaderCarrier
import utils.GenericViewModel
import view_models.paye.PayeIncomeBeforeTax
import view_models.{Amount, IncomeBeforeTax}

import scala.concurrent.Future

object PayeIncomeService extends PayeIncomeService {
  override val atsService = PayeAtsService
}

trait PayeIncomeService {
  def atsService: PayeAtsService

  def getIncomeData(taxYear:Int)(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[GenericViewModel] = {
    atsService.createModel(taxYear, createIncomeConverter)
  }

  private[services] def createIncomeConverter(atsData: AtsData): PayeIncomeBeforeTax = {
    def payload(key: String): Amount =
      atsData.income_data.flatMap(_.payload.flatMap(_.get(key))).getOrElse(Amount.empty)

    def taxpayerName(key: String): String =
      atsData.taxPayerData.flatMap(_.taxpayer_name.flatMap(_.get(key))).getOrElse("")

      PayeIncomeBeforeTax(atsData.taxYear,
        payload("self_employment_income"),
        payload("income_from_employment"),
        payload("state_pension"),
        payload("other_pension_income"),
        payload("taxable_state_benefits"),
        payload("other_income"),
        payload("benefits_from_employment"),
        payload("total_income_before_tax")
      )
    }
}
