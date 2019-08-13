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

import models.{AtsData, DataHolder}
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import utils.GenericViewModel
import view_models.{NoYearViewModel, TotalIncomeTax}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object TotalIncomeTaxService extends TotalIncomeTaxService {
  override val atsService = AtsService
  override val atsYearListService = AtsYearListService
}

trait TotalIncomeTaxService {
  def atsService: AtsService
  def atsYearListService: AtsYearListService

  def getIncomeData(implicit user: User, hc: HeaderCarrier, request: Request[AnyRef]): Future[GenericViewModel] = {
    atsYearListService.getSelectedAtsTaxYear flatMap {
      case Success(taxYear) => atsService.createModel(taxYear, totalIncomeConverter)
      case Failure(exception:Exception) => {
        val noYearViewModel = new NoYearViewModel
        Future(noYearViewModel)
      }
    }
  }

  private def totalIncomeConverter: (AtsData => GenericViewModel) =
    (output: AtsData) => {
      val wrapper: DataHolder = output.income_tax.get
      TotalIncomeTax(output.taxYear,
        output.utr.get,
        wrapper.payload.get.get("starting_rate_for_savings").get,
        wrapper.payload.get.get("starting_rate_for_savings_amount").get,
        wrapper.payload.get.get("basic_rate_income_tax").get,
        wrapper.payload.get.get("basic_rate_income_tax_amount").get,
        wrapper.payload.get.get("higher_rate_income_tax").get,
        wrapper.payload.get.get("higher_rate_income_tax_amount").get,
        wrapper.payload.get.get("additional_rate_income_tax").get,
        wrapper.payload.get.get("additional_rate_income_tax_amount").get,
        wrapper.payload.get.get("ordinary_rate").get,
        wrapper.payload.get.get("ordinary_rate_amount").get,
        wrapper.payload.get.get("upper_rate").get,
        wrapper.payload.get.get("upper_rate_amount").get,
        wrapper.payload.get.get("additional_rate").get,
        wrapper.payload.get.get("additional_rate_amount").get,
        wrapper.payload.get.get("other_adjustments_increasing").get,
        wrapper.payload.get.get("marriage_allowance_received_amount").get,
        wrapper.payload.get.get("other_adjustments_reducing").get,
        wrapper.payload.get.get("total_income_tax").get,
        wrapper.payload.get.get("scottish_income_tax").get,
        wrapper.incomeTaxStatus.get,
        wrapper.rates.get("starting_rate_for_savings_rate"),
        wrapper.rates.get("basic_rate_income_tax_rate"),
        wrapper.rates.get("higher_rate_income_tax_rate"),
        wrapper.rates.get("additional_rate_income_tax_rate"),
        wrapper.rates.get("ordinary_rate_tax_rate"),
        wrapper.rates.get("upper_rate_rate"),
        wrapper.rates.get("additional_rate_rate"),
        output.taxPayerData.get.taxpayer_name.get("title"),
        output.taxPayerData.get.taxpayer_name.get("forename"),
        output.taxPayerData.get.taxpayer_name.get("surname")
      )
    }
}
