/*
 * Copyright 2024 HM Revenue & Customs
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
import controllers.auth.requests.AuthenticatedRequest
import models.AtsResponse
import models.admin.{PAYEServiceToggle, SelfAssessmentServiceToggle}
import play.api.Logging
import repository.TaxsAgentTokenSessionCacheRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.DataKey
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import utils._
import view_models.{AtsList, AtsMergePageViewModel}

import scala.concurrent.{ExecutionContext, Future}

class AtsMergePageService @Inject() (
  taxsAgentTokenSessionCacheRepository: TaxsAgentTokenSessionCacheRepository,
  payeAtsService: PayeAtsService,
  atsListService: AtsListService,
  appConfig: ApplicationConfig,
  cryptoService: CryptoService,
  featureFlagService: FeatureFlagService
)(implicit ec: ExecutionContext)
    extends Logging {

  def getSaAndPayeYearList(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]
  ): Future[Either[AtsResponse, AtsMergePageViewModel]] = {
    val requiredYearsList: List[Int] = {
      val yearFrom = appConfig.taxYear - appConfig.maxTaxYearsTobeDisplayed + 1
      (yearFrom to appConfig.taxYear).toList
    }

    getPayeYearListIfEnabled(request.isAgent).flatMap {
      // If paye response has all required years then we don't need to call sa API at all
      case Right(payeData) if payeData == requiredYearsList =>
        Future.successful[Either[AtsResponse, AtsMergePageViewModel]](
          Right(AtsMergePageViewModel(AtsList.empty, payeData, appConfig, request.confidenceLevel))
        )
      case payeResponse                                     =>
        getSaYearListIfEnabled.map { saResponse =>
          (saResponse, payeResponse) match {
            case (Left(atsResponse), _)                                     => Left(atsResponse)
            case (Right(saData), _) if saData.yearList == requiredYearsList =>
              Right(AtsMergePageViewModel(saData, Nil, appConfig, request.confidenceLevel))
            case (Right(_), Left(atsResponse))                              => Left(atsResponse)
            case (Right(saData), Right(payeData))                           =>
              Right(AtsMergePageViewModel(saData, payeData, appConfig, request.confidenceLevel))
          }
        }
    }
  }

  private def getSaYearListIfEnabled(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]
  ): Future[Either[AtsResponse, AtsList]] =
    featureFlagService.get(SelfAssessmentServiceToggle).flatMap { toggle =>
      if (toggle.isEnabled) getSaYearList
      else Future.successful(Right(AtsList.empty))
    }

  private def getPayeYearListIfEnabled(isAgent: Boolean)(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]
  ): Future[Either[AtsResponse, List[Int]]] =
    featureFlagService.get(PAYEServiceToggle).flatMap { toggle =>
      if (toggle.isEnabled && !isAgent)
        getPayeAtsYearList
      else
        Future.successful(Right(List.empty[Int]))
    }

  private def getSaYearList(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]
  ): Future[Either[AtsResponse, AtsList]] =
    if (request.getQueryString(Globals.TAXS_USER_TYPE_QUERY_PARAMETER).contains(Globals.TAXS_PORTAL_REFERENCE)) {
      val agentToken = request.getQueryString(Globals.TAXS_AGENT_TOKEN_ID)

      agentToken
        .fold(Future.successful(None)) { token =>
          if (AccountUtils.isAgent(request)) {
            val finalAgentToken = cryptoService.getAgentToken(token)
            taxsAgentTokenSessionCacheRepository
              .putSession(DataKey(Globals.TAXS_AGENT_TOKEN_KEY), finalAgentToken)
              .map(_ => None)
          } else {
            Future.successful(None)
          }
        }
        .map(_ => atsListService.createModel())
        .flatten
    } else {
      atsListService.createModel()
    }

  private def getPayeAtsYearList(implicit
    hc: HeaderCarrier,
    request: AuthenticatedRequest[_]
  ): Future[Either[AtsResponse, List[Int]]] =
    request.nino
      .map(
        payeAtsService
          .getPayeTaxYearData(_, appConfig.taxYear - appConfig.maxTaxYearsTobeDisplayed + 1, appConfig.taxYear)
      )
      .getOrElse(Future.successful(Right(List.empty)))
}
