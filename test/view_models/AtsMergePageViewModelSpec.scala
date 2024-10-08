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

package view_models

import config.ApplicationConfig
import controllers.auth.requests
import controllers.auth.requests.AuthenticatedRequest
import models.{AtsYearChoice, NoATS, PAYE, SA}
import org.mockito.Mockito.{reset, when}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.domain.{SaUtr, Uar}
import utils.BaseSpec
import utils.TestConstants.{testUar, testUtr}

class AtsMergePageViewModelSpec extends BaseSpec with GuiceOneAppPerSuite {
  val fakeCredentials: Credentials     = new Credentials("provider ID", "provider type")
  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]

  implicit val agentRequest: AuthenticatedRequest[AnyContentAsEmpty.type] = requests.AuthenticatedRequest(
    userId = "userId",
    agentRef = Some(Uar(testUar)),
    saUtr = Some(SaUtr(testUtr)),
    nino = None,
    isAgentActive = false,
    confidenceLevel = ConfidenceLevel.L50,
    credentials = fakeCredentials,
    request = FakeRequest("Get", s"?taxYear=$taxYear")
  )

  override def beforeEach(): Unit =
    reset(mockAppConfig)

  "AtsMergePageViewModel" must {

    "set showIvUpliftLink to true if paye data is present and confidence level is below 200" in {
      val model =
        AtsMergePageViewModel(AtsList("", "", "", List.empty), List(taxYear), mockAppConfig, ConfidenceLevel.L50)
      model.showIvUpliftLink mustBe true
    }

    "set showIvUpliftLink to false if paye data is not present and confidence level is below 200" in {
      val model = AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, mockAppConfig, ConfidenceLevel.L50)
      model.showIvUpliftLink mustBe false
    }

    "set showIvUpliftLink to false if paye data is present and confidence level is 200" in {
      val model =
        AtsMergePageViewModel(AtsList("", "", "", List.empty), List(taxYear), mockAppConfig, ConfidenceLevel.L200)
      model.showIvUpliftLink mustBe false
    }

    "set completeYearList to contain all the years sorted and with correct SA types when showIVUplift is false" in {
      when(mockAppConfig.taxYear).thenReturn(taxYear)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(4)

      val model =
        AtsMergePageViewModel(AtsList("", "", "", List(taxYear - 2)), List(taxYear), appConfig, ConfidenceLevel.L200)
      model.completeYearList mustBe List(
        AtsYearChoice(PAYE, taxYear),
        AtsYearChoice(NoATS, taxYear - 1),
        AtsYearChoice(SA, taxYear - 2),
        AtsYearChoice(NoATS, taxYear - 3)
      )

    }

    "set completeYearList to contain all the years sorted and with correct SA types when showIVUplift is true" in {
      when(mockAppConfig.taxYear).thenReturn(taxYear)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(4)

      val model =
        AtsMergePageViewModel(AtsList("", "", "", List(taxYear - 2)), List(taxYear), appConfig, ConfidenceLevel.L50)
      model.completeYearList mustBe List(
        AtsYearChoice(NoATS, taxYear - 1),
        AtsYearChoice(SA, taxYear - 2),
        AtsYearChoice(NoATS, taxYear - 3)
      )

    }

    "set showContinueButton to true when showSaYearList is true" in {
      val model =
        AtsMergePageViewModel(AtsList("", "", "", List(taxYear - 2)), List.empty, appConfig, ConfidenceLevel.L200)
      model.showContinueButton mustBe true
    }

    "set showContinueButton to true when showNoAtsYearList is true" in {
      when(mockAppConfig.taxYear).thenReturn(taxYear)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(4)
      val model = AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, appConfig, ConfidenceLevel.L200)
      model.showContinueButton mustBe true
    }

    "set showContinueButton to true when paye data is present and show IV uplift is false" in {
      when(mockAppConfig.taxYear).thenReturn(taxYear)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(0)
      val model =
        AtsMergePageViewModel(AtsList("", "", "", List.empty), List(taxYear - 1), mockAppConfig, ConfidenceLevel.L200)
      model.showContinueButton mustBe true
    }

    "set showContinueButton to false when there is no paye, sa or no ats data" in {
      when(mockAppConfig.taxYear).thenReturn(taxYear - 10)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(0)
      val model =
        AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, mockAppConfig, ConfidenceLevel.L200)
      model.showContinueButton mustBe false
    }

    "set showContinueButton to false when there is no sa or no ats data, there is paye data but the user needs iv uplift" in {
      when(mockAppConfig.taxYear).thenReturn(taxYear - 10)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(0)
      val model =
        AtsMergePageViewModel(AtsList("", "", "", List.empty), List(taxYear), mockAppConfig, ConfidenceLevel.L50)
      model.showContinueButton mustBe false
    }

    "set name to be name and surname from sa data" in {
      val model =
        AtsMergePageViewModel(AtsList("", "name", "surname", List(1)), List.empty, mockAppConfig, ConfidenceLevel.L200)
      model.name mustBe "name surname"
    }

  }
}
