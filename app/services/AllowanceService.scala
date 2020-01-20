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
import view_models.{Allowances, Amount}

import scala.concurrent.Future

object AllowanceService extends AllowanceService {
  override val atsService = AtsService
  override val atsYearListService = AtsYearListService
}

trait AllowanceService {
  def atsService: AtsService
  def atsYearListService: AtsYearListService

  def getAllowances(taxYear: Int)(implicit request: AuthenticatedRequest[_], hc: HeaderCarrier): Future[GenericViewModel] = {
    atsService.createModel(taxYear, allowanceDataConverter)
  }

  private[services] def allowanceDataConverter(atsData: AtsData): Allowances = {
    def payload(key: String): Amount =
      atsData.allowance_data.flatMap(_.payload.flatMap(_.get(key))).getOrElse(Amount.empty)

    def taxpayerName(key: String): String =
      atsData.taxPayerData.flatMap(_.taxpayer_name.flatMap(_.get(key))).getOrElse("")

    Allowances(
      atsData.taxYear,
      atsData.utr.getOrElse(""),
      payload("personal_tax_free_amount"),
      payload("marriage_allowance_transferred_amount"),
      payload("other_allowances_amount"),
      payload("you_pay_tax_on"),
      payload("total_tax_free_amount"),
      taxpayerName("title"),
      taxpayerName("forename"),
      taxpayerName("surname")
    )
  }
}
