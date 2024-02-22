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

package controllers.auth.actions

import com.google.inject.{ImplementedBy, Inject}
import connectors.DataCacheConnector
import controllers.auth.requests.AuthenticatedRequest
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, ControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.Globals

import scala.concurrent.{ExecutionContext, Future}

class AgentTokenAuthActionImpl @Inject() (
  cc: ControllerComponents,
  dataCacheConnector: DataCacheConnector
) extends AgentTokenAuthAction
    with I18nSupport
    with Logging {

  override def messagesApi: MessagesApi = cc.messagesApi

  override protected def refine[A](
    request: AuthenticatedRequest[A]
  ): Future[Either[Result, AuthenticatedRequest[A]]] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    dataCacheConnector.getAgentToken.map { agentToken =>
      val isActiveAgentButTokenMissing = request.isAgentActive && (request
        .getQueryString(Globals.TAXS_USER_TYPE_QUERY_PARAMETER)
        .isEmpty || request
        .getQueryString(Globals.TAXS_AGENT_TOKEN_ID)
        .isEmpty) &&
        agentToken.isDefined
      if (isActiveAgentButTokenMissing) {
        Left(Redirect(controllers.routes.ErrorController.notAuthorised))
      } else {
        Right(request)
      }
    }
  }

  override protected implicit val executionContext: ExecutionContext = cc.executionContext
}

@ImplementedBy(classOf[AgentTokenAuthActionImpl])
trait AgentTokenAuthAction extends ActionRefiner[AuthenticatedRequest, AuthenticatedRequest]
