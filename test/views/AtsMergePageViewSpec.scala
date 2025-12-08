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

package views

import config.ApplicationConfig
import models.requests
import models.requests.AuthenticatedRequest
import models.{ActingAsAttorneyFor, AtsYearChoice, PAYE, SA, requests}
import org.jsoup.Jsoup
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import play.api.data.Form
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.{SaUtr, Uar}
import utils.TestConstants
import view_models.{AtsForms, AtsList, AtsMergePageViewModel}
import views.html.AtsMergePageView

class AtsMergePageViewSpec extends ViewSpecBase with TestConstants with BeforeAndAfterEach {
  lazy implicit val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]

  implicit val agentRequest: AuthenticatedRequest[AnyContentAsEmpty.type] = requests.AuthenticatedRequest(
    "userId",
    Some(Uar(testUar)),
    Some(SaUtr(testUtr)),
    None,
    isAgentActive = false,
    ConfidenceLevel.L50,
    fakeCredentials,
    FakeRequest("Get", s"?taxYear=$currentTaxYearSA")
  )

  lazy val atsMergePageView: AtsMergePageView = inject[AtsMergePageView]
  lazy val atsForms: AtsForms                 = inject[AtsForms]

  val requestWithCL50: AuthenticatedRequest[AnyContentAsEmpty.type] = requests.AuthenticatedRequest(
    "userId",
    Some(Uar(testUar)),
    Some(SaUtr(testUtr)),
    None,
    isAgentActive = false,
    ConfidenceLevel.L50,
    fakeCredentials,
    FakeRequest("Get", s"?taxYear=$currentTaxYearSA")
  )

  val requestWithCL200: AuthenticatedRequest[AnyContentAsEmpty.type] = requests.AuthenticatedRequest(
    "userId",
    Some(Uar(testUar)),
    Some(SaUtr(testUtr)),
    None,
    isAgentActive = false,
    ConfidenceLevel.L200,
    fakeCredentials,
    FakeRequest("Get", s"?taxYear=$currentTaxYearSA")
  )

  def view(
    model: AtsMergePageViewModel,
    form: Form[AtsYearChoice],
    payeAvailable: Boolean = true,
    saAvailable: Boolean = true
  )(implicit request: AuthenticatedRequest[_]): String =
    atsMergePageView(model, form, payeAvailable = payeAvailable, saAvailable = saAvailable)(request, implicitly).body

  def agentView(model: AtsMergePageViewModel, form: Form[AtsYearChoice]): String =
    atsMergePageView(
      model,
      form,
      Some(ActingAsAttorneyFor(Some("Agent"), Map())),
      saAvailable = true,
      payeAvailable = true
    )(
      implicitly,
      implicitly
    ).body

  override def beforeEach(): Unit = {
    reset(mockAppConfig)
    when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(4)
    ()
  }

  "view" when {
    "SA and PAYE tax years are the current test values" must {
      def resetMocks = {
        when(mockAppConfig.taxYearSA).thenReturn(currentTaxYearSA)
        when(mockAppConfig.taxYearPAYE).thenReturn(currentTaxYearPAYE)
      }

      "display the page heading" in {
        resetMocks
        val result = view(
          AtsMergePageViewModel(AtsList("", "", "", List()), List.empty, mockAppConfig, ConfidenceLevel.L200),
          atsForms.atsYearFormMapping
        )

        result must include(messages("merge.page.ats.select_tax_year.title"))
      }

      "display h1" in {
        resetMocks
        val result =
          view(
            AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, mockAppConfig, ConfidenceLevel.L200),
            atsForms.atsYearFormMapping
          )

        result must include(messages("merge.page.ats.select_tax_year.title"))
      }

      s"show generic no ats message and radiobuttons if there are years missing from paye and sa data from ${currentTaxYearSA - 2}" in {
        resetMocks
        when(mockAppConfig.taxYearSA).thenReturn(currentTaxYearSA)

        val result =
          view(
            AtsMergePageViewModel(
              AtsList(
                "",
                "",
                "",
                List(currentTaxYearSA - 5, currentTaxYearSA - 4, currentTaxYearSA - 3, currentTaxYearSA - 2)
              ),
              List.empty,
              mockAppConfig,
              ConfidenceLevel.L200
            ),
            atsForms.atsYearFormMapping
          )

        result must include(messages("merge.page.no.ats.summary.text"))
        result must include(
          messages(s"${currentTaxYearSA - 2} to ${currentTaxYearSA - 1} for a general Annual Tax Summary")
        )
        result must include(messages(s"${currentTaxYearSA - 1} to $currentTaxYearSA for a general Annual Tax Summary"))
      }

      s"not show no ats before ${currentTaxYearSA - 2} message if there are no years missing from paye and sa data before ${currentTaxYearSA - 2}" in {
        resetMocks
        val result =
          view(
            AtsMergePageViewModel(
              AtsList(
                "",
                "",
                "",
                List(
                  currentTaxYearSA - 5,
                  currentTaxYearSA - 4,
                  currentTaxYearSA - 3,
                  currentTaxYearSA - 2,
                  currentTaxYearSA - 1
                )
              ),
              List.empty,
              mockAppConfig,
              ConfidenceLevel.L200
            ),
            atsForms.atsYearFormMapping
          )

        result must not include messages("merge.page.no.ats.summary.unavailable.text")
      }

      "show radiobuttons if there is paye data when the paye is available and not show paye shuttered message" in {
        resetMocks
        val result = view(
          AtsMergePageViewModel(
            AtsList("", "", "", List.empty),
            List(
              currentTaxYearSA - 5,
              currentTaxYearSA - 4,
              currentTaxYearSA - 3,
              currentTaxYearSA - 2,
              currentTaxYearSA - 1,
              currentTaxYearSA
            ),
            mockAppConfig,
            ConfidenceLevel.L200
          ),
          atsForms.atsYearFormMapping
        )

        result must include(s"${currentTaxYearSA - 1} to $currentTaxYearSA for PAYE")
        result must include(s"${currentTaxYearSA - 2} to ${currentTaxYearSA - 1} for PAYE")
        result must include(s"${currentTaxYearSA - 3} to ${currentTaxYearSA - 2} for PAYE")
        result must include(s"${currentTaxYearSA - 4} to ${currentTaxYearSA - 3} for PAYE")
        result must include(s"${currentTaxYearSA - 5} to ${currentTaxYearSA - 4} for PAYE")
        result mustNot include(messages("merge.page.paye.unavailable"))

      }

      "not show radiobuttons if paye data is not present" in {
        resetMocks
        val result =
          view(
            AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, mockAppConfig, ConfidenceLevel.L200),
            atsForms.atsYearFormMapping
          )

        result must not include "for PAYE"
      }

      "show showIvUpliftLink if paye data is present and CL is lower than 200" in {
        resetMocks
        val result =
          view(
            AtsMergePageViewModel(
              AtsList("", "", "", List.empty),
              List(currentTaxYearSA - 5),
              mockAppConfig,
              ConfidenceLevel.L50
            ),
            atsForms.atsYearFormMapping
          )

        result must include(messages("merge.page.paye.ivuplift.text"))
      }

      "not show showIvUpliftLink if paye data is not present and CL is lower than 200" in {
        resetMocks
        val result =
          view(
            AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, mockAppConfig, ConfidenceLevel.L50),
            atsForms.atsYearFormMapping
          )

        result must not include messages("merge.page.paye.ivuplift.text")
      }

      "not show showIvUpliftLink if paye data is present and CL is 200" in {
        resetMocks
        val result =
          view(
            AtsMergePageViewModel(
              AtsList("", "", "", List.empty),
              List(currentTaxYearSA - 5),
              mockAppConfig,
              ConfidenceLevel.L200
            ),
            atsForms.atsYearFormMapping
          )

        result must not include messages("merge.page.paye.ivuplift.text")
      }

      "show paye shuttered message if paye service is not available" in {
        resetMocks
        val result = view(
          AtsMergePageViewModel(AtsList("", "", "", List.empty), List(1), mockAppConfig, ConfidenceLevel.L200),
          atsForms.atsYearFormMapping,
          payeAvailable = false
        )
        result must include(messages("merge.page.paye.unavailable"))
        result mustNot include(s"${currentTaxYearSA - 1} to $currentTaxYearSA for PAYE")
      }

      "show radiobuttons if there is sa data and not show sa shuttered message" in {
        resetMocks
        val result = view(
          AtsMergePageViewModel(
            AtsList(
              "",
              "",
              "",
              List(
                currentTaxYearSA - 5,
                currentTaxYearSA - 4,
                currentTaxYearSA - 3,
                currentTaxYearSA - 2,
                currentTaxYearSA - 1,
                currentTaxYearSA
              )
            ),
            List.empty,
            mockAppConfig,
            ConfidenceLevel.L200
          ),
          atsForms.atsYearFormMapping
        )

        result must include(s"${currentTaxYearSA - 1} to $currentTaxYearSA for Self Assessment")
        result must include(s"${currentTaxYearSA - 2} to ${currentTaxYearSA - 1} for Self Assessment")
        result must include(s"${currentTaxYearSA - 3} to ${currentTaxYearSA - 2} for Self Assessment")
        result must include(s"${currentTaxYearSA - 4} to ${currentTaxYearSA - 3} for Self Assessment")
        result must include(s"${currentTaxYearSA - 5} to ${currentTaxYearSA - 4} for Self Assessment")
        result mustNot include(messages("merge.page.sa.unavailable"))

      }

      "not show radiobuttons if sa data is not present" in {
        resetMocks
        val result =
          view(
            AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, mockAppConfig, ConfidenceLevel.L200),
            atsForms.atsYearFormMapping
          )

        result must not include "for Self Assessment"
      }

      "show sa shuttered message if sa service is not available" in {
        resetMocks
        val result = view(
          AtsMergePageViewModel(AtsList("", "", "", List.empty), List(1), mockAppConfig, ConfidenceLevel.L200),
          atsForms.atsYearFormMapping,
          saAvailable = false
        )
        result must include(messages("merge.page.sa.unavailable"))
        result mustNot include(s"${currentTaxYearSA - 1} to $currentTaxYearSA for Self Assessment")

      }

      "not show account menu for agent" in {
        resetMocks
        val result = agentView(
          AtsMergePageViewModel(
            AtsList("", "", "", List.empty),
            List(
              currentTaxYearSA - 5,
              currentTaxYearSA - 4,
              currentTaxYearSA - 3,
              currentTaxYearSA - 2,
              currentTaxYearSA - 1,
              currentTaxYearSA
            ),
            mockAppConfig,
            ConfidenceLevel.L50
          ),
          atsForms.atsYearFormMapping
        )
        result must not include "hmrc-account-menu"
      }

      "show account menu for non agent users" in {
        resetMocks
        val result = view(
          AtsMergePageViewModel(
            AtsList("", "", "", List.empty),
            List(
              currentTaxYearSA - 5,
              currentTaxYearSA - 4,
              currentTaxYearSA - 3,
              currentTaxYearSA - 2,
              currentTaxYearSA - 1,
              currentTaxYearSA
            ),
            mockAppConfig,
            ConfidenceLevel.L50
          ),
          atsForms.atsYearFormMapping
        )
        result must include("hmrc-account-menu")
      }

      "have an error link to the first radio button if there is an error no ATS" in {
        resetMocks
        val result = Jsoup.parse(
          view(
            AtsMergePageViewModel(
              saData = AtsList(
                "",
                "",
                "",
                List(currentTaxYearSA - 5, currentTaxYearSA - 4, currentTaxYearSA - 3, currentTaxYearSA - 2)
              ),
              payeTaxYearList = List.empty,
              appConfig = mockAppConfig,
              confidenceLevel = ConfidenceLevel.L200
            ),
            atsForms.atsYearFormMapping.withError("error", "broken")
          )
        )

        assert(!result.getElementsByAttributeValue("href", s"#year-$latestAvailableYear-NoATS").isEmpty)
      }

      "have the correct radio option checked when form is filled with SA value" in {
        resetMocks
        val result = Jsoup.parse(
          view(
            AtsMergePageViewModel(
              AtsList(
                "",
                "",
                "",
                List(
                  currentTaxYearSA - 5,
                  currentTaxYearSA - 4,
                  currentTaxYearSA - 3,
                  currentTaxYearSA - 2,
                  currentTaxYearSA - 1,
                  currentTaxYearSA
                )
              ),
              List.empty,
              mockAppConfig,
              ConfidenceLevel.L200
            ),
            atsForms.atsYearFormMapping.fill(AtsYearChoice(SA, currentTaxYearSA))
          )
        )
        assert(result.getElementById(s"year-$currentTaxYearSA-SA").hasAttr("checked"))
      }

      "have the correct radio option checked when form is filled with PAYE value" in {
        resetMocks
        val result = Jsoup.parse(
          view(
            AtsMergePageViewModel(
              saData = AtsList(
                "",
                "",
                "",
                List(currentTaxYearSA - 5, currentTaxYearSA - 4, currentTaxYearSA - 2, currentTaxYearSA - 1)
              ),
              payeTaxYearList = List(currentTaxYearPAYE - 3, currentTaxYearPAYE),
              appConfig = mockAppConfig,
              confidenceLevel = ConfidenceLevel.L200
            ),
            atsForms.atsYearFormMapping.fill(AtsYearChoice(PAYE, currentTaxYearPAYE - 3))
          )
        )
        assert(result.getElementById(s"year-${currentTaxYearPAYE - 3}-PAYE").hasAttr("checked"))
      }

    }
    "SA, PAYE & gov spend tax years are the same" must {
      s"not show generic no ats message nor radiobuttons if user only has paye data & it's for all years" in {
        when(mockAppConfig.taxYearSA).thenReturn(currentTaxYearPAYE)
        when(mockAppConfig.taxYearPAYE).thenReturn(currentTaxYearPAYE)
        when(mockAppConfig.taxYearGovSpend).thenReturn(currentTaxYearPAYE)
        val result =
          view(
            AtsMergePageViewModel(
              saData = AtsList("", "", "", List.empty),
              payeTaxYearList = (currentTaxYearPAYE - 5 to currentTaxYearPAYE).toList,
              appConfig = mockAppConfig,
              confidenceLevel = ConfidenceLevel.L200
            ),
            atsForms.atsYearFormMapping
          )

        result must not include messages("merge.page.no.ats.summary.text")
        allYears(Seq(currentTaxYearSA, currentTaxYearSA, currentTaxYearGovSpend)).foreach { year =>
          result must not include messages(
            s"${year - 1} to $year for a general Annual Tax Summary"
          )
        }

      }

      "show paye uplift header message if user only has paye data & it's for all years and needs uplift" in {
        when(mockAppConfig.taxYearSA).thenReturn(currentTaxYearPAYE)
        when(mockAppConfig.taxYearPAYE).thenReturn(currentTaxYearPAYE)
        when(mockAppConfig.taxYearGovSpend).thenReturn(currentTaxYearPAYE)
        val result = view(
          AtsMergePageViewModel(
            saData = AtsList("", "", "", List.empty),
            payeTaxYearList = (currentTaxYearPAYE - 5 to currentTaxYearPAYE).toList,
            appConfig = mockAppConfig,
            confidenceLevel = ConfidenceLevel.L50
          ),
          atsForms.atsYearFormMapping
        )
        result.contains(messages("merge.page.paye.ivuplift.header")) mustBe true
      }

      "have an error link to the first radio button if there is an error with SA data" in {
        when(mockAppConfig.taxYearSA).thenReturn(currentTaxYearSA)
        when(mockAppConfig.taxYearPAYE).thenReturn(currentTaxYearSA)
        when(mockAppConfig.taxYearGovSpend).thenReturn(currentTaxYearSA)
        val result = Jsoup.parse(
          view(
            AtsMergePageViewModel(
              saData = AtsList("", "", "", (currentTaxYearSA - 5 to currentTaxYearSA).toList),
              payeTaxYearList = List.empty,
              appConfig = mockAppConfig,
              confidenceLevel = ConfidenceLevel.L200
            ),
            atsForms.atsYearFormMapping.withError("error", "broken")
          )
        )

        assert(!result.getElementsByAttributeValue("href", s"#year-$currentTaxYearSA-SA").isEmpty)
      }

      "have an error link to the first radio button if there is an error" in {
        when(mockAppConfig.taxYearSA).thenReturn(currentTaxYearSA)
        when(mockAppConfig.taxYearPAYE).thenReturn(currentTaxYearSA)
        when(mockAppConfig.taxYearGovSpend).thenReturn(currentTaxYearSA)
        val result = Jsoup.parse(
          view(
            AtsMergePageViewModel(
              saData = AtsList(
                "",
                "",
                "",
                List(currentTaxYearSA - 5, currentTaxYearSA - 4, currentTaxYearSA - 3, currentTaxYearSA - 2)
              ),
              payeTaxYearList = List(currentTaxYearSA, currentTaxYearSA - 1),
              appConfig = mockAppConfig,
              confidenceLevel = ConfidenceLevel.L200
            ),
            atsForms.atsYearFormMapping.withError("error", "broken")
          )
        )

        assert(!result.getElementsByAttributeValue("href", s"#year-$currentTaxYearSA-PAYE").isEmpty)
      }

    }
  }
}
