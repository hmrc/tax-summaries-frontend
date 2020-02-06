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
import models.SpendData
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.FakeRequest
import services.atsData.AtsTestData
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestConstants._
import view_models.Amount
import view_models.paye.PayeGovernmentSpend

class PayeGovernmentSpendServiceSpec extends UnitSpec with GuiceOneAppPerSuite with ScalaFutures with MockitoSugar {

  class TestService extends PayeGovernmentSpendService with MockitoSugar {
    override lazy val atsService: PayeAtsService = mock[PayeAtsService]
    implicit val hc = new HeaderCarrier
    val request = AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest("GET","?taxYear=2015"))
    val taxYear = 2015
  }


  "GovernmentSpendService govSpend" should {

    "return a complete GovernmentSpend when given complete AtsData" in new TestService{
      val atsData = AtsTestData.govSpendingData
      val result = govSpend(atsData)

      result shouldBe PayeGovernmentSpend(
       2019,
        List("welfare" -> SpendData(Amount(100, "GBP"), 10)),
        "Mr",
        "John",
        "Smith",
        Amount(200,"GBP"),
        "",
        Amount(500,"GBP")
      )
    }

  }
}
