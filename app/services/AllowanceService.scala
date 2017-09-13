/*
 * Copyright 2017 HM Revenue & Customs
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

import models.{AtsData, DataHolder}
import play.api.mvc.Request
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.GenericViewModel
import view_models.Allowances
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

object AllowanceService extends AllowanceService {
  override val atsService = AtsService
  override val atsYearListService = AtsYearListService
}

trait AllowanceService {
  def atsService: AtsService
  def atsYearListService: AtsYearListService

  def getAllowances(implicit user: User, request: Request[AnyRef], hc: HeaderCarrier): Future[GenericViewModel] = {
    atsYearListService.getSelectedAtsTaxYear flatMap {
      case taxYear => atsService.createModel(taxYear, allowanceService)
    }
  }

  private def allowanceService: (AtsData => GenericViewModel) =
    (output: AtsData) => {
      val wrapper: DataHolder = output.allowance_data.get
      Allowances(output.taxYear,
        output.utr.get,
        wrapper.payload.get.get("personal_tax_free_amount").get,
        wrapper.payload.get.get("other_allowances_amount").get,
        wrapper.payload.get.get("total_tax_free_amount").get,
        output.taxPayerData.get.taxpayer_name.get("title"),
        output.taxPayerData.get.taxpayer_name.get("forename"),
        output.taxPayerData.get.taxpayer_name.get("surname")
      )
    }
}
