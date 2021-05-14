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
import connectors.DataCacheConnector
import controllers.auth.AuthenticatedRequest
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils._
import view_models.{AtsList, AtsMergePageViewModel}

import scala.concurrent.{ExecutionContext, Future}

class AtsMergePageService @Inject()(
  dataCacheConnector: DataCacheConnector,
  payeAtsService: PayeAtsService,
  atsListService: AtsListService,
  atsYearListService: AtsYearListService,
  appConfig: ApplicationConfig)(implicit ec: ExecutionContext)
    extends AccountUtils {

  def getSaAndPayeYearList(
    implicit hc: HeaderCarrier,
    request: AuthenticatedRequest[_]): Future[Either[Int, AtsMergePageViewModel]] =
    for {
      saData   <- getSaYearList
      payeData <- getPayeAtsYearList
    } yield {
      (saData, payeData) match {
        case (Right(saTaxYearData), Right(payeTaxYearList)) => {
          Right(AtsMergePageViewModel(saTaxYearData, payeTaxYearList, appConfig))
        }
        case _ => Left(INTERNAL_SERVER_ERROR)
      }

    }

  private def getSaYearList(
    implicit hc: HeaderCarrier,
    request: AuthenticatedRequest[_]): Future[Either[Int, AtsList]] =
    if (appConfig.saShuttered) Future.successful(Right(AtsList.empty))
    else {
      if (request.getQueryString(Globals.TAXS_USER_TYPE_QUERY_PARAMETER).equals(Some(Globals.TAXS_PORTAL_REFERENCE))) {

        val session = request.session + (Globals.TAXS_USER_TYPE_KEY -> Globals.TAXS_PORTAL_REFERENCE)
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
      atsYearListService.getAtsListData
    }

  private def getPayeAtsYearList(
    implicit hc: HeaderCarrier,
    request: AuthenticatedRequest[_]): Future[Either[HttpResponse, List[Int]]] =
    if (appConfig.payeShuttered) Future.successful(Right(List.empty))
    else {
      val payeYear: Int = appConfig.taxYear
      request.nino
        .map(payeAtsService.getPayeTaxYearData(_, payeYear - 1, payeYear))
        .getOrElse(Future(Right(List.empty)))
    }
}
