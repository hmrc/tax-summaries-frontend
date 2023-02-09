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

package views

import config.ApplicationConfig
import controllers.auth.AuthenticatedRequest
import models.{ActingAsAttorneyFor, AtsYearChoice, PAYE, SA}
import org.jsoup.Jsoup
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import play.api.data.Form
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.{SaUtr, Uar}
import utils.TestConstants
import view_models.{AtsForms, AtsList, AtsMergePageViewModel}
import views.html.AtsMergePageView

import scala.util.Random

class AtsMergePageViewSpec extends ViewSpecBase with TestConstants with BeforeAndAfterEach {
  lazy implicit val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]

  implicit val agentRequest = AuthenticatedRequest(
    "userId",
    Some(Uar(testUar)),
    Some(SaUtr(testUtr)),
    None,
    true,
    false,
    ConfidenceLevel.L50,
    fakeCredentials,
    FakeRequest("Get", s"?taxYear=$taxYear"),
    None
  )

  lazy val atsMergePageView = inject[AtsMergePageView]
  lazy val atsForms         = inject[AtsForms]

  val requestWithCL50 = AuthenticatedRequest(
    "userId",
    Some(Uar(testUar)),
    Some(SaUtr(testUtr)),
    None,
    true,
    false,
    ConfidenceLevel.L50,
    fakeCredentials,
    FakeRequest("Get", s"?taxYear=$taxYear"),
    None
  )

  val requestWithCL200 = AuthenticatedRequest(
    "userId",
    Some(Uar(testUar)),
    Some(SaUtr(testUtr)),
    None,
    true,
    false,
    ConfidenceLevel.L200,
    fakeCredentials,
    FakeRequest("Get", s"?taxYear=$taxYear"),
    None
  )

  def view(model: AtsMergePageViewModel, form: Form[AtsYearChoice])(implicit request: AuthenticatedRequest[_]): String =
    atsMergePageView(model, form)(implicitly, implicitly, mockAppConfig, implicitly).body

  def agentView(model: AtsMergePageViewModel, form: Form[AtsYearChoice])(implicit
    request: AuthenticatedRequest[_]
  ): String =
    atsMergePageView(model, form, Some(ActingAsAttorneyFor(Some("Agent"), Map())))(
      implicitly,
      implicitly,
      mockAppConfig,
      implicitly
    ).body

  override def beforeEach() = {
    when(mockAppConfig.payeShuttered).thenReturn(false)
    when(mockAppConfig.saShuttered).thenReturn(false)
    when(mockAppConfig.taxYear).thenReturn(taxYear)
    when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(5)
  }

  "view" must {

    "display the page heading" in {
      val result = view(
        AtsMergePageViewModel(AtsList("", "", "", List()), List.empty, mockAppConfig, ConfidenceLevel.L200),
        atsForms.atsYearFormMapping
      )

      result must include(messages("merge.page.ats.select_tax_year.title"))
    }

    "display h1" in {
      val result =
        view(
          AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, mockAppConfig, ConfidenceLevel.L200),
          atsForms.atsYearFormMapping
        )

      result must include(messages("merge.page.ats.select_tax_year.title"))
    }

    s"show generic no ats message and radiobuttons if there are years missing from paye and sa data from ${taxYear - 2}" in {

      when(mockAppConfig.taxYear).thenReturn(2022)

      val result =
        view(
          AtsMergePageViewModel(
            AtsList("", "", "", List(taxYear - 5, taxYear - 4, taxYear - 3, taxYear - 2)),
            List.empty,
            mockAppConfig,
            ConfidenceLevel.L200
          ),
          atsForms.atsYearFormMapping
        )

      result must include(messages("merge.page.no.ats.summary.text"))
      result must include(messages(s"${taxYear - 2} to ${taxYear - 1} for a general Annual Tax Summary"))
      result must include(messages(s"${taxYear - 1} to $taxYear for a general Annual Tax Summary"))
    }

    s"not show generic no ats message nor radiobuttons if there are no years missing from paye and sa data from ${taxYear - 2}" in {
      val result =
        view(
          AtsMergePageViewModel(
            AtsList("", "", "", List.empty),
            (mockAppConfig.taxYear - 5 to mockAppConfig.taxYear).toList,
            mockAppConfig,
            ConfidenceLevel.L200
          ),
          atsForms.atsYearFormMapping
        )

      result must not include messages("merge.page.no.ats.summary.text")
      result must not include messages(s"${taxYear - 2} to ${taxYear - 1} for a general Annual Tax Summary")
      result must not include messages(s"${taxYear - 1} to $taxYear for a general Annual Tax Summary")
    }

    s"show no ats before ${taxYear - 2} message if there are years missing from paye and sa data before ${taxYear - 2}" in {
      val result =
        view(
          AtsMergePageViewModel(
            AtsList("", "", "", List(taxYear - 2, taxYear - 1)),
            List.empty,
            mockAppConfig,
            ConfidenceLevel.L200
          ),
          atsForms.atsYearFormMapping
        )

      result must include(messages("merge.page.no.ats.summary.unavailable.text"))
    }

    s"not show no ats before ${taxYear - 2} message if there are no years missing from paye and sa data before ${taxYear - 2}" in {
      val result =
        view(
          AtsMergePageViewModel(
            AtsList("", "", "", List(taxYear - 5, taxYear - 4, taxYear - 3, taxYear - 2, taxYear - 1)),
            List.empty,
            mockAppConfig,
            ConfidenceLevel.L200
          ),
          atsForms.atsYearFormMapping
        )

      result must not include messages("merge.page.no.ats.summary.unavailable.text")
    }

    "show radiobuttons if there is paye data" in {
      val result = view(
        AtsMergePageViewModel(
          AtsList("", "", "", List.empty),
          List(taxYear - 5, taxYear - 4, taxYear - 3, taxYear - 2, taxYear - 1, taxYear),
          mockAppConfig,
          ConfidenceLevel.L200
        ),
        atsForms.atsYearFormMapping
      )(request = requestWithCL200)

      result must include(s"${taxYear - 1} to $taxYear for PAYE")
      result must include(s"${taxYear - 2} to ${taxYear - 1} for PAYE")
      result must include(s"${taxYear - 3} to ${taxYear - 2} for PAYE")
      result must include(s"${taxYear - 4} to ${taxYear - 3} for PAYE")
      result must include(s"${taxYear - 5} to ${taxYear - 4} for PAYE")
    }

    "not show radiobuttons if paye data is not present" in {
      val result =
        view(
          AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, mockAppConfig, ConfidenceLevel.L200),
          atsForms.atsYearFormMapping
        )(request = requestWithCL200)

      result must not include "for PAYE"
    }

    "show showIvUpliftLink if paye data is present and CL is lower than 200" in {
      val result =
        view(
          AtsMergePageViewModel(AtsList("", "", "", List.empty), List(taxYear - 5), mockAppConfig, ConfidenceLevel.L50),
          atsForms.atsYearFormMapping
        )(request = requestWithCL50)

      result must include(messages("merge.page.paye.ivuplift.text"))
    }

    "not show showIvUpliftLink if paye data is not present and CL is lower than 200" in {
      val result =
        view(
          AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, mockAppConfig, ConfidenceLevel.L50),
          atsForms.atsYearFormMapping
        )(request = requestWithCL50)

      result must not include (messages("merge.page.paye.ivuplift.text"))
    }

    "not show showIvUpliftLink if paye data is present and CL is 200" in {
      val result =
        view(
          AtsMergePageViewModel(
            AtsList("", "", "", List.empty),
            List(taxYear - 5),
            mockAppConfig,
            ConfidenceLevel.L200
          ),
          atsForms.atsYearFormMapping
        )(request = requestWithCL200)

      result must not include (messages("merge.page.paye.ivuplift.text"))
    }

    "not show paye shuttered message if service is shuttered" in {
      val result = view(
        AtsMergePageViewModel(AtsList("", "", "", List.empty), List(1), mockAppConfig, ConfidenceLevel.L200),
        atsForms.atsYearFormMapping
      )(request = requestWithCL200)
      result mustNot include(messages("merge.page.paye.unavailable"))
    }

    "show paye shuttered message if service is shuttered" in {
      when(mockAppConfig.payeShuttered).thenReturn(true)
      val result = view(
        AtsMergePageViewModel(AtsList("", "", "", List.empty), List(1), mockAppConfig, ConfidenceLevel.L200),
        atsForms.atsYearFormMapping
      )(request = requestWithCL200)
      result must include(messages("merge.page.paye.unavailable"))
    }

    "show radiobuttons if there is sa data" in {
      val result = view(
        AtsMergePageViewModel(
          AtsList("", "", "", List(taxYear - 5, taxYear - 4, taxYear - 3, taxYear - 2, taxYear - 1, taxYear)),
          List.empty,
          mockAppConfig,
          ConfidenceLevel.L200
        ),
        atsForms.atsYearFormMapping
      )

      result must include(s"${taxYear - 1} to $taxYear for Self Assessment")
      result must include(s"${taxYear - 2} to ${taxYear - 1} for Self Assessment")
      result must include(s"${taxYear - 3} to ${taxYear - 2} for Self Assessment")
      result must include(s"${taxYear - 4} to ${taxYear - 3} for Self Assessment")
      result must include(s"${taxYear - 5} to ${taxYear - 4} for Self Assessment")
    }

    "not show radiobuttons if sa data is not present" in {
      val result =
        view(
          AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, mockAppConfig, ConfidenceLevel.L200),
          atsForms.atsYearFormMapping
        )

      result must not include "for Self Assessment"
    }

    "not show sa shuttered message if service is shuttered" in {
      val result = view(
        AtsMergePageViewModel(AtsList("", "", "", List.empty), List(1), mockAppConfig, ConfidenceLevel.L200),
        atsForms.atsYearFormMapping
      )
      result mustNot include(messages("merge.page.sa.unavailable"))
    }

    "show sa shuttered message if service is shuttered" in {
      when(mockAppConfig.saShuttered).thenReturn(true)
      val result = view(
        AtsMergePageViewModel(AtsList("", "", "", List.empty), List(1), mockAppConfig, ConfidenceLevel.L200),
        atsForms.atsYearFormMapping
      )
      result must include(messages("merge.page.sa.unavailable"))

    }

    "show paye uplift header message if user only has paye data and needs uplift" in {
      when(mockAppConfig.saShuttered).thenReturn(true)
      val result = view(
        AtsMergePageViewModel(
          AtsList("", "", "", List.empty),
          List(taxYear - 5, taxYear - 4, taxYear - 3, taxYear - 2, taxYear - 1, taxYear),
          mockAppConfig,
          ConfidenceLevel.L50
        ),
        atsForms.atsYearFormMapping
      )
      result must include(messages("merge.page.paye.ivuplift.header"))
    }

    "not show account menu for agent" in {

      val result = agentView(
        AtsMergePageViewModel(
          AtsList("", "", "", List.empty),
          List(taxYear - 5, taxYear - 4, taxYear - 3, taxYear - 2, taxYear - 1, taxYear),
          mockAppConfig,
          ConfidenceLevel.L50
        ),
        atsForms.atsYearFormMapping
      )
      result must not include "hmrc-account-menu"
    }

    "show account menu for non agent users" in {

      val result = view(
        AtsMergePageViewModel(
          AtsList("", "", "", List.empty),
          List(taxYear - 5, taxYear - 4, taxYear - 3, taxYear - 2, taxYear - 1, taxYear),
          mockAppConfig,
          ConfidenceLevel.L50
        ),
        atsForms.atsYearFormMapping
      )
      result must include("hmrc-account-menu")
    }
    "have an error link to the first radio button if there is an error with SA data" in {
      val result = Jsoup.parse(
        view(
          AtsMergePageViewModel(
            AtsList("", "", "", List(taxYear - 5, taxYear - 4, taxYear - 3, taxYear - 2, taxYear - 1, taxYear)),
            List.empty,
            mockAppConfig,
            ConfidenceLevel.L200
          ),
          atsForms.atsYearFormMapping.withError("error", "broken")
        )
      )
      assert(!result.getElementsByAttributeValue("href", s"#year-$taxYear-SA").isEmpty)
    }

    "have an error link to the first radio button if there is an error with PAYE data" in {
      val result = Jsoup.parse(
        view(
          AtsMergePageViewModel(
            AtsList("", "", "", List(taxYear - 5, taxYear - 4, taxYear - 3, taxYear - 2)),
            List(taxYear, taxYear - 1),
            mockAppConfig,
            ConfidenceLevel.L200
          ),
          atsForms.atsYearFormMapping.withError("error", "broken")
        )
      )

      assert(!result.getElementsByAttributeValue("href", s"#year-$taxYear-PAYE").isEmpty)
    }

    "have an error link to the first radio button if there is an error no ATS" in {
      val result = Jsoup.parse(
        view(
          AtsMergePageViewModel(
            AtsList("", "", "", List(taxYear - 5, taxYear - 4, taxYear - 3, taxYear - 2)),
            List.empty,
            mockAppConfig,
            ConfidenceLevel.L200
          ),
          atsForms.atsYearFormMapping.withError("error", "broken")
        )
      )

      assert(!result.getElementsByAttributeValue("href", s"#year-$taxYear-NoATS").isEmpty)
    }

    "have the correct radio option checked when form is filled with SA value" in {
      val result = Jsoup.parse(
        view(
          AtsMergePageViewModel(
            AtsList("", "", "", List(taxYear - 5, taxYear - 4, taxYear - 3, taxYear - 2, taxYear - 1, taxYear)),
            List.empty,
            mockAppConfig,
            ConfidenceLevel.L200
          ),
          atsForms.atsYearFormMapping.fill(AtsYearChoice(SA, taxYear))
        )
      )
      assert(result.getElementById(s"year-$taxYear-SA").hasAttr("checked"))
    }

    "have the correct radio option checked when form is filled with PAYE value" in {
      val result = Jsoup.parse(
        view(
          AtsMergePageViewModel(
            AtsList("", "", "", List(taxYear - 5, taxYear - 4, taxYear - 2, taxYear - 1)),
            List(taxYear - 3, taxYear),
            mockAppConfig,
            ConfidenceLevel.L200
          ),
          atsForms.atsYearFormMapping.fill(AtsYearChoice(PAYE, taxYear - 3))
        )
      )
      assert(result.getElementById(s"year-${taxYear - 3}-PAYE").hasAttr("checked"))
    }

    "show the number of unread messages in the nav menu" in {
      val messageCount = Random.nextInt(100) + 1

      val result = atsMergePageView(
        AtsMergePageViewModel(AtsList("", "", "", List()), List.empty, mockAppConfig, ConfidenceLevel.L200),
        atsForms.atsYearFormMapping
      )(
        request = requestWithCL200.copy(unreadMessageCount = Some(messageCount)),
        implicitly,
        implicitly,
        implicitly
      ).body

      result must include(s"""<span class="hmrc-notification-badge">$messageCount</span>""")
    }
  }
}
