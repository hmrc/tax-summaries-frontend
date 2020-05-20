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

package services

import com.google.inject.Inject
import controllers.auth.AuthenticatedRequest
import models.AtsListData
import uk.gov.hmrc.http.HeaderCarrier
import utils.GenericViewModel
import view_models.{AtsList, TaxYearEnd}

import scala.concurrent.Future


class AtsYearListService @Inject()(atsListService: AtsListService) {


  def getAtsListData(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[GenericViewModel] = {
    atsListService.createModel(atsListDataConverter)
  }

  def storeSelectedAtsTaxYear(taxYear: Int)(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[Int] = {
    atsListService.storeSelectedTaxYear(taxYear)
  }


  private[services] def atsListDataConverter(atsListData: AtsListData): AtsList = {
      AtsList(
        atsListData.utr,
        atsListData.taxPayer.get.taxpayer_name.get("forename"),
        atsListData.taxPayer.get.taxpayer_name.get("surname"),
        atsListData.atsYearList.get.map(year => TaxYearEnd(Some(year.toString)))
      )
    }
}
