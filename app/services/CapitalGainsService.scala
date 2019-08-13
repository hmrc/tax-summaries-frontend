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
import view_models.{CapitalGains, NoATSViewModel, NoYearViewModel}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object CapitalGainsService extends CapitalGainsService {
  override val atsService = AtsService
  override val atsYearListService = AtsYearListService
}

trait CapitalGainsService {
  def atsService: AtsService
  def atsYearListService: AtsYearListService

  def getCapitalGains(implicit user: User, hc: HeaderCarrier, request: Request[AnyRef]): Future[GenericViewModel] = {
    atsYearListService.getSelectedAtsTaxYear flatMap {
      case Success(taxYear) => atsService.createModel(taxYear, capitalGains)
      case Failure(exception:Exception) => {
        val noYearViewModel = new NoYearViewModel
        Future(noYearViewModel)
      }
    }
  }

  private def capitalGains: (AtsData => GenericViewModel) =
    (output: AtsData) => {
      val wrapper: DataHolder = output.capital_gains_data.get
      CapitalGains(output.taxYear,
        output.utr.get,
        wrapper.payload.get.get("taxable_gains").get,
        wrapper.payload.get.get("less_tax_free_amount").get,
        wrapper.payload.get.get("pay_cg_tax_on").get,
        wrapper.payload.get.get("amount_at_entrepreneurs_rate").get,
        wrapper.payload.get.get("amount_due_at_entrepreneurs_rate").get,
        wrapper.payload.get.get("amount_at_ordinary_rate").get,
        wrapper.payload.get.get("amount_due_at_ordinary_rate").get,
        wrapper.payload.get.get("amount_at_higher_rate").get,
        wrapper.payload.get.get("amount_due_at_higher_rate").get,
        wrapper.payload.get.get("adjustments").get,
        wrapper.payload.get.get("total_cg_tax").get,
        wrapper.payload.get.get("cg_tax_per_currency_unit").get,
        wrapper.rates.get("cg_entrepreneurs_rate"),
        wrapper.rates.get("cg_ordinary_rate"),
        wrapper.rates.get("cg_upper_rate"),
        wrapper.rates.get("total_cg_tax_rate"),
        output.taxPayerData.get.taxpayer_name.get("title"),
        output.taxPayerData.get.taxpayer_name.get("forename"),
        output.taxPayerData.get.taxpayer_name.get("surname")
      )
    }
}
