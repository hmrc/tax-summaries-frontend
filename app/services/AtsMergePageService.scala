/*
 * Copyright 2022 HM Revenue & Customs
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

import cats.data.EitherT
import com.google.inject.Inject
import config.ApplicationConfig
import connectors.DataCacheConnector
import controllers.auth.AuthenticatedRequest
import models.AtsResponse
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import utils._
import view_models.{AtsList, AtsMergePageViewModel}

import scala.concurrent.{ExecutionContext, Future}

class AtsMergePageService @Inject() (
  dataCacheConnector: DataCacheConnector,
  payeAtsService: PayeAtsService,
  atsListService: AtsListService,
  appConfig: ApplicationConfig
)(implicit ec: ExecutionContext)
    extends Logging {

  def getSaAndPayeYearList(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]
  ): Future[Either[AtsResponse, AtsMergePageViewModel]] =
    (for {
      saData   <- EitherT(if (!appConfig.saShuttered) { getSaYearList }
                  else { Future(Right(AtsList.empty)) })
      payeData <- EitherT(if (!appConfig.payeShuttered) { getPayeAtsYearList }
                  else { Future(Right(List.empty[Int])) })
    } yield AtsMergePageViewModel(saData, payeData, appConfig, request.confidenceLevel)).value

  private def getSaYearList(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]
  ): Future[Either[AtsResponse, AtsList]] = {
    if (request.getQueryString(Globals.TAXS_USER_TYPE_QUERY_PARAMETER).equals(Some(Globals.TAXS_PORTAL_REFERENCE))) {
      val agentToken = request.getQueryString(Globals.TAXS_AGENT_TOKEN_ID)

      agentToken.fold[Future[_]] {
        Future.successful(None)
      } { token =>
        if (AccountUtils.isAgent(request)) {
          dataCacheConnector.storeAgentToken(token) recover { case e: Throwable => throw e }
        } else {
          Future.successful(None)
        }
      }
    }
    atsListService.createModel
  }

  private def getPayeAtsYearList(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]
  ): Future[Either[AtsResponse, List[Int]]] =
    request.nino
      .map(
        payeAtsService.getPayeTaxYearData(_, appConfig.taxYear - appConfig.maxTaxYearsTobeDisplayed, appConfig.taxYear)
      )
      .getOrElse(Future(Right(List.empty)))
}
