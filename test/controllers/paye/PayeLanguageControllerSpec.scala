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

package controllers.paye

import config.ApplicationConfig
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, _}
import uk.gov.hmrc.play.language.LanguageUtils
import utils.ControllerBaseSpec

class PayeLanguageControllerSpec extends ControllerBaseSpec {

  val langUtils: LanguageUtils             = app.injector.instanceOf[LanguageUtils]
  val applicationConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

  def sut = new PayeLanguageController(langUtils, mcc, applicationConfig)

  val fakeRequest         = FakeRequest().withHeaders(("Referer", controllers.routes.ErrorController.notAuthorised.url))
  val redirectLocationUrl = controllers.routes.ErrorController.notAuthorised.url

  "SaLanguageController" must {

    "redirect to English translated page" in {
      val result =
        sut.switchToLanguage("english")(fakeRequest)
      cookies(result).get("PLAY_LANG").get.value mustBe "en"
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe redirectLocationUrl
    }

    "redirect to Welsh translated page" in {
      val result =
        sut.switchToLanguage("welsh")(fakeRequest)
      cookies(result).get("PLAY_LANG").get.value mustBe "cy"
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe redirectLocationUrl
    }

    "redirect back to a fallback url when the Referer is empty " in {

      val result = sut.switchToLanguage("welsh")(FakeRequest())

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe "/annual-tax-summary/paye/main"

    }
  }
}
