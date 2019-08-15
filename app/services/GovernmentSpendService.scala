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

import models.{AtsData, GovernmentSpendingOutputWrapper}
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import utils.GenericViewModel
import view_models.GovernmentSpend

import scala.concurrent.Future

object GovernmentSpendService extends GovernmentSpendService {
  override val atsService = AtsService
  override val atsYearListService = AtsYearListService
}

trait GovernmentSpendService {
  def atsService: AtsService
  def atsYearListService: AtsYearListService

  def getGovernmentSpendData(taxYear: Int)(implicit user: User, hc: HeaderCarrier, request: Request[AnyRef]): Future[GenericViewModel] =
    atsService.createModel(taxYear, govSpend)

  private def govSpend: AtsData => GenericViewModel =
    (output: AtsData) => {
      val wrapper: GovernmentSpendingOutputWrapper = output.gov_spending.get
      new GovernmentSpend(output.taxYear,
        output.utr.get,
        wrapper.govSpendAmountData.get.toList,
        output.taxPayerData.get.taxpayer_name.get("title"),
        output.taxPayerData.get.taxpayer_name.get("forename"),
        output.taxPayerData.get.taxpayer_name.get("surname"),
        wrapper.totalAmount,
        output.income_tax.get.incomeTaxStatus.getOrElse(""),
        output.income_tax.get.payload.get("scottish_income_tax")
      )
    }
}
