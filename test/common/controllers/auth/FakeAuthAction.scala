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

package common.controllers.auth

import common.controllers.auth.actions.Auth
import common.models.requests
import common.models.requests.AuthenticatedRequest
import common.utils.ControllerBaseSpec
import common.utils.TestConstants.*
import play.api.mvc.*
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.SaUtr

import scala.concurrent.{ExecutionContext, Future}

object FakeAuthAction extends ControllerBaseSpec with Auth {

  override val parser: BodyParser[AnyContent]               = mcc.parsers.anyContent
  override protected val executionContext: ExecutionContext = mcc.executionContext

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] =
    block(
      requests.AuthenticatedRequest(
        userId = "userId",
        agentRef = None,
        saUtr = Some(SaUtr(testUtr)),
        nino = None,
        isAgentActive = false,
        confidenceLevel = ConfidenceLevel.L50,
        credentials = fakeCredentials,
        request = request
      )
    )
}
