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

package utils

import config.ApplicationConfig
import controllers.auth.requests
import controllers.auth.requests.AuthenticatedRequest
import models.MissingTaxYear
import org.mockito.Mockito.when

import org.scalatest.wordspec.AnyWordSpec
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.SaUtr
import utils.TestConstants._

class TaxYearUtilSpec extends BaseSpec {
  private val mockAppConfig = mock[ApplicationConfig]
  private val taxYearUtil   = new TaxYearUtil(mockAppConfig)

  override def beforeEach(): Unit =
    super.beforeEach()

  "isValidTaxYear" must {
    "return true for current year" in {
      when(mockAppConfig.taxYear).thenReturn(currentTaxYear)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(4)
      taxYearUtil.isValidTaxYear(currentTaxYear) mustBe true
    }
    "return true for earliest year" in {
      when(mockAppConfig.taxYear).thenReturn(currentTaxYear)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(4)
      taxYearUtil.isValidTaxYear(currentTaxYear - 3) mustBe true
    }
    "return false for earliest year - 1" in {
      when(mockAppConfig.taxYear).thenReturn(currentTaxYear)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(4)
      taxYearUtil.isValidTaxYear(currentTaxYear - 4) mustBe false
    }
    "return false for current year + 1" in {
      when(mockAppConfig.taxYear).thenReturn(currentTaxYear)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(4)
      taxYearUtil.isValidTaxYear(currentTaxYear + 1) mustBe false
    }
  }

  "isYearListComplete" must {
    "return false when list empty" in {
      when(mockAppConfig.taxYear).thenReturn(currentTaxYear)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(4)
      taxYearUtil.isYearListComplete(Nil) mustBe false
    }
    "return true when all years present" in {
      when(mockAppConfig.taxYear).thenReturn(currentTaxYear)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(4)
      taxYearUtil.isYearListComplete(
        Seq(
          currentTaxYear - 3,
          currentTaxYear - 2,
          currentTaxYear - 1,
          currentTaxYear
        )
      ) mustBe true
    }
    "return false when first year missing" in {
      when(mockAppConfig.taxYear).thenReturn(currentTaxYear)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(4)
      taxYearUtil.isYearListComplete(
        Seq(currentTaxYear - 2, currentTaxYear - 1, currentTaxYear)
      ) mustBe false
    }
    "return false when first year missing but correct number of years due to earlier year present" in {
      when(mockAppConfig.taxYear).thenReturn(currentTaxYear)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(4)
      taxYearUtil.isYearListComplete(
        Seq(
          currentTaxYear - 4,
          currentTaxYear - 2,
          currentTaxYear - 1,
          currentTaxYear
        )
      ) mustBe false
    }
    "return false when first year missing but correct number of years due to later year present" in {
      when(mockAppConfig.taxYear).thenReturn(currentTaxYear)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(4)
      taxYearUtil.isYearListComplete(
        Seq(
          currentTaxYear - 3,
          currentTaxYear - 2,
          currentTaxYear - 1,
          currentTaxYear + 1
        )
      ) mustBe false
    }

    "return false when last year missing" in {
      when(mockAppConfig.taxYear).thenReturn(currentTaxYear)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(4)
      taxYearUtil.isYearListComplete(
        Seq(currentTaxYear - 3, currentTaxYear - 2, currentTaxYear - 1)
      ) mustBe false
    }
    "return false when last year missing but correct number of years due to earlier year present" in {
      when(mockAppConfig.taxYear).thenReturn(currentTaxYear)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(4)
      taxYearUtil.isYearListComplete(
        Seq(
          currentTaxYear - 4,
          currentTaxYear - 3,
          currentTaxYear - 2,
          currentTaxYear - 1
        )
      ) mustBe false
    }
    "return false when last year missing but correct number of years due to later year present" in {
      when(mockAppConfig.taxYear).thenReturn(currentTaxYear)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(4)
      taxYearUtil.isYearListComplete(
        Seq(
          currentTaxYear - 3,
          currentTaxYear - 2,
          currentTaxYear - 1,
          currentTaxYear + 1
        )
      ) mustBe false
    }
    "return false when year missing in middle of list" in {
      when(mockAppConfig.taxYear).thenReturn(currentTaxYear)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(4)
      taxYearUtil.isYearListComplete(
        Seq(currentTaxYear - 3, currentTaxYear - 1, currentTaxYear)
      ) mustBe false
    }
  }

  "taxYearUtil" must {
    "extract tax year when a valid tax year is present" in {
      implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] = requests.AuthenticatedRequest(
        "userId",
        None,
        Some(SaUtr(testUtr)),
        None,
        false,
        ConfidenceLevel.L50,
        fakeCredentials,
        FakeRequest("GET", s"?taxYear=$taxYear")
      )

      val result = taxYearUtil.extractTaxYear

      result mustBe Right(taxYear)

    }

    "return a MissingTaxYear response" when {

      " taxYear is more than 4 digits long " in {

        implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] = requests.AuthenticatedRequest(
          "userId",
          None,
          Some(SaUtr(testUtr)),
          None,
          false,
          ConfidenceLevel.L50,
          fakeCredentials,
          FakeRequest("GET", s"?taxYear=${currentTaxYear}2")
        )

        val result = taxYearUtil.extractTaxYear

        result mustBe Left(MissingTaxYear)
      }

      " taxYear is less than 4 digits long " in {

        implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] = requests.AuthenticatedRequest(
          "userId",
          None,
          Some(SaUtr(testUtr)),
          None,
          false,
          ConfidenceLevel.L50,
          fakeCredentials,
          FakeRequest("GET", "?taxYear=201")
        )

        val result = taxYearUtil.extractTaxYear

        result mustBe Left(MissingTaxYear)
      }

      "request has no taxYear field " in {

        implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] =
          requests.AuthenticatedRequest(
            "userId",
            None,
            Some(SaUtr(testUtr)),
            None,
            false,
            ConfidenceLevel.L50,
            fakeCredentials,
            FakeRequest("GET", "?")
          )

        val result = taxYearUtil.extractTaxYear

        result mustBe Left(MissingTaxYear)

      }

      "taxYear is not numeric " in {

        implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] = requests.AuthenticatedRequest(
          "userId",
          None,
          Some(SaUtr(testUtr)),
          None,
          false,
          ConfidenceLevel.L50,
          fakeCredentials,
          FakeRequest("GET", "?taxYear=ABCD")
        )

        val result = taxYearUtil.extractTaxYear

        result mustBe Left(MissingTaxYear)
      }
    }
  }

}
