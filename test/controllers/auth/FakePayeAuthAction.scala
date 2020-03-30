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

package controllers.auth

import play.api.mvc.{Request, Result}
import utils.TestConstants._

import scala.concurrent.Future

object FakePayeAuthAction extends PayeAuthAction {
  override def invokeBlock[A](request: Request[A], block: PayeAuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    block(PayeAuthenticatedRequest("1234567890", testNino, request))
  }
}


