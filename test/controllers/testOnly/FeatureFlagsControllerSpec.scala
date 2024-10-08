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

package controllers.testOnly

import controllers.auth.FakeAuthAction.mcc
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.OK
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import testOnly.controllers.FeatureFlagsController
import utils.BaseSpec

import scala.concurrent.Future

class FeatureFlagsControllerSpec extends BaseSpec {

  val controller = new FeatureFlagsController(mcc, mockFeatureFlagService)

  "PUT /setDefaults" must {
    "return a ok response" when {
      "default values are successfully set" in {
        when(mockFeatureFlagService.setAll(any())).thenReturn(Future.successful(()))

        val result = controller.setDefaults()(
          FakeRequest().withHeaders("Authorization" -> "Token some-token")
        )

        status(result) mustBe OK
        contentAsString(result) mustBe "Default flags set"
      }
    }
  }
}
