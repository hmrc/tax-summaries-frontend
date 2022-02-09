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

package connectors

import com.google.inject.Inject
import config.TAXSSessionCache
import models.{AgentToken, AtsData, AtsListData}
import services.CryptoService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.Globals

import scala.concurrent.{ExecutionContext, Future}

class DataCacheConnector @Inject()(cryptoService: CryptoService, sessionCache: TAXSSessionCache)(
  implicit ec: ExecutionContext) {

  val sourceId: String = Globals.TAXS_CACHE_KEY
  val sourceAtsListId: String = Globals.TAXS_ATS_LIST_CACHE_KEY
  val sourceAtsSelectedTaxYearId: String = Globals.TAXS_SELECTED_TAX_YEAR_CACHE_KEY

  def fetchAndGetAtsForSession(taxYear: Int)(implicit hc: HeaderCarrier): Future[Option[AtsData]] = {
    val atsSourceId = sourceId + taxYear
    sessionCache.fetchAndGetEntry[AtsData](atsSourceId)
  }

  def storeAtsForSession(data: AtsData)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[AtsData]] = {
    val atsSourceId = sourceId + data.taxYear
    val result = sessionCache.cache[AtsData](atsSourceId, data)
    result flatMap {
      case data: CacheMap => Future.successful(data.getEntry[AtsData](atsSourceId))
    }
  }

  def fetchAndGetAtsListForSession(implicit hc: HeaderCarrier): Future[Option[AtsListData]] =
    sessionCache.fetchAndGetEntry[AtsListData](sourceAtsListId)

  def storeAtsListForSession(
    data: AtsListData)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[AtsListData]] = {
    val result = sessionCache.cache[AtsListData](sourceAtsListId, data)
    result flatMap {
      case data: CacheMap => Future.successful(data.getEntry[AtsListData](sourceAtsListId))
    }
  }

  def storeAtsTaxYearForSession(taxYear: Int)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Int]] = {
    val result = sessionCache.cache[Int](sourceAtsSelectedTaxYearId, taxYear)
    result flatMap {
      case data => Future.successful(data.getEntry[Int](sourceAtsSelectedTaxYearId))
    }
  }

  def fetchAndGetAtsTaxYearForSession(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Int]] =
    sessionCache.fetchAndGetEntry[Int](sourceAtsSelectedTaxYearId)

  def storeAgentToken(token: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AnyRef] = {
    val agentToken = cryptoService.getAgentToken(token)
    sessionCache.cache[AgentToken](Globals.TAXS_AGENT_TOKEN_KEY, agentToken)
  }

  def getAgentToken(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[AgentToken]] =
    sessionCache.fetchAndGetEntry[AgentToken](Globals.TAXS_AGENT_TOKEN_KEY)
}
