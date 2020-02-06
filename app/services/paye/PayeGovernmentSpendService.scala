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
import models.{AtsData, GovernmentSpendingOutputWrapper}
import uk.gov.hmrc.http.HeaderCarrier
import utils.GenericViewModel
import view_models.paye.PayeGovernmentSpend
import view_models.{Amount, GovernmentSpend}

import scala.concurrent.Future

object PayeGovernmentSpendService extends PayeGovernmentSpendService {
  override val atsService = PayeAtsService
}

trait PayeGovernmentSpendService {
  def atsService: PayeAtsService

  def getGovernmentSpendData(taxYear: Int)(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[GenericViewModel] =
    atsService.createModel(taxYear, govSpend)

  private[services] def govSpend(atsData: AtsData): PayeGovernmentSpend = {
    def payload(key: String): Amount =
      atsData.income_tax.flatMap(_.payload.flatMap(_.get(key))).getOrElse(Amount.empty)

    def taxpayerName(key: String): String =
      atsData.taxPayerData.flatMap(_.taxpayer_name.flatMap(_.get(key))).getOrElse("")

    val govSpendingData: GovernmentSpendingOutputWrapper = atsData.gov_spending.get

    PayeGovernmentSpend(atsData.taxYear,
      govSpendingData.govSpendAmountData.get.toList,
      taxpayerName("title"),
      taxpayerName("forename"),
      taxpayerName("surname"),
      govSpendingData.totalAmount,
      atsData.income_tax.get.incomeTaxStatus.getOrElse(""),
      payload("scottish_income_tax")
    )
  }
}
