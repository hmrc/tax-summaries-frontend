/*
 * Copyright 2023 HM Revenue & Customs
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

package common.services

import cats.data.EitherT
import cats.implicits.*
import com.google.inject.Inject
import common.config.ApplicationConfig
import common.connectors.GovSpendConnector
import io.jsonwebtoken.io.DeserializationException
import common.models.requests.AuthenticatedRequest
import common.models.{AtsData, AtsErrorResponse, GovernmentSpendingOutputWrapper, SpendData}
import play.api.libs.json.{JsObject, JsValue}
import uk.gov.hmrc.http.HeaderCarrier
import common.utils.{CategoriesUtils, GenericViewModel}
import common.view_models.GovernmentSpend
import sa.services.AtsService

import scala.concurrent.{ExecutionContext, Future}

class GovernmentSpendService @Inject() (atsService: AtsService, middleConnector: GovSpendConnector)(implicit
  val appConfig: ApplicationConfig
) {

  def getGovernmentSpendData(
    taxYear: Int
  )(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_], ec: ExecutionContext): Future[GenericViewModel] =
    atsService.createFutureModel(taxYear, govSpend)

  def getGovernmentSpendFigures(
    taxYear: Int
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): EitherT[Future, AtsErrorResponse, Seq[(String, Double)]] = {

    val governmentSpend = EitherT(middleConnector.connectToGovernmentSpend(taxYear)).leftMap(upStreamErrorResponse =>
      AtsErrorResponse(upStreamErrorResponse.message)
    )

    def read(value: JsValue): List[(String, Double)] = value match {
      case x: JsObject =>
        val values = x.fields.map { field =>
          (field._1, field._2.toString().toDouble)
        }
        values.toList
      case _           => throw new DeserializationException("Expected Map as JsObject, but got different")
    }

    for {
      response <- governmentSpend
    } yield read(response.json)
  }

  private[services] def govSpend(
    atsData: AtsData
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[GovernmentSpend] = {
    val govSpendingData: GovernmentSpendingOutputWrapper = atsData.gov_spending.get

    getGovernmentSpendFigures(atsData.taxYear)
      .map { governmentSpendFiguredMapList =>
        val governmentSpendCategoryOrder = governmentSpendFiguredMapList.map(_._1)
        GovernmentSpend(
          atsData.taxYear,
          atsData.utr.get,
          CategoriesUtils
            .reorderCategories[SpendData](
              governmentSpendCategoryOrder.toList,
              govSpendingData.govSpendAmountData.get.toList
            ),
          atsData.taxPayerData.getOrElse("title", ""),
          atsData.taxPayerData.getOrElse("forename", ""),
          atsData.taxPayerData.getOrElse("surname", ""),
          govSpendingData.totalAmount,
          atsData.income_tax.get.incomeTaxStatus.getOrElse(""),
          atsData.income_tax.get.payload.get("scottish_income_tax")
        )
      }
      .value
      .map(
        _.getOrElse(
          GovernmentSpend(
            atsData.taxYear,
            atsData.utr.get,
            govSpendingData.govSpendAmountData.get.toList,
            atsData.taxPayerData.getOrElse("title", ""),
            atsData.taxPayerData.getOrElse("forename", ""),
            atsData.taxPayerData.getOrElse("surname", ""),
            govSpendingData.totalAmount,
            atsData.income_tax.get.incomeTaxStatus.getOrElse(""),
            atsData.income_tax.get.payload.get("scottish_income_tax")
          )
        )
      )
  }

}
