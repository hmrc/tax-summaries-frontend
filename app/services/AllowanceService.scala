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
import controllers.auth.AuthenticatedRequest
import models.{AtsData, DataHolder}
import uk.gov.hmrc.http.HeaderCarrier
import utils.GenericViewModel
import view_models.Allowances

import scala.concurrent.Future

class AllowanceService @Inject() (atsService: AtsService) {

  def getAllowances(
    taxYear: Int
  )(implicit request: AuthenticatedRequest[_], hc: HeaderCarrier): Future[GenericViewModel] =
    atsService.createModel(taxYear, allowanceDataConverter)

  private[services] def allowanceDataConverter(atsData: AtsData): Allowances = {
    val allowanceData: DataHolder = atsData.allowance_data.get

    Allowances(
      atsData.taxYear,
      atsData.utr.get,
      allowanceData.payload.get("personal_tax_free_amount"),
      allowanceData.payload.get("marriage_allowance_transferred_amount"),
      allowanceData.payload.get("other_allowances_amount"),
      allowanceData.payload.get("total_tax_free_amount"),
      atsData.taxPayerData.get.taxpayer_name.get("title"),
      atsData.taxPayerData.get.taxpayer_name.get("forename"),
      atsData.taxPayerData.get.taxpayer_name.get("surname")
    )
  }
}
