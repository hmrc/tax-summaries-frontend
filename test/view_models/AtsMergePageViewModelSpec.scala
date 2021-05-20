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
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.domain.{SaUtr, Uar}
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestConstants.{testUar, testUtr}

class AtsMergePageViewModelSpec extends UnitSpec with GuiceOneAppPerSuite {
  implicit val appConfig: ApplicationConfig = fakeApplication.injector.instanceOf[ApplicationConfig]
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
      val model = AtsMergePageViewModel(AtsList("", "", "", List(1)), List.empty, appConfig)
      model.showSaYearList shouldBe true
    }

    "set showSaYearList flag to false if saData is not present " in {
      val model = AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, appConfig)
      model.showSaYearList shouldBe false
    }

    "set showPayeYearList flag to true if payeTaxYearList is present " in {
      val model = AtsMergePageViewModel(AtsList("", "", "", List.empty), List(1), appConfig)
      model.showPayeYearList shouldBe true
    }

    "set showPayeYearList flag to false if payeTaxYearList is not present " in {
      val model = AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, appConfig)
      model.showPayeYearList shouldBe false
    }

    "fill noAtsTaxYearList with the last 5 years if there is no paye or ats data " in {
      val model = AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, appConfig)
      model.noAtsTaxYearList shouldBe List(2015, 2016, 2017, 2018, 2019, 2020)
    }

    "noAtsTaxYearList should not contain data present in saData" in {
      val saYear = 2015
      val model = AtsMergePageViewModel(AtsList("", "", "", List(saYear)), List.empty, appConfig)
      model.noAtsTaxYearList shouldBe List(2016, 2017, 2018, 2019, 2020)
      model.noAtsTaxYearList shouldNot contain(saYear)
    }

    "noAtsTaxYearList should not contain data present in payeList" in {
      val payeYear = 2015
      val model = AtsMergePageViewModel(AtsList("", "", "", List.empty), List(payeYear), appConfig)
      model.noAtsTaxYearList shouldBe List(2016, 2017, 2018, 2019, 2020)
      model.noAtsTaxYearList shouldNot contain(payeYear)
    }

    "set showNoAts to true if not all years from 2018 are present in sa or paye data" in {
      val model = AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, appConfig)
      model.showNoAtsText shouldBe true
    }

    "set showNoAts to false if all years from 2018 are present in sa or paye data" in {
      val model = AtsMergePageViewModel(AtsList("", "", "", List.empty), List(2018, 2019, 2020), appConfig)
      model.showNoAtsText shouldBe true
    }

    "noAtsYearListAvailable should only return years past 2018 if no sa or paye data present" in {
      val model = AtsMergePageViewModel(AtsList("", "", "", List.empty), List.empty, appConfig)
      model.noAtsYearListAvailable shouldBe List(2019, 2020)
    }

    "set showIvUpliftLink to true if paye data is present and confidence level is below 200" in {
      val model = AtsMergePageViewModel(AtsList("", "", "", List.empty), List(2020), appConfig)
      model.showIvUpliftLink shouldBe true
    }

    "set showIvUpliftLink to false if paye data is present and confidence level is 200" in {
      val req = AuthenticatedRequest(
        "userId",
        Some(Uar(testUar)),
        Some(SaUtr(testUtr)),
        None,
        true,
        ConfidenceLevel.L200,
        fakeCredentials,
        FakeRequest("Get", s"?taxYear=$taxYear"))
      val model = AtsMergePageViewModel(AtsList("", "", "", List.empty), List(2020), appConfig)(request = req)
      model.showIvUpliftLink shouldBe false
    }
  }
}
