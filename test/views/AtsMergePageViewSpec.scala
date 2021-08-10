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
import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import play.api.data.Form
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.{SaUtr, Uar}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import utils.TestConstants
import view_models.AtsForms
import view_models.{AtsList, AtsMergePageViewModel}
import views.html.AtsMergePageView

import scala.concurrent.Future

class AtsMergePageViewSpec extends ViewSpecBase with TestConstants with BeforeAndAfterEach {
  lazy implicit val mockAppConfig = mock[ApplicationConfig]
  override val taxYear = 2015

  implicit val agentRequest = AuthenticatedRequest(
    "userId",
    Some(Uar(testUar)),
    Some(SaUtr(testUtr)),
    None,
    true,
    ConfidenceLevel.L50,
    fakeCredentials,
    FakeRequest("Get", s"?taxYear=$taxYear"))

  lazy val atsMergePageView = inject[AtsMergePageView]
  lazy val atsForms = inject[AtsForms]

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

  def view(model: AtsMergePageViewModel, form: Form[AtsYearChoice])(implicit request: AuthenticatedRequest[_]): String =
    atsMergePageView(model, form).body

  override def beforeEach() = {
    when(mockAppConfig.payeShuttered).thenReturn(false)
    when(mockAppConfig.saShuttered).thenReturn(false)
    when(mockAppConfig.taxYear).thenReturn(2020)
    when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(5)
    when(mockAppConfig.reportAProblemPartialUrl).thenReturn(appConfig.reportAProblemPartialUrl)
  }

  "view" must {

    "display the page heading" in {
      val result = view(
        AtsMergePageViewModel(AtsList("", "", "", List()), List.empty, mockAppConfig, ConfidenceLevel.L200),
        atsForms.atsYearFormMapping)

      result must include(messages("ats.summary.title"))
    }

    "display h1" in {
      val result =
        view(
          AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, mockAppConfig, ConfidenceLevel.L200),
          atsForms.atsYearFormMapping)

      result must include(messages("merge.page.ats.select_tax_year.title"))
    }

    "show generic no ats message and radiobuttons if there are years missing from paye and sa data from 2018" in {
      val result =
        view(
          AtsMergePageViewModel(
            AtsList("", "", "", List(2015, 2016, 2017, 2018)),
            List.empty,
            mockAppConfig,
            ConfidenceLevel.L200),
          atsForms.atsYearFormMapping)

      result must include(messages("merge.page.no.ats.summary.text"))
      result must include(messages("2018 to 2019 for a general Annual Tax Summary"))
      result must include(messages("2019 to 2020 for a general Annual Tax Summary"))
    }

    "not show generic no ats message nor radiobuttons if there are no years missing from paye and sa data from 2018" in {
      val result =
        view(
          AtsMergePageViewModel(
            AtsList("", "", "", List.empty),
            List(2015, 2016, 2017, 2018, 2019, 2020),
            mockAppConfig,
            ConfidenceLevel.L200),
          atsForms.atsYearFormMapping
        )

      result must not include messages("merge.page.no.ats.summary.text")
      result must not include messages("2018 to 2019 for a general Annual Tax Summary")
      result must not include messages("2019 to 2020 for a general Annual Tax Summary")
    }

    "show no ats before 2018 message if there are years missing from paye and sa data before 2018" in {
      val result =
        view(
          AtsMergePageViewModel(AtsList("", "", "", List(2018, 2019)), List.empty, mockAppConfig, ConfidenceLevel.L200),
          atsForms.atsYearFormMapping)

      result must include(messages("merge.page.no.ats.summary.unavailable.text"))
    }

    "not show no ats before 2018 message if there are no years missing from paye and sa data before 2018" in {
      val result =
        view(
          AtsMergePageViewModel(
            AtsList("", "", "", List(2015, 2016, 2017, 2018, 2019)),
            List.empty,
            mockAppConfig,
            ConfidenceLevel.L200),
          atsForms.atsYearFormMapping)

      result must not include messages("merge.page.no.ats.summary.unavailable.text")
    }

    "show radiobuttons if there is paye data" in {
      val result = view(
        AtsMergePageViewModel(
          AtsList("", "", "", List.empty),
          List(2015, 2016, 2017, 2018, 2019, 2020),
          mockAppConfig,
          ConfidenceLevel.L200),
        atsForms.atsYearFormMapping
      )(request = requestWithCL200)

      result must include("2019 to 2020 for PAYE")
      result must include("2018 to 2019 for PAYE")
      result must include("2017 to 2018 for PAYE")
      result must include("2016 to 2017 for PAYE")
      result must include("2015 to 2016 for PAYE")
    }

    "not show radiobuttons if paye data is not present" in {
      val result =
        view(
          AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, mockAppConfig, ConfidenceLevel.L200),
          atsForms.atsYearFormMapping)(request = requestWithCL200)

      result must not include "for PAYE"
    }

    "show showIvUpliftLink if paye data is present and CL is lower than 200" in {
      val result =
        view(
          AtsMergePageViewModel(AtsList("", "", "", List.empty), List(2015), mockAppConfig, ConfidenceLevel.L50),
          atsForms.atsYearFormMapping)(request = requestWithCL50)

      result must include(messages("merge.page.paye.ivuplift.text"))
    }

    "not show showIvUpliftLink if paye data is not present and CL is lower than 200" in {
      val result =
        view(
          AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, mockAppConfig, ConfidenceLevel.L50),
          atsForms.atsYearFormMapping)(request = requestWithCL50)

      result must not include (messages("merge.page.paye.ivuplift.text"))
    }

    "not show showIvUpliftLink if paye data is present and CL is 200" in {
      val result =
        view(
          AtsMergePageViewModel(AtsList("", "", "", List.empty), List(2015), mockAppConfig, ConfidenceLevel.L200),
          atsForms.atsYearFormMapping)(request = requestWithCL200)

      result must not include (messages("merge.page.paye.ivuplift.text"))
    }

    "not show paye shuttered message if service is shuttered" in {
      val result = view(
        AtsMergePageViewModel(AtsList("", "", "", List.empty), List(1), mockAppConfig, ConfidenceLevel.L200),
        atsForms.atsYearFormMapping)(request = requestWithCL200)
      result mustNot include(messages("merge.page.paye.unavailable"))
    }

    "show paye shuttered message if service is shuttered" in {
      when(mockAppConfig.payeShuttered).thenReturn(true)
      val result = view(
        AtsMergePageViewModel(AtsList("", "", "", List.empty), List(1), mockAppConfig, ConfidenceLevel.L200),
        atsForms.atsYearFormMapping)(request = requestWithCL200)
      result must include(messages("merge.page.paye.unavailable"))
    }

    "show radiobuttons if there is sa data" in {
      val result = view(
        AtsMergePageViewModel(
          AtsList("", "", "", List(2015, 2016, 2017, 2018, 2019, 2020)),
          List.empty,
          mockAppConfig,
          ConfidenceLevel.L200),
        atsForms.atsYearFormMapping
      )

      result must include("2019 to 2020 for Self Assessment")
      result must include("2018 to 2019 for Self Assessment")
      result must include("2017 to 2018 for Self Assessment")
      result must include("2016 to 2017 for Self Assessment")
      result must include("2015 to 2016 for Self Assessment")
    }

    "not show radiobuttons if sa data is not present" in {
      val result =
        view(
          AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, mockAppConfig, ConfidenceLevel.L200),
          atsForms.atsYearFormMapping)

      result must not include "for Self Assessment"
    }

    "not show sa shuttered message if service is shuttered" in {
      val result = view(
        AtsMergePageViewModel(AtsList("", "", "", List.empty), List(1), mockAppConfig, ConfidenceLevel.L200),
        atsForms.atsYearFormMapping)
      result mustNot include(messages("merge.page.sa.unavailable"))
    }

    "show sa shuttered message if service is shuttered" in {
      when(mockAppConfig.saShuttered).thenReturn(true)
      val result = view(
        AtsMergePageViewModel(AtsList("", "", "", List.empty), List(1), mockAppConfig, ConfidenceLevel.L200),
        atsForms.atsYearFormMapping)
      result must include(messages("merge.page.sa.unavailable"))

    }

    "show paye uplisft header message if user only has paye data and needs uplift" in {
      when(mockAppConfig.saShuttered).thenReturn(true)
      val result = view(
        AtsMergePageViewModel(
          AtsList("", "", "", List.empty),
          List(2015, 2016, 2017, 2018, 2019, 2020),
          mockAppConfig,
          ConfidenceLevel.L50),
        atsForms.atsYearFormMapping)
      result must include(messages("merge.page.paye.ivuplift.header"))

    }
  }
}
