/*
 * Copyright 2021 HM Revenue & Customs
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
import config.ApplicationConfig
import connectors.MiddleConnector
import controllers.auth.AuthenticatedRequest
import models.{AtsData, GovernmentSpendingOutputWrapper}
import uk.gov.hmrc.domain.TaxIdentifier
import uk.gov.hmrc.http.HeaderCarrier
import utils.{CategoriesUtils, GenericViewModel}
import view_models.GovernmentSpend

import scala.concurrent.{ExecutionContext, Future}

class GovernmentSpendService @Inject()(
  atsService: AtsService,
  atsYearListService: AtsYearListService,
  middleConnector: MiddleConnector)(implicit val appConfig: ApplicationConfig) {

  def getGovernmentSpendData(
    taxYear: Int)(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[GenericViewModel] =
    atsService.createModel(taxYear, govSpend)

  def getGovernmentSpendFigures(taxYear: Int, taxIdentifier: Option[TaxIdentifier])(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[Seq[(String, Double)]] =
    taxIdentifier match {
      case Some(value) =>
        middleConnector.connectToGovernmentSpend(taxYear, value).map { response =>
          val sortedGovSpendingData = response.json.as[Map[String, Double]].toList.sortWith(_._2 > _._2)
          CategoriesUtils.reorderCategories(appConfig, taxYear, sortedGovSpendingData)
        }
      case _ => Future.failed(new IllegalArgumentException("No tax identifier was found, cannot complete request"))
    }

  private[services] def govSpend(atsData: AtsData): GovernmentSpend = {
    val govSpendingData: GovernmentSpendingOutputWrapper = atsData.gov_spending.get

    GovernmentSpend(
      atsData.taxYear,
      atsData.utr.get,
      govSpendingData.govSpendAmountData.get.toList,
      atsData.taxPayerData.get.taxpayer_name.get("title"),
      atsData.taxPayerData.get.taxpayer_name.get("forename"),
      atsData.taxPayerData.get.taxpayer_name.get("surname"),
      govSpendingData.totalAmount,
      atsData.income_tax.get.incomeTaxStatus.getOrElse(""),
      atsData.income_tax.get.payload.get("scottish_income_tax")
    )
  }
}
