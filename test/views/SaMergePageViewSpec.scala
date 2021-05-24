/*
 * Copyright 2021 HM Revenue & Customs
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

package views

import config.ApplicationConfig
import controllers.auth.AuthenticatedRequest
import models.AtsYearChoice
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.data.Form
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.{SaUtr, Uar}
import utils.TestConstants
import utils.TestConstants.{testUar, testUtr}
import view_models.AtsForms.atsYearFormMapping
import view_models.{Amount, AtsList, AtsMergePageViewModel, CapitalGains}
import views.html.SaMergePageView

class SaMergePageViewSpec extends ViewSpecBase with TestConstants {

  override lazy implicit val appConfig = mock[ApplicationConfig]

  val taxYear = 2015

  implicit val agentRequest = AuthenticatedRequest(
    "userId",
    Some(Uar(testUar)),
    Some(SaUtr(testUtr)),
    None,
    true,
    ConfidenceLevel.L50,
    fakeCredentials,
    FakeRequest("Get", s"?taxYear=$taxYear"))

  lazy val saMergePageView = inject[SaMergePageView]

  def view(model: AtsMergePageViewModel, form: Form[AtsYearChoice]): String =
    saMergePageView(model, form).body

  when(appConfig.saShuttered).thenReturn(false)

  "view" should {

    "show radiobuttons if there is sa data" in {
      val result = view(AtsMergePageViewModel(AtsList("", "", "", List(1)), List.empty, appConfig), atsYearFormMapping)

      result should include(messages("merge.page.sa.text"))
    }

    "not show radiobuttons if sa data is not present" in {
      val result =
        view(AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, appConfig), atsYearFormMapping)

      result should not include messages("merge.page.sa.text")
    }

    "not show sa shuttered message if service is shuttered" in {
      val result = view(AtsMergePageViewModel(AtsList("", "", "", List.empty), List(1), appConfig), atsYearFormMapping)
      result shouldNot include(messages("merge.page.sa.unavailable"))
    }

    "show sa shuttered message if service is shuttered" in {
      when(appConfig.saShuttered).thenReturn(true)
      val result = view(AtsMergePageViewModel(AtsList("", "", "", List.empty), List(1), appConfig), atsYearFormMapping)
      result should include(messages("merge.page.sa.unavailable"))

    }

  }
}
