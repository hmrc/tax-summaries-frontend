/*
 * Copyright 2025 HM Revenue & Customs
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

package common.controllers

import play.api.test.FakeRequest
import play.api.test.Helpers.*
import common.utils.BaseSpec

class AccountControllerTest extends BaseSpec {

  private val controller: AccountController = inject[AccountController]

  "signOut" must {

    "redirect user to bas gateway with feedback url" in {
      val expUrl: String = appConfig.basGatewaySignOut(appConfig.survey)
      val result         = controller.signOut(FakeRequest())
      redirectLocation(result) mustBe Some(expUrl)
    }
  }

  "keepAlive" must {

    "return NoContent" in {
      val result = controller.keepAlive(FakeRequest())
      status(result) mustBe NO_CONTENT
    }
  }

  "sessionExpired" must {

    "return OK and render session expired view" in {
      val result = controller.sessionExpired(FakeRequest())

      status(result) mustBe OK
      contentAsString(result) must include("For your security, we signed you out")
    }
  }
}
