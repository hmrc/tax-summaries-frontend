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

import controllers.auth.requests
import controllers.auth.requests.AuthenticatedRequest
import models.InvalidTaxYear
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.SaUtr
import utils.TestConstants._

class TaxYearUtilSpec extends AnyWordSpec with Matchers {

  val authenticatedRequest: Unit =
    "TaxYearUtil" must {
      "extract tax year when a valid tax year is present" in {

        val taxYear = 2022

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

        val result = TaxYearUtil.extractTaxYear

        result mustBe Right(taxYear)

      }

      "return an InvalidTaxYear response" when {

        " taxYear is more than 4 digits long " in {

          implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] = requests.AuthenticatedRequest(
            "userId",
            None,
            Some(SaUtr(testUtr)),
            None,
            false,
            ConfidenceLevel.L50,
            fakeCredentials,
            FakeRequest("GET", "?taxYear=20192")
          )

          val result = TaxYearUtil.extractTaxYear

          result mustBe Left(InvalidTaxYear)
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

          val result = TaxYearUtil.extractTaxYear

          result mustBe Left(InvalidTaxYear)
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

          val result = TaxYearUtil.extractTaxYear

          result mustBe Left(InvalidTaxYear)

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

          val result = TaxYearUtil.extractTaxYear

          result mustBe Left(InvalidTaxYear)
        }
      }
    }
}
