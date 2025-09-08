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
import utils.TestConstants.*

class TaxYearUtilSpec extends BaseSpec {
  private val mockAppConfig = mock[ApplicationConfig]
  private val taxYearUtil   = new TaxYearUtil(mockAppConfig)

  // Override so that both tax years are always the same, which is required for these tests.
  override protected val currentTaxYearPAYE: Int = currentTaxYearSA
  private val maxTaxYearsTobeDisplayed: Int      = 4

  override def beforeEach(): Unit =
    super.beforeEach()

  "isValidTaxYear" when {
    "SA and PAYE tax years are the same" must {
      "return true for current year" in {
        when(mockAppConfig.taxYearSA).thenReturn(currentTaxYearSA)
        when(mockAppConfig.taxYearPAYE).thenReturn(currentTaxYearPAYE)
        when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(maxTaxYearsTobeDisplayed)
        taxYearUtil.isValidTaxYear(currentTaxYearSA) mustBe true
      }
      "return true for earliest year" in {
        when(mockAppConfig.taxYearSA).thenReturn(currentTaxYearSA)
        when(mockAppConfig.taxYearPAYE).thenReturn(currentTaxYearPAYE)
        when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(maxTaxYearsTobeDisplayed)
        taxYearUtil.isValidTaxYear(currentTaxYearSA - 3) mustBe true
      }
      "return false for earliest year - 1" in {
        when(mockAppConfig.taxYearSA).thenReturn(currentTaxYearSA)
        when(mockAppConfig.taxYearPAYE).thenReturn(currentTaxYearPAYE)
        when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(maxTaxYearsTobeDisplayed)
        taxYearUtil.isValidTaxYear(currentTaxYearSA - 4) mustBe false
      }
      "return false for current year + 1" in {
        when(mockAppConfig.taxYearSA).thenReturn(currentTaxYearSA)
        when(mockAppConfig.taxYearPAYE).thenReturn(currentTaxYearPAYE)
        when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(maxTaxYearsTobeDisplayed)
        taxYearUtil.isValidTaxYear(currentTaxYearSA + 1) mustBe false
      }
    }

    "sa tax year is paye tax year + 1" must {
      val currentTaxYearSA = currentTaxYearPAYE + 1
      "return true for current sa year" in {
        when(mockAppConfig.taxYearSA).thenReturn(currentTaxYearSA)
        when(mockAppConfig.taxYearPAYE).thenReturn(currentTaxYearPAYE)
        when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(maxTaxYearsTobeDisplayed)
        taxYearUtil.isValidTaxYear(currentTaxYearSA) mustBe true
      }
      "return true for current paye year" in {
        when(mockAppConfig.taxYearSA).thenReturn(currentTaxYearSA)
        when(mockAppConfig.taxYearPAYE).thenReturn(currentTaxYearPAYE)
        when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(maxTaxYearsTobeDisplayed)
        taxYearUtil.isValidTaxYear(currentTaxYearPAYE) mustBe true
      }
      "return true for earliest year" in {
        when(mockAppConfig.taxYearSA).thenReturn(currentTaxYearSA)
        when(mockAppConfig.taxYearPAYE).thenReturn(currentTaxYearPAYE)
        when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(maxTaxYearsTobeDisplayed)
        taxYearUtil.isValidTaxYear(currentTaxYearPAYE - 3) mustBe true
      }
      "return false for earliest year - 1" in {
        when(mockAppConfig.taxYearSA).thenReturn(currentTaxYearSA)
        when(mockAppConfig.taxYearPAYE).thenReturn(currentTaxYearPAYE)
        when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(maxTaxYearsTobeDisplayed)
        taxYearUtil.isValidTaxYear(currentTaxYearPAYE - 4) mustBe false
      }
      "return false for current sa year + 1" in {
        when(mockAppConfig.taxYearSA).thenReturn(currentTaxYearSA)
        when(mockAppConfig.taxYearPAYE).thenReturn(currentTaxYearPAYE)
        when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(maxTaxYearsTobeDisplayed)
        taxYearUtil.isValidTaxYear(currentTaxYearSA + 1) mustBe false
      }
    }

    "paye tax year is sa tax year + 1" must {
      val currentTaxYearPAYE = currentTaxYearSA + 1
      "return true for current paye year" in {
        when(mockAppConfig.taxYearSA).thenReturn(currentTaxYearSA)
        when(mockAppConfig.taxYearPAYE).thenReturn(currentTaxYearPAYE)
        when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(maxTaxYearsTobeDisplayed)
        taxYearUtil.isValidTaxYear(currentTaxYearPAYE) mustBe true
      }
      "return true for current sa year" in {
        when(mockAppConfig.taxYearSA).thenReturn(currentTaxYearSA)
        when(mockAppConfig.taxYearPAYE).thenReturn(currentTaxYearPAYE)
        when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(maxTaxYearsTobeDisplayed)
        taxYearUtil.isValidTaxYear(currentTaxYearSA) mustBe true
      }
      "return true for earliest year" in {
        when(mockAppConfig.taxYearSA).thenReturn(currentTaxYearSA)
        when(mockAppConfig.taxYearPAYE).thenReturn(currentTaxYearPAYE)
        when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(maxTaxYearsTobeDisplayed)
        taxYearUtil.isValidTaxYear(currentTaxYearSA - 3) mustBe true
      }
      "return false for earliest year - 1" in {
        when(mockAppConfig.taxYearSA).thenReturn(currentTaxYearSA)
        when(mockAppConfig.taxYearPAYE).thenReturn(currentTaxYearPAYE)
        when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(maxTaxYearsTobeDisplayed)
        taxYearUtil.isValidTaxYear(currentTaxYearSA - 4) mustBe false
      }
      "return false for current sa year + 1" in {
        when(mockAppConfig.taxYearSA).thenReturn(currentTaxYearSA)
        when(mockAppConfig.taxYearPAYE).thenReturn(currentTaxYearPAYE)
        when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(maxTaxYearsTobeDisplayed)
        taxYearUtil.isValidTaxYear(currentTaxYearPAYE + 1) mustBe false
      }
    }

  }

  def validCompleteTaxYear(currentTaxYearSA: Int, currentTaxYearPAYE: Int): Unit = {

    val fullYearList = allYears(currentTaxYearSA, currentTaxYearPAYE)
    "return false when list empty" in {
      when(mockAppConfig.taxYearSA).thenReturn(currentTaxYearSA)
      when(mockAppConfig.taxYearPAYE).thenReturn(currentTaxYearPAYE)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(maxTaxYearsTobeDisplayed)
      taxYearUtil.isYearListComplete(Nil) mustBe false
    }
    "return true when all years present" in {
      when(mockAppConfig.taxYearSA).thenReturn(currentTaxYearSA)
      when(mockAppConfig.taxYearPAYE).thenReturn(currentTaxYearPAYE)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(maxTaxYearsTobeDisplayed)
      taxYearUtil.isYearListComplete(fullYearList) mustBe true
    }
    "return false when first year missing" in {
      when(mockAppConfig.taxYearSA).thenReturn(currentTaxYearSA)
      when(mockAppConfig.taxYearPAYE).thenReturn(currentTaxYearPAYE)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(maxTaxYearsTobeDisplayed)
      taxYearUtil.isYearListComplete(
        fullYearList.tail
      ) mustBe false
    }
    "return false when first year missing but correct number of years due to earlier year present" in {
      when(mockAppConfig.taxYearSA).thenReturn(currentTaxYearSA)
      when(mockAppConfig.taxYearPAYE).thenReturn(currentTaxYearPAYE)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(maxTaxYearsTobeDisplayed)
      taxYearUtil.isYearListComplete(
        Seq(currentTaxYearSA - 6) ++ fullYearList.tail
      ) mustBe false
    }
    "return false when first year missing but correct number of years due to later year present" in {
      when(mockAppConfig.taxYearSA).thenReturn(currentTaxYearSA)
      when(mockAppConfig.taxYearPAYE).thenReturn(currentTaxYearPAYE)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(maxTaxYearsTobeDisplayed)
      taxYearUtil.isYearListComplete(
        Seq(currentTaxYearSA + 2) ++ fullYearList.tail
      ) mustBe false
    }

    "return false when last year missing" in {
      when(mockAppConfig.taxYearSA).thenReturn(currentTaxYearSA)
      when(mockAppConfig.taxYearPAYE).thenReturn(currentTaxYearPAYE)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(maxTaxYearsTobeDisplayed)
      taxYearUtil.isYearListComplete(
        fullYearList.reverse.tail.reverse
      ) mustBe false
    }
    "return false when last year missing but correct number of years due to earlier year present" in {
      when(mockAppConfig.taxYearSA).thenReturn(currentTaxYearSA)
      when(mockAppConfig.taxYearPAYE).thenReturn(currentTaxYearPAYE)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(maxTaxYearsTobeDisplayed)
      taxYearUtil.isYearListComplete(
        Seq(currentTaxYearSA - 6) ++ fullYearList.reverse.tail.reverse
      ) mustBe false
    }
    "return false when last year missing but correct number of years due to later year present" in {
      when(mockAppConfig.taxYearSA).thenReturn(currentTaxYearSA)
      when(mockAppConfig.taxYearPAYE).thenReturn(currentTaxYearPAYE)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(maxTaxYearsTobeDisplayed)
      taxYearUtil.isYearListComplete(
        Seq(currentTaxYearSA + 2) ++ fullYearList.reverse.tail.reverse
      ) mustBe false
    }
    "return false when year missing in middle of list" in {
      when(mockAppConfig.taxYearSA).thenReturn(currentTaxYearSA)
      when(mockAppConfig.taxYearPAYE).thenReturn(currentTaxYearPAYE)
      when(mockAppConfig.maxTaxYearsTobeDisplayed).thenReturn(maxTaxYearsTobeDisplayed)
      taxYearUtil.isYearListComplete(
        Seq(fullYearList.head) ++ fullYearList.tail.tail
      ) mustBe false
    }
  }

  "isYearListComplete" when {
    "SA and PAYE tax years are the same" must {
      behave like validCompleteTaxYear(currentTaxYearSA, currentTaxYearPAYE)
    }

    "SA tax year is PAYE tax year + 1" must {
      behave like validCompleteTaxYear(currentTaxYearSA + currentTaxYearPAYE + 1, currentTaxYearPAYE)
    }

    "PAYE tax year is SA tax year + 1" must {
      behave like validCompleteTaxYear(currentTaxYearSA, currentTaxYearSA + 1)
    }
  }

  "extractTaxYear" must {
    "extract tax year when a valid tax year is present" in {
      implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] = requests.AuthenticatedRequest(
        "userId",
        None,
        Some(SaUtr(testUtr)),
        None,
        false,
        ConfidenceLevel.L50,
        fakeCredentials,
        FakeRequest("GET", s"?taxYear=$currentTaxYearSA")
      )

      val result = taxYearUtil.extractTaxYear

      result mustBe Right(currentTaxYearSA)

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
          FakeRequest("GET", s"?taxYear=${currentTaxYearSA}2")
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
