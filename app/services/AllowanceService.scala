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
    val emptyAmount = Amount(0.0, "GBP")
    val emptyUserData = UserData(Some(Map("title" -> "", "forename" -> "", "surname" ->" ")))
    val allowanceData: DataHolder = atsData.allowance_data.get
    val taxPayerData: UserData  = atsData.taxPayerData.getOrElse(emptyUserData)

    Allowances(
      atsData.taxYear,
      atsData.utr.getOrElse(atsData.nino.getOrElse("")),
      allowanceData.payload.get.getOrElse("personal_tax_free_amount", emptyAmount),
      allowanceData.payload.get.getOrElse("marriage_allowance_transferred_amount", emptyAmount),
      allowanceData.payload.get.getOrElse("other_allowances_amount", emptyAmount),
      allowanceData.payload.get.getOrElse("you_pay_tax_on", emptyAmount),
      allowanceData.payload.get.getOrElse("total_tax_free_amount", emptyAmount),
      taxPayerData.taxpayer_name.get("title"),
      taxPayerData.taxpayer_name.get("forename"),
      taxPayerData.taxpayer_name.get("surname")
    )
  }
}
