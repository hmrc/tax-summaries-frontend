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
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.FakeRequest
import services.atsData.AtsTestData
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestConstants._
import view_models._
import view_models.paye.PayeAllowances

class PayeAllowanceServiceSpec extends UnitSpec with GuiceOneAppPerSuite with ScalaFutures with MockitoSugar {

  val noAtsaViewModel: NoATSViewModel = new NoATSViewModel()

  class TestService extends PayeAllowanceService with MockitoSugar {
    override lazy val atsService: PayeAtsService = mock[PayeAtsService]
    implicit val hc = new HeaderCarrier
    val taxYear = 2015
    val request = AuthenticatedRequest("userId", None, None, Some(Nino(testNino)), None, None, None, FakeRequest("GET",s"?taxYear=$taxYear"))
  }

  "AllowanceService.allowanceDataConverter" should {
    "return a complete AllowancesData when given complete AtsData" in new TestService {

      val atsData = AtsTestData.atsAllowancesData
      val result = allowanceDataConverter(atsData)

      result shouldBe PayeAllowances(
        2019,
        Amount(100, "GBP"),
        Amount(200, "GBP"),
        Amount(300, "GBP"),
        Amount(500, "GBP"),
        Amount(400, "GBP"),
        Amount(600, "GBP")
      )
    }
  }
}
