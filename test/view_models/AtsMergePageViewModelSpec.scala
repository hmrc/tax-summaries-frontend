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

package view_models

import config.ApplicationConfig
import controllers.auth.AuthenticatedRequest
import models.{AtsYearChoice, NoATS, PAYE, SA}
import org.mockito.Mockito.{reset, when}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.domain.{SaUtr, Uar}
import utils.BaseSpec
import utils.TestConstants.{testUar, testUtr}

class AtsMergePageViewModelSpec extends BaseSpec with GuiceOneAppPerSuite {
  val fakeCredentials = new Credentials("provider ID", "provider type")
  val mockAppConfig = mock[ApplicationConfig]
  override val taxYear = 2015

  implicit val agentRequest = AuthenticatedRequest(
    "userId",
    Some(Uar(testUar)),
    Some(SaUtr(testUtr)),
    None,
    true,
    false,
    ConfidenceLevel.L50,
    fakeCredentials,
    FakeRequest("Get", s"?taxYear=$taxYear"))

  override def beforeEach() =
    reset(mockAppConfig)

  "AtsMergePageViewModel" must {

    "set showNoAts to true if not all years from 2018 are present in sa or paye data" in {
      val model =
        AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, mockAppConfig, ConfidenceLevel.L200)
      model.showNoAtsText mustBe true
    }

    "set showNoAts to false if all years from 2018 are present in sa or paye data" in {
      when(mockAppConfig.taxYear).thenReturn(2020)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(2)
      val model =
        AtsMergePageViewModel(
          AtsList("", "", "", List.empty),
          List(2018, 2019, 2020),
          mockAppConfig,
          ConfidenceLevel.L200)
      model.showNoAtsText mustBe false
    }

    "set showIvUpliftLink to true if paye data is present and confidence level is below 200" in {
      val model = AtsMergePageViewModel(AtsList("", "", "", List.empty), List(2020), mockAppConfig, ConfidenceLevel.L50)
      model.showIvUpliftLink mustBe true
    }

    "set showIvUpliftLink to false if paye data is not present and confidence level is below 200" in {
      val model = AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, mockAppConfig, ConfidenceLevel.L50)
      model.showIvUpliftLink mustBe false
    }

    "set showIvUpliftLink to false if paye data is present and confidence level is 200" in {
      val model =
        AtsMergePageViewModel(AtsList("", "", "", List.empty), List(2020), mockAppConfig, ConfidenceLevel.L200)
      model.showIvUpliftLink mustBe false
    }

    "set completeYearList to contain all the years sorted and with correct SA types when showIVUplift is false" in {
      when(mockAppConfig.taxYear).thenReturn(2021)
      when(mockAppConfig.currentTaxYearSpendData).thenReturn(true)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(5)
      val model =
        AtsMergePageViewModel(AtsList("", "", "", List(2018)), List(2020), mockAppConfig, ConfidenceLevel.L200)
      println(model.completeYearList)
      model.completeYearList mustBe List(
        AtsYearChoice(NoATS, mockAppConfig.taxYear),
        AtsYearChoice(PAYE, 2020),
        AtsYearChoice(NoATS, 2019),
        AtsYearChoice(SA, 2018))
    }

    "set completeYearList to contain all the years sorted and with correct SA types when showIVUplift is false but without 2021 data if currentTaxYearSpendData toggle is false" in {
      when(mockAppConfig.taxYear).thenReturn(2021)
      when(mockAppConfig.currentTaxYearSpendData).thenReturn(false)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(5)
      val model =
        AtsMergePageViewModel(AtsList("", "", "", List(2018)), List(2020), mockAppConfig, ConfidenceLevel.L200)
      println(model.completeYearList)
      model.completeYearList mustBe List(AtsYearChoice(PAYE, 2020), AtsYearChoice(NoATS, 2019), AtsYearChoice(SA, 2018))
    }

    "set completeYearList to contain all the years sorted and with correct SA types when showIVUplift is true" in {
      when(mockAppConfig.taxYear).thenReturn(2021)
      when(mockAppConfig.currentTaxYearSpendData).thenReturn(true)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(5)
      val model = AtsMergePageViewModel(AtsList("", "", "", List(2018)), List(2020), mockAppConfig, ConfidenceLevel.L50)
      println(model.completeYearList)
      model.completeYearList mustBe List(
        AtsYearChoice(NoATS, mockAppConfig.taxYear),
        AtsYearChoice(NoATS, 2019),
        AtsYearChoice(SA, 2018))
    }

    "set completeYearList to contain all the years sorted and with correct SA types when showIVUplift is true but without 2021 data if currentTaxYearSpendData toggle is false" in {
      when(mockAppConfig.taxYear).thenReturn(2021)
      when(mockAppConfig.currentTaxYearSpendData).thenReturn(false)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(5)
      val model = AtsMergePageViewModel(AtsList("", "", "", List(2018)), List(2020), mockAppConfig, ConfidenceLevel.L50)
      println(model.completeYearList)
      model.completeYearList mustBe List(AtsYearChoice(NoATS, 2019), AtsYearChoice(SA, 2018))
    }

    "set showContinueButton to true when showSaYearList is true" in {
      val model = AtsMergePageViewModel(AtsList("", "", "", List(2018)), List.empty, appConfig, ConfidenceLevel.L200)
      model.showContinueButton mustBe true
    }

    "set showContinueButton to true when showNoAtsYearList is true" in {
      when(mockAppConfig.taxYear).thenReturn(2020)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(5)
      val model = AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, appConfig, ConfidenceLevel.L200)
      model.showContinueButton mustBe true
    }

    "set showContinueButton to true when paye data is present and show IV uplift is false" in {
      when(mockAppConfig.taxYear).thenReturn(2020)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(0)
      val model =
        AtsMergePageViewModel(AtsList("", "", "", List.empty), List(2019), mockAppConfig, ConfidenceLevel.L200)
      model.showContinueButton mustBe true
    }

    "set showContinueButton to false when there is no paye, sa or no ats data" in {
      when(mockAppConfig.taxYear).thenReturn(2018)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(0)
      val model =
        AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, mockAppConfig, ConfidenceLevel.L200)
      model.showContinueButton mustBe false
    }

    "set showContinueButton to false when there is no sa or no ats data, there is paye data but the user needs iv uplift" in {
      when(mockAppConfig.taxYear).thenReturn(2018)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(0)
      val model = AtsMergePageViewModel(AtsList("", "", "", List.empty), List(2020), mockAppConfig, ConfidenceLevel.L50)
      model.showContinueButton mustBe false
    }

    "set name to be name and surname from sa data" in {
      val model =
        AtsMergePageViewModel(AtsList("", "name", "surname", List(1)), List.empty, mockAppConfig, ConfidenceLevel.L200)
      model.name mustBe "name surname"
    }

  }
}
