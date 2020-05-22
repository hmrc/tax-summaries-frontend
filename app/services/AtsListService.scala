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
import models.{AgentToken, AtsListData, IncomingAtsError}
import uk.gov.hmrc.domain.{SaUtr, TaxIdentifier, Uar}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AccountUtils, AtsError, AuditTypes, AuthorityUtils, GenericViewModel}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class AtsListService @Inject()(auditService: AuditService,
                               middleConnector: MiddleConnector,
                               dataCache : DataCacheConnector) {

  def authUtils: AuthorityUtils = AuthorityUtils
  def accountUtils: AccountUtils = AccountUtils

  def createModel(converter: (AtsListData => GenericViewModel))(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[GenericViewModel] = {
    getAtsYearList map {
      checkCreateModel(_, converter)
    }
  }

  private def checkCreateModel(output: AtsListData, converter: (AtsListData => GenericViewModel)): GenericViewModel = {
    output match {
      case error if error.errors.nonEmpty => error.errors.get match {
        case IncomingAtsError(_) => throw new AtsError(error.errors.toString)
      }
      case atsList => converter(atsList)
    }
  }

  def getAtsYearList(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[AtsListData] = {
    for {
      data <- dataCache.fetchAndGetAtsListForSession
    } yield {
      data match {
        case Some(data) =>
          if (accountUtils.isAgent(request)) {
            fetchAgentInfo(data)
          } else {
            getAtsListAndStore()
          }
        case _ =>
          if (accountUtils.isAgent(request)) {
            dataCache.getAgentToken.flatMap {
              token => getAtsListAndStore(token)
            }
          } else {
            getAtsListAndStore()

          }
      }
    }
  } flatMap { identity }


  private def fetchAgentInfo (data: AtsListData)(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]) : Future[AtsListData] = {
    for {
      token <- dataCache.getAgentToken
    } yield {
      if (authUtils.checkUtr(data.utr, token)) {
        Future.successful(data)
      } else {
        getAtsListAndStore(token)
      }
    }
  } flatMap (identity)

  private def getAtsListAndStore(agentToken: Option[AgentToken] = None)(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[AtsListData] = {
    val account = utils.AccountUtils.getAccount(request)
    val requestedUTR = authUtils.getRequestedUtr(account, agentToken)

    val gotData = (account: @unchecked) match {
      case agent: Uar => middleConnector.connectToAtsListOnBehalfOf(agent, requestedUTR)
      case individual: SaUtr => middleConnector.connectToAtsList(individual)
    }

    // TODO: Audit Events
    for (data <- gotData) yield {
      data.errors match {
        case None =>
          sendAuditEvent(account, data)
          storeAtsListData(data)
        case Some(IncomingAtsError("NoAtsError")) => storeAtsListData(data)
        case Some(_)                              => Future(data)
      }
    }
  } flatMap { identity }

  private def storeAtsListData(atsList: AtsListData)(implicit hc: HeaderCarrier): Future[AtsListData] = {
    dataCache.storeAtsListForSession(atsList) map {
      data => data.get
    }
  }

  def storeSelectedTaxYear(taxYear: Int)(implicit hc: HeaderCarrier): Future[Int] = {
    dataCache.storeAtsTaxYearForSession(taxYear: Int) map {
      data => data.get
    }
  }

  def fetchSelectedTaxYear(implicit hc: HeaderCarrier): Future[Int] = {
    dataCache.fetchAndGetAtsTaxYearForSession map {
      data => data.get
    }
  }

  private def sendAuditEvent(account: TaxIdentifier, data: AtsListData)(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]) = {
    (account: @unchecked) match {
      case _: Uar =>
        auditService.sendEvent(AuditTypes.Tx_SUCCEEDED, Map(
          "agentId" -> AccountUtils.getAccountId(request),
          "clientUtr" -> data.utr,
          "time" -> new Date().toString
        ))
      case _: SaUtr =>
        val userType = if (AccountUtils.isPortalUser(request)) "non-transitioned" else "transitioned"
        auditService.sendEvent(AuditTypes.Tx_SUCCEEDED, Map(
          "userId" -> request.userId,
          "userUtr" -> data.utr,
          "userType" -> userType,
          "time" -> new Date().toString
        ))
    }
  }
}
