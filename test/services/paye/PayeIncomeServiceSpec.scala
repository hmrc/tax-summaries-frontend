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

package services.paye

import controllers.auth.AuthenticatedRequest
import models.AtsData
import org.scalatest.MustMatchers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.FakeRequest
import services.atsData.AtsTestData
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestConstants._
import view_models.Amount
import view_models.paye.PayeIncomeBeforeTax

class PayeIncomeServiceSpec extends UnitSpec with GuiceOneAppPerSuite with ScalaFutures with MockitoSugar {

  class TestService extends PayeIncomeService with MockitoSugar {
    override lazy val atsService: PayeAtsService = mock[PayeAtsService]
    implicit val hc = new HeaderCarrier
    val taxYear = 2015
    val request = AuthenticatedRequest("userId", None, None, Some(Nino(testNino)), None, None, None, FakeRequest("GET", s"?taxYear=$taxYear"))
  }

  "IncomeService.createIncomeConverter" should {

    "return complete IncomeBeforeTaxData when given complete AtsData" in new TestService {

      val incomeData: AtsData = AtsTestData.incomeData
      val result: PayeIncomeBeforeTax = createIncomeConverter(incomeData)
      result mustEqual PayeIncomeBeforeTax(
        2019,
        Amount(100, "GBP"),
        Amount(200, "GBP"),
        Amount(300, "GBP"),
        Amount(400, "GBP"),
        Amount(500, "GBP"),
        Amount(600, "GBP"),
        Amount(700, "GBP"),
        Amount(800, "GBP")
      )
    }


  }
}
