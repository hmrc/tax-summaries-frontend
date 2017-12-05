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
import utils.GenericViewModel
import view_models.IncomeBeforeTax

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

object IncomeService extends IncomeService {
  override val atsService = AtsService
  override val atsYearListService = AtsYearListService
}

trait IncomeService {
  def atsService: AtsService
  def atsYearListService: AtsYearListService

  def getIncomeData(implicit user: User, hc: HeaderCarrier, request: Request[AnyRef]): Future[GenericViewModel] = {
    atsYearListService.getSelectedAtsTaxYear flatMap {
      case taxYear => atsService.createModel(taxYear, createIncomeConverter)
    }
  }

  private def createIncomeConverter:
  (AtsData => GenericViewModel) =
    (output: AtsData) => {
      val wrapper: DataHolder = output.income_data.get
      IncomeBeforeTax(output.taxYear,
        output.utr.get,
        wrapper.payload.get.get("self_employment_income").get,
        wrapper.payload.get.get("income_from_employment").get,
        wrapper.payload.get.get("state_pension").get,
        wrapper.payload.get.get("other_pension_income").get,
        wrapper.payload.get.get("taxable_state_benefits").get,
        wrapper.payload.get.get("other_income").get,
        wrapper.payload.get.get("benefits_from_employment").get,
        wrapper.payload.get.get("total_income_before_tax").get,
        output.taxPayerData.get.taxpayer_name.get("title"),
        output.taxPayerData.get.taxpayer_name.get("forename"),
        output.taxPayerData.get.taxpayer_name.get("surname")
      )
    }
}
