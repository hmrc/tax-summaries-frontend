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

import controllers.auth.AuthenticatedRequest
import models.AtsYearChoice
import play.api.data.Form
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.{SaUtr, Uar}
import utils.TestConstants
import view_models.AtsForms.atsYearFormMapping
import view_models.{AtsList, AtsMergePageViewModel}
import views.html.{NoAtsMergePageView, SaMergePageView}

class NoAtsMergePageViewSpec extends ViewSpecBase with TestConstants {

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

  lazy val noAtsMergePageView = inject[NoAtsMergePageView]

  def view(model: AtsMergePageViewModel, form: Form[AtsYearChoice]): String =
    noAtsMergePageView(model, form).body

  "view" should {

    "show no ats radio buttons and message if there are years missing from paye and sa data from 2018" in {
      val result = view(AtsMergePageViewModel(AtsList("", "", "", List()), List.empty, appConfig), atsYearFormMapping)

      result should include(messages("merge.page.no.ats.summary.unavailable.text"))
    }

    "not show no ats radio buttons and message if there are no years missing from paye and sa data from 2018" in {
      val result =
        view(
          AtsMergePageViewModel(AtsList("", "", "", List.empty), List(2015, 2016, 2017, 2018, 2019, 2020), appConfig),
          atsYearFormMapping)

      result should not include messages("merge.page.no.ats.summary.unavailable.text")
    }
  }
}
