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
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.domain.{SaUtr, Uar}
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestConstants.{testUar, testUtr}

class AtsMergePageViewModelSpec extends UnitSpec with GuiceOneAppPerSuite {
  implicit val appConfig = mock[ApplicationConfig]
  val fakeCredentials = new Credentials("provider ID", "provider type")
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

  "AtsMergePageViewModel" should {
    "set showSaYearList flag to true if saData is present " in {
      val model = AtsMergePageViewModel(AtsList("", "", "", List(1)), List.empty, appConfig, ConfidenceLevel.L200)
      model.showSaYearList shouldBe true
    }

    "set showSaYearList flag to false if saData is not present " in {
      val model = AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, appConfig, ConfidenceLevel.L200)
      model.showSaYearList shouldBe false
    }

    "set showPayeYearList flag to true if payeTaxYearList is present " in {
      val model = AtsMergePageViewModel(AtsList("", "", "", List.empty), List(1), appConfig, ConfidenceLevel.L200)
      model.showPayeYearList shouldBe true
    }

    "set showPayeYearList flag to false if payeTaxYearList is not present " in {
      val model = AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, appConfig, ConfidenceLevel.L200)
      model.showPayeYearList shouldBe false
    }

    "set showNoAts to true if not all years from 2018 are present in sa or paye data" in {
      val model = AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, appConfig, ConfidenceLevel.L200)
      model.showNoAtsText shouldBe true
    }

    "set showNoAts to false if all years from 2018 are present in sa or paye data" in {
      when(appConfig.taxYear).thenReturn(2020)
      when(appConfig.maxTaxYearsTobeDisplayed).thenReturn(2)
      val model =
        AtsMergePageViewModel(AtsList("", "", "", List.empty), List(2018, 2019, 2020), appConfig, ConfidenceLevel.L200)
      model.showNoAtsText shouldBe false
    }

    "set showIvUpliftLink to true if paye data is present and confidence level is below 200" in {
      val model = AtsMergePageViewModel(AtsList("", "", "", List.empty), List(2020), appConfig, ConfidenceLevel.L50)
      model.showIvUpliftLink shouldBe true
    }

    "set showIvUpliftLink to false if paye data is not present and confidence level is below 200" in {
      val model = AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, appConfig, ConfidenceLevel.L50)
      model.showIvUpliftLink shouldBe false
    }

    "set showIvUpliftLink to false if paye data is present and confidence level is 200" in {
      val model = AtsMergePageViewModel(AtsList("", "", "", List.empty), List(2020), appConfig, ConfidenceLevel.L200)
      model.showIvUpliftLink shouldBe false
    }

    "set completeYearList to contain all the years sorted and with correct SA types when showIVUplift is false" in {
      when(appConfig.taxYear).thenReturn(2020)
      when(appConfig.maxTaxYearsTobeDisplayed).thenReturn(5)
      val model = AtsMergePageViewModel(AtsList("", "", "", List(2018)), List(2020), appConfig, ConfidenceLevel.L200)
      model.completeYearList shouldBe List(
        AtsYearChoice(PAYE, 2020),
        AtsYearChoice(NoATS, 2019),
        AtsYearChoice(SA, 2018))
    }

    "set completeYearList to contain all the years sorted and with correct SA types when showIVUplift is true" in {
      when(appConfig.taxYear).thenReturn(2020)
      when(appConfig.maxTaxYearsTobeDisplayed).thenReturn(5)
      val model = AtsMergePageViewModel(AtsList("", "", "", List(2018)), List(2020), appConfig, ConfidenceLevel.L50)
      model.completeYearList shouldBe List(AtsYearChoice(NoATS, 2019), AtsYearChoice(SA, 2018))
    }

    "set showContinueButton to true when showSaYearList is true" in {
      val model = AtsMergePageViewModel(AtsList("", "", "", List(2018)), List.empty, appConfig, ConfidenceLevel.L200)
      model.showContinueButton shouldBe true
    }

    "set showContinueButton to true when showNoAtsYearList is true" in {
      when(appConfig.taxYear).thenReturn(2020)
      when(appConfig.maxTaxYearsTobeDisplayed).thenReturn(5)
      val model = AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, appConfig, ConfidenceLevel.L200)
      model.showContinueButton shouldBe true
    }

    "set showContinueButton to true when paye data is present and show IV uplift is false" in {
      when(appConfig.taxYear).thenReturn(2020)
      when(appConfig.maxTaxYearsTobeDisplayed).thenReturn(0)
      val model = AtsMergePageViewModel(AtsList("", "", "", List.empty), List(2019), appConfig, ConfidenceLevel.L200)
      model.showContinueButton shouldBe true
    }

    "set showContinueButton to false when there is no paye, sa or no ats data" in {
      when(appConfig.taxYear).thenReturn(2018)
      when(appConfig.maxTaxYearsTobeDisplayed).thenReturn(0)
      val model = AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, appConfig, ConfidenceLevel.L200)
      model.showContinueButton shouldBe false
    }

    "set showContinueButton to false when there is no sa or no ats data, there is paye data but the user needs iv uplift" in {
      when(appConfig.taxYear).thenReturn(2018)
      when(appConfig.maxTaxYearsTobeDisplayed).thenReturn(0)
      val model = AtsMergePageViewModel(AtsList("", "", "", List.empty), List(2020), appConfig, ConfidenceLevel.L50)
      model.showContinueButton shouldBe false
    }

    "set name to be name and surname from sa data" in {
      val model =
        AtsMergePageViewModel(AtsList("", "name", "surname", List(1)), List.empty, appConfig, ConfidenceLevel.L200)
      model.name shouldBe "name surname"
    }

  }
}
