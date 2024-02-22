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
import config.ApplicationConfig
import controllers.auth.requests.AuthenticatedRequest
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, ControllerComponents, Result}

import scala.concurrent.{ExecutionContext, Future}

class SaCheckActionImpl @Inject() (
  cc: ControllerComponents,
  appConfig: ApplicationConfig
) extends SaCheckAuthAction
    with I18nSupport
    with Logging {

  override def messagesApi: MessagesApi = cc.messagesApi

  private val saShuttered: Boolean = appConfig.saShuttered

  private def notAuthorisedPage: Result = Redirect(controllers.routes.ErrorController.notAuthorised)

  override protected def refine[A](
    request: AuthenticatedRequest[A]
  ): Future[Either[Result, AuthenticatedRequest[A]]] =
    Future.successful(
      if (saShuttered) {
        Left(Redirect(controllers.routes.ErrorController.serviceUnavailable))
      } else {
        (request.agentRef, request.isAgentActive, request.saUtr) match {
          case (Some(_), false, _) => Left(notAuthorisedPage) // Inactive agent
          case _                   => Right(request)
        }
      }
    )

  override protected implicit val executionContext: ExecutionContext = cc.executionContext
}

@ImplementedBy(classOf[SaCheckActionImpl])
trait SaCheckAuthAction extends ActionRefiner[AuthenticatedRequest, AuthenticatedRequest]
