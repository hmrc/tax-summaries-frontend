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
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import utils.GenericViewModel
import view_models.{NoATSViewModel, NoTaxYearViewModel, Summary}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

import scala.util.{Failure, Success}

object SummaryService extends SummaryService {
  override val atsService = AtsService
  override val atsYearListService = AtsYearListService
}

trait SummaryService {

  def atsService: AtsService
  def atsYearListService: AtsYearListService

  def getSummaryData(implicit user: User, hc: HeaderCarrier, request: Request[AnyRef]): Future[GenericViewModel] = {
    atsYearListService.getSelectedAtsTaxYear flatMap {
      case Success(taxYear) => {
        atsService.createModel(taxYear, summaryConverter)
      }
      case Failure(exception) => {
        val noTaxYearViewModel = new NoTaxYearViewModel
        Future.successful(noTaxYearViewModel)
      }
    }
  }

  private def summaryConverter: (AtsData => GenericViewModel) =
    (output: AtsData) => {
      val wrapper: DataHolder = output.summary_data.get

      Summary(output.taxYear,
        output.utr.get,
        wrapper.payload.get.get("employee_nic_amount").get,
        wrapper.payload.get.get("total_income_tax_and_nics").get,
        wrapper.payload.get.get("your_total_tax").get,
        wrapper.payload.get.get("personal_tax_free_amount").get,
        wrapper.payload.get.get("total_tax_free_amount").get,
        wrapper.payload.get.get("total_income_before_tax").get,
        wrapper.payload.get.get("total_income_tax").get,
        wrapper.payload.get.get("total_cg_tax").get,
        wrapper.payload.get.get("taxable_gains").get,
        wrapper.payload.get.get("cg_tax_per_currency_unit").get,
        wrapper.payload.get.get("nics_and_tax_per_currency_unit").get,
        wrapper.rates.get.get("total_cg_tax_rate").get,
        wrapper.rates.get.get("nics_and_tax_rate").get,
        output.taxPayerData.get.taxpayer_name.get("title"),
        output.taxPayerData.get.taxpayer_name.get("forename"),
        output.taxPayerData.get.taxpayer_name.get("surname")
      )
    }
}
