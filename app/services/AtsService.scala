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

import java.util.Date

import com.google.inject.Inject
import connectors.{DataCacheConnector, MiddleConnector}
import controllers.auth.AuthenticatedRequest
import models._
import play.api.Play
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.domain.{SaUtr, TaxIdentifier, Uar}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AccountUtils, AtsError, AuthorityUtils, GenericViewModel}
import view_models.NoATSViewModel

import scala.concurrent.Future

class AtsService @Inject()(
                            middleConnector: MiddleConnector,
                            dataCacheConnector: DataCacheConnector,
                            val auditService: AuditService) {
  val authUtils: AuthorityUtils = AuthorityUtils
  val accountUtils: AccountUtils = AccountUtils

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
    dataCacheConnector.fetchAndGetAtsForSession(taxYear) flatMap {
      case Some(data) =>
        if (accountUtils.isAgent(request)) {
          fetchAgentInfo(data, taxYear)
        } else {
          getAtsAndStore(taxYear)
        }
      case None =>
        if (accountUtils.isAgent(request)) {
          dataCacheConnector.getAgentToken.flatMap {
            token => getAtsAndStore(taxYear, token)
          }
        } else {
          getAtsAndStore(taxYear)
        }
    }
  }


  private def fetchAgentInfo (data :AtsData, taxYear: Int)(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]) : Future[AtsData] = {
    dataCacheConnector.getAgentToken.flatMap {
      token =>
        if (authUtils.checkUtr(data.utr, token)) {
          Future.successful(data)
        } else {
          getAtsAndStore(taxYear, token)
        }
    }
  }
  

  private def getAtsAndStore(taxYear: Int, agentToken: Option[AgentToken] = None)(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[AtsData] = {
    val account = utils.AccountUtils.getAccount(request)
    val requestedUTR = authUtils.getRequestedUtr(account, agentToken)

    //This warning is unchecked because we know that AuthorisedFor will only give us those accounts
    val gotData = (account: @unchecked) match {
      case agentUar: Uar => middleConnector.connectToAtsOnBehalfOf(agentUar, requestedUTR, taxYear)
      case individualUtr: SaUtr => middleConnector.connectToAts(individualUtr, taxYear)
    }

    gotData flatMap { data =>
      data.errors match {
        case None =>
          sendAuditEvent(account, data)
          storeAtsData(data)
        case Some(IncomingAtsError("NoAtsError")) =>
          storeAtsData(data)
        case Some(_) =>
          Future.successful(data)
      }
    }
  }

  private def storeAtsData(dataWithUser: AtsData)(implicit hc: HeaderCarrier) = {
    dataCacheConnector.storeAtsForSession(dataWithUser) map {
      data => data.get
    }
  }

  private def sendAuditEvent(account: TaxIdentifier, data: AtsData)(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]) = {
    (account: @unchecked) match {
      case _: Uar =>
        auditService.sendEvent(AuditTypes.Tx_SUCCEEDED, Map(
          "agentId" -> AccountUtils.getAccountId(request),
          "clientUtr" -> data.utr.get,
          "taxYear" -> data.taxYear.toString,
          "time" -> new Date().toString
        ))
      case _: SaUtr =>
        val userType = if (AccountUtils.isPortalUser(request)) "non-transitioned" else "transitioned"
        auditService.sendEvent(AuditTypes.Tx_SUCCEEDED, Map(
          "userId" -> request.userId,
          "userUtr" -> data.utr.get,
          "userType" -> userType,
          "taxYear" -> data.taxYear.toString,
          "time" -> new Date().toString
        ))
    }
  }
}
