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

package controllers.auth

import play.api.mvc._
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.SaUtr
import utils.ControllerBaseSpec
import utils.TestConstants._

import scala.concurrent.{ExecutionContext, Future}

object FakeSelfAssessmentAuthAction extends ControllerBaseSpec with SelfAssessmentAction {

  override protected def executionContext: ExecutionContext = mcc.executionContext

  override protected def refine[A](request: AuthenticatedRequest[A]): Future[Either[Result, AuthenticatedRequest[A]]] =
    Future(
      Right(
        AuthenticatedRequest(
          "userId",
          None,
          Some(SaUtr(testUtr)),
          None,
          true,
          false,
          ConfidenceLevel.L50,
          fakeCredentials,
          request,
          None
        )
      )
    )

}
