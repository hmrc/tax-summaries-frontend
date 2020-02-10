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

import java.util.Date

import connectors.{DataCacheConnector, MiddleConnector}
import controllers.auth.AuthenticatedRequest
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import services.{AgentToken, AuditService, AuditTypes}
import uk.gov.hmrc.domain.{Nino, SaUtr, TaxIdentifier, Uar}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AccountUtils, AtsError, AuthorityUtils, GenericViewModel}
import view_models.NoATSViewModel

import scala.concurrent.Future

object PayeAtsService extends PayeAtsService {
  override val middleConnector = MiddleConnector
  override val dataCache = DataCacheConnector
  override val auditService = AuditService
  override val authUtils = AuthorityUtils
  override val accountUtils = AccountUtils
}

trait PayeAtsService {
  def middleConnector: MiddleConnector
  def dataCache: DataCacheConnector
  def auditService: AuditService
  val authUtils: AuthorityUtils
  val accountUtils: AccountUtils

  def createModel(taxYear: Int, converter: AtsData => GenericViewModel)(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[GenericViewModel] = {
    getAts(taxYear) map {
      checkCreateModel(_, converter)
    }
  }

  def checkCreateModel(output: AtsData, converter: AtsData => GenericViewModel): GenericViewModel = {
    output match {
      case errors if errors.errors.nonEmpty => errors.errors.get match {
        case IncomingAtsError("NoAtsError") => new NoATSViewModel
        case IncomingAtsError(_) => throw new AtsError(errors.errors.get.toString)
      }
      case wrapper => converter(wrapper)
    }
  }

  def getAts(taxYear: Int)(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[AtsData] = {
    dataCache.fetchAndGetAtsForSession(taxYear) flatMap {
      case Some(data) => Future.successful(data)
      case None => getAtsAndStore(taxYear)

    }
  }

  private def getAtsAndStore(taxYear: Int, agentToken: Option[AgentToken] = None)(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[AtsData] = {
    val nino :Nino = request.nino.get
    val gotData = middleConnector.connectToPayeAts(nino, taxYear)

    gotData flatMap { data =>
      data.errors match {
        case None =>
          sendAuditEvent(nino, data)
          storeAtsData(data)
        case Some(IncomingAtsError("NoAtsError")) =>
          storeAtsData(data)
        case Some(_) =>
          Future.successful(data)
      }
    }
  }

  private def storeAtsData(dataWithUser: AtsData)(implicit hc: HeaderCarrier) = {
    dataCache.storeAtsForSession(dataWithUser) map {
      data => data.get
    }
  }

  private def sendAuditEvent(account: Nino, data: AtsData)(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]) = {
        val userType = if (AccountUtils.isPortalUser(request)) "non-transitioned" else "transitioned"
        auditService.sendEvent(AuditTypes.Tx_SUCCEEDED, Map(
          "userId" -> request.userId,
          "nino" -> data.nino.get,
          "userType" -> userType,
          "taxYear" -> data.taxYear.toString,
          "time" -> new Date().toString
        ))
  }
}
