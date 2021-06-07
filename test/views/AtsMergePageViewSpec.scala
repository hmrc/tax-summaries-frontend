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
import org.scalatest.BeforeAndAfterEach
import play.api.data.Form
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.{SaUtr, Uar}
import utils.{MockPartialRetriever, TestConstants}
import view_models.AtsForms.atsYearFormMapping
import view_models.{AtsList, AtsMergePageViewModel}
import views.html.AtsMergePageView
import org.scalatestplus.mockito.MockitoSugar._
import uk.gov.hmrc.play.partials.FormPartialRetriever

class AtsMergePageViewSpec extends ViewSpecBase with TestConstants with BeforeAndAfterEach {
  override implicit val formPartialRetriever: FormPartialRetriever = MockPartialRetriever
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

  lazy val atsMergePageView = inject[AtsMergePageView]

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
    when(appConfig.payeShuttered).thenReturn(false)
    when(appConfig.saShuttered).thenReturn(false)
    when(appConfig.taxYear).thenReturn(2020)
    when(appConfig.maxTaxYearsTobeDisplayed).thenReturn(5)
  }

  "view" should {

    "display the page heading" in {
      val result = view(
        AtsMergePageViewModel(AtsList("", "", "", List()), List.empty, appConfig, ConfidenceLevel.L200),
        atsYearFormMapping)

      result should include(messages("ats.summary.title"))
    }

    "display h1" in {
      val result =
        view(
          AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, appConfig, ConfidenceLevel.L200),
          atsYearFormMapping)

      result should include(messages("merge.page.ats.select_tax_year.title"))
    }

    "show generic no ats message and radiobuttons if there are years missing from paye and sa data from 2018" in {
      val result =
        view(
          AtsMergePageViewModel(
            AtsList("", "", "", List(2015, 2016, 2017, 2018)),
            List.empty,
            appConfig,
            ConfidenceLevel.L200),
          atsYearFormMapping)

      result should include(messages("merge.page.no.ats.summary.text"))
      result should include(messages("2018 to 2019 for a general Annual Tax Summary"))
      result should include(messages("2019 to 2020 for a general Annual Tax Summary"))
    }

    "not show generic no ats message nor radiobuttons if there are no years missing from paye and sa data from 2018" in {
      val result =
        view(
          AtsMergePageViewModel(
            AtsList("", "", "", List.empty),
            List(2015, 2016, 2017, 2018, 2019, 2020),
            appConfig,
            ConfidenceLevel.L200),
          atsYearFormMapping)

      result should not include messages("merge.page.no.ats.summary.text")
      result should not include messages("2018 to 2019 for a general Annual Tax Summary")
      result should not include messages("2019 to 2020 for a general Annual Tax Summary")
    }

    "show no ats before 2018 message if there are years missing from paye and sa data before 2018" in {
      val result =
        view(
          AtsMergePageViewModel(AtsList("", "", "", List(2018, 2019)), List.empty, appConfig, ConfidenceLevel.L200),
          atsYearFormMapping)

      result should include(messages("merge.page.no.ats.summary.unavailable.text"))
    }

    "not show no ats before 2018 message if there are no years missing from paye and sa data before 2018" in {
      val result =
        view(
          AtsMergePageViewModel(
            AtsList("", "", "", List(2015, 2016, 2017, 2018, 2019)),
            List.empty,
            appConfig,
            ConfidenceLevel.L200),
          atsYearFormMapping)

      result should not include messages("merge.page.no.ats.summary.unavailable.text")
    }

    "show radiobuttons if there is paye data" in {
      val result = view(
        AtsMergePageViewModel(
          AtsList("", "", "", List.empty),
          List(2015, 2016, 2017, 2018, 2019, 2020),
          appConfig,
          ConfidenceLevel.L200),
        atsYearFormMapping
      )(request = requestWithCL200)

      result should include("2019 to 2020 for PAYE")
      result should include("2018 to 2019 for PAYE")
      result should include("2017 to 2018 for PAYE")
      result should include("2016 to 2017 for PAYE")
      result should include("2015 to 2016 for PAYE")
    }

    "not show radiobuttons if paye data is not present" in {
      val result =
        view(
          AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, appConfig, ConfidenceLevel.L200),
          atsYearFormMapping)(request = requestWithCL200)

      result should not include "for PAYE"
    }

    "show showIvUpliftLink if paye data is present and CL is lower than 200" in {
      val result =
        view(
          AtsMergePageViewModel(AtsList("", "", "", List.empty), List(2015), appConfig, ConfidenceLevel.L50),
          atsYearFormMapping)(request = requestWithCL50)

      result should include(messages("merge.page.paye.ivuplift.text"))
    }

    "not show showIvUpliftLink if paye data is not present and CL is lower than 200" in {
      val result =
        view(
          AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, appConfig, ConfidenceLevel.L50),
          atsYearFormMapping)(request = requestWithCL50)

      result should not include (messages("merge.page.paye.ivuplift.text"))
    }

    "not show showIvUpliftLink if paye data is present and CL is 200" in {
      val result =
        view(
          AtsMergePageViewModel(AtsList("", "", "", List.empty), List(2015), appConfig, ConfidenceLevel.L200),
          atsYearFormMapping)(request = requestWithCL200)

      result should not include (messages("merge.page.paye.ivuplift.text"))
    }

    "not show paye shuttered message if service is shuttered" in {
      val result = view(
        AtsMergePageViewModel(AtsList("", "", "", List.empty), List(1), appConfig, ConfidenceLevel.L200),
        atsYearFormMapping)(request = requestWithCL200)
      result shouldNot include(messages("merge.page.paye.unavailable"))
    }

    "show paye shuttered message if service is shuttered" in {
      when(appConfig.payeShuttered).thenReturn(true)
      val result = view(
        AtsMergePageViewModel(AtsList("", "", "", List.empty), List(1), appConfig, ConfidenceLevel.L200),
        atsYearFormMapping)(request = requestWithCL200)
      result should include(messages("merge.page.paye.unavailable"))
    }

    "show radiobuttons if there is sa data" in {
      val result = view(
        AtsMergePageViewModel(
          AtsList("", "", "", List(2015, 2016, 2017, 2018, 2019, 2020)),
          List.empty,
          appConfig,
          ConfidenceLevel.L200),
        atsYearFormMapping)

      result should include("2019 to 2020 for Self Assessment")
      result should include("2018 to 2019 for Self Assessment")
      result should include("2017 to 2018 for Self Assessment")
      result should include("2016 to 2017 for Self Assessment")
      result should include("2015 to 2016 for Self Assessment")
    }

    "not show radiobuttons if sa data is not present" in {
      val result =
        view(
          AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, appConfig, ConfidenceLevel.L200),
          atsYearFormMapping)

      result should not include "for Self Assessment"
    }

    "not show sa shuttered message if service is shuttered" in {
      val result = view(
        AtsMergePageViewModel(AtsList("", "", "", List.empty), List(1), appConfig, ConfidenceLevel.L200),
        atsYearFormMapping)
      result shouldNot include(messages("merge.page.sa.unavailable"))
    }

    "show sa shuttered message if service is shuttered" in {
      when(appConfig.saShuttered).thenReturn(true)
      val result = view(
        AtsMergePageViewModel(AtsList("", "", "", List.empty), List(1), appConfig, ConfidenceLevel.L200),
        atsYearFormMapping)
      result should include(messages("merge.page.sa.unavailable"))

    }
  }
}
