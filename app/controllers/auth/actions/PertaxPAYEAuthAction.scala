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
import controllers.auth.requests.{AuthenticatedRequest, PayeAuthenticatedRequest}
import controllers.auth.services.PertaxAuthService
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{ActionRefiner, ControllerComponents, Result}

import scala.concurrent.{ExecutionContext, Future}

class PertaxAuthActionImpl @Inject() (
  cc: ControllerComponents,
  pertaxAuthService: PertaxAuthService
) extends PertaxPAYEAuthAction
    with I18nSupport
    with Logging {

  override def messagesApi: MessagesApi = cc.messagesApi

  override protected def refine[A](
    request: PayeAuthenticatedRequest[A]
  ): Future[Either[Result, PayeAuthenticatedRequest[A]]] =
    pertaxAuthService.authorise[A, PayeAuthenticatedRequest[A]](request)

  override protected implicit val executionContext: ExecutionContext = cc.executionContext
}

class PertaxAuthActionSAImpl @Inject() (
  cc: ControllerComponents,
  pertaxAuthService: PertaxAuthService
) extends PertaxSAAuthAction
    with I18nSupport
    with Logging {

  override def messagesApi: MessagesApi = cc.messagesApi

  override protected def refine[A](
    request: AuthenticatedRequest[A]
  ): Future[Either[Result, AuthenticatedRequest[A]]] =
    pertaxAuthService.authorise[A, AuthenticatedRequest[A]](request)

  override protected implicit val executionContext: ExecutionContext = cc.executionContext
}

@ImplementedBy(classOf[PertaxAuthActionImpl])
trait PertaxPAYEAuthAction extends ActionRefiner[PayeAuthenticatedRequest, PayeAuthenticatedRequest]
@ImplementedBy(classOf[PertaxAuthActionSAImpl])
trait PertaxSAAuthAction extends ActionRefiner[AuthenticatedRequest, AuthenticatedRequest]
