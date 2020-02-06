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
import view_models.Amount
import view_models.paye.PayeAllowances

import scala.concurrent.Future

object PayeAllowanceService extends PayeAllowanceService {
  override val atsService = PayeAtsService
}

trait PayeAllowanceService {
  def atsService: PayeAtsService

  def getAllowances(taxYear: Int)(implicit request: AuthenticatedRequest[_], hc: HeaderCarrier): Future[GenericViewModel] = {
    atsService.createModel(taxYear, allowanceDataConverter)
  }

  private[services] def allowanceDataConverter(atsData: AtsData): PayeAllowances = {
    def payload(key: String): Amount =
      atsData.allowance_data.flatMap(_.payload.flatMap(_.get(key))).getOrElse(Amount.empty)

    def taxpayerName(key: String): String =
      atsData.taxPayerData.flatMap(_.taxpayer_name.flatMap(_.get(key))).getOrElse("")

    PayeAllowances(
      atsData.taxYear,
      payload("personal_tax_free_amount"),
      payload("marriage_allowance_transferred_amount"),
      payload("other_allowances_amount"),
      payload("you_pay_tax_on"),
      payload("total_tax_free_amount"),
      payload("total_income_before_tax")
    )
  }
}
