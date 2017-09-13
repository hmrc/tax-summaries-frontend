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

import models.AtsListData
import play.api.mvc.Request
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.GenericViewModel
import view_models.{TaxYearEnd, AtsList}

import scala.concurrent.Future

object AtsYearListService extends AtsYearListService {
  override val atsListService = AtsListService
}

trait AtsYearListService {
  def atsListService: AtsListService

  def getAtsListData(implicit user: User, hc: HeaderCarrier, request: Request[AnyRef]): Future[GenericViewModel] = {
    atsListService.createModel(atsList)
  }

  def storeSelectedAtsTaxYear(taxYear: Int)(implicit user: User, hc: HeaderCarrier, request: Request[AnyRef]): Future[Int] = {
    atsListService.storeSelectedTaxYear(taxYear)
  }

  def getSelectedAtsTaxYear(implicit user: User, hc: HeaderCarrier, request: Request[AnyRef]): Future[Int] = {
    Future.successful( request.getQueryString("taxYear").getOrElse("").toInt )
  }

  private def atsList: (AtsListData => GenericViewModel) =
    (output: AtsListData) => {
      new AtsList(output.utr,
        output.taxPayer.get.taxpayer_name.get("forename"),
        output.taxPayer.get.taxpayer_name.get("surname"),
        output.atsYearList.get.map(year => TaxYearEnd(Some(year.toString)))
      )
    }
}
