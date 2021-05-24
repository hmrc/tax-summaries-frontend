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
import org.scalatestplus.mockito.MockitoSugar._
import play.api.data.Form
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.{SaUtr, Uar}
import utils.TestConstants
import view_models.AtsForms.atsYearFormMapping
import view_models.{AtsList, AtsMergePageViewModel}
import views.html.PayeMergePageView

class PayeMergePageViewSpec extends ViewSpecBase with TestConstants {

  override lazy implicit val appConfig = mock[ApplicationConfig]
  val taxYear = 2015

  val requestWithCL50 = AuthenticatedRequest(
    "userId",
    Some(Uar(testUar)),
    Some(SaUtr(testUtr)),
    None,
    true,
    ConfidenceLevel.L50,
    fakeCredentials,
    FakeRequest("Get", s"?taxYear=$taxYear"))

  val requestWithCL200 = AuthenticatedRequest(
    "userId",
    Some(Uar(testUar)),
    Some(SaUtr(testUtr)),
    None,
    true,
    ConfidenceLevel.L200,
    fakeCredentials,
    FakeRequest("Get", s"?taxYear=$taxYear"))

  lazy val payeMergePageView = inject[PayeMergePageView]

  def view(model: AtsMergePageViewModel, form: Form[AtsYearChoice])(implicit request: AuthenticatedRequest[_]): String =
    payeMergePageView(model, form).body

  when(appConfig.payeShuttered).thenReturn(false)

  "view" should {

    "show radiobuttons if there is paye data" in {
      val result = view(
        AtsMergePageViewModel(AtsList("", "", "", List.empty), List(1), appConfig)(requestWithCL200),
        atsYearFormMapping)(request = requestWithCL200)

      result should include(messages("merge.page.paye.text"))
    }

    "not show radiobuttons if paye data is not present" in {
      val result =
        view(
          AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, appConfig)(requestWithCL200),
          atsYearFormMapping)(request = requestWithCL200)

      result should not include messages("merge.page.paye.text")
    }

    "show showIvUpliftLink if paye data is present and CL is lower than 200" in {
      val result =
        view(
          AtsMergePageViewModel(AtsList("", "", "", List.empty), List(2015), appConfig)(requestWithCL50),
          atsYearFormMapping)(request = requestWithCL50)

      result should include(messages("merge.page.paye.ivuplift.text"))
    }

    "not show showIvUpliftLink if paye data is not present and CL is lower than 200" in {
      val result =
        view(
          AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, appConfig)(requestWithCL50),
          atsYearFormMapping)(request = requestWithCL50)

      result should not include (messages("merge.page.paye.ivuplift.text"))
    }

    "not show showIvUpliftLink if paye data is present and CL is 200" in {
      val result =
        view(
          AtsMergePageViewModel(AtsList("", "", "", List.empty), List(2015), appConfig)(requestWithCL200),
          atsYearFormMapping)(request = requestWithCL200)

      result should not include (messages("merge.page.paye.ivuplift.text"))
    }

    "not show paye shuttered message if service is shuttered" in {
      val result = view(
        AtsMergePageViewModel(AtsList("", "", "", List.empty), List(1), appConfig)(requestWithCL200),
        atsYearFormMapping)(request = requestWithCL200)
      result shouldNot include(messages("merge.page.paye.unavailable"))
    }

    "show paye shuttered message if service is shuttered" in {
      when(appConfig.payeShuttered).thenReturn(true)
      val result = view(
        AtsMergePageViewModel(AtsList("", "", "", List.empty), List(1), appConfig)(requestWithCL200),
        atsYearFormMapping)(request = requestWithCL200)
      result should include(messages("merge.page.paye.unavailable"))
    }

  }
}
