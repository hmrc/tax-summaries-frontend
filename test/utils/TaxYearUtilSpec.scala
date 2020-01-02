/*
 * Copyright 2020 HM Revenue & Customs
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

import controllers.auth.AuthenticatedRequest
import models.InvalidTaxYear
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestConstants._

class TaxYearUtilSpec extends UnitSpec {

  "TaxYearUtil" should {
    "extract tax year when a valid tax year is present" in {

      val taxYear = 2019

      implicit val request = AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest("GET", s"?taxYear=$taxYear"))

      val result = TaxYearUtil.extractTaxYear

      result shouldBe Right(taxYear)

    }

    "return an InvalidTaxYear response" when {

      " taxYear is more than 4 digits long " in {

        implicit val request = AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest("GET", "?taxYear=20192"))

        val result = TaxYearUtil.extractTaxYear

        result shouldBe Left(InvalidTaxYear)
      }

      " taxYear is less than 4 digits long " in {

        implicit val request = AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest("GET", "?taxYear=201"))

        val result = TaxYearUtil.extractTaxYear

        result shouldBe Left(InvalidTaxYear)
      }

      "request has no taxYear field " in {

        implicit val request = AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest("GET", "?"))

        val result = TaxYearUtil.extractTaxYear

        result shouldBe Left(InvalidTaxYear)

      }

      "taxYear is not numeric " in {

        implicit val request = AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest("GET", "?taxYear=ABCD"))

        val result = TaxYearUtil.extractTaxYear

        result shouldBe Left(InvalidTaxYear)
      }
    }
  }
}
