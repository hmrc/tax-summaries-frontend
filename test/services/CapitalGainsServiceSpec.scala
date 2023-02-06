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

package services

import controllers.auth.AuthenticatedRequest
import models.AtsData
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.MockitoSugar
import play.api.test.FakeRequest
import services.atsData.AtsTestData
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestConstants._
import utils.{BaseSpec, GenericViewModel}
import view_models._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class CapitalGainsServiceSpec extends BaseSpec {

  val genericViewModel: GenericViewModel = AtsList(
    utr = "3000024376",
    forename = "forename",
    surname = "surname",
    yearList = List(2015)
  )

  implicit val hc = new HeaderCarrier

  val mockAtsService   = mock[AtsService]
  override val taxYear = 2015
  val request          = AuthenticatedRequest(
    "userId",
    None,
    Some(SaUtr(testUtr)),
    None,
    true,
    false,
    ConfidenceLevel.L50,
    fakeCredentials,
    FakeRequest("GET", s"?taxYear=$taxYear")
  )

  val sut = new CapitalGainsService(mockAtsService) with MockitoSugar

  "CapitalGainsService getCapitalGains" must {

    "return a GenericViewModel when TaxYearUtil.extractTaxYear returns a taxYear" in {
      when(
        mockAtsService.createModel(meq(taxYear), any[Function1[AtsData, GenericViewModel]]())(
          any(),
          any()
        )
      ).thenReturn(Future(genericViewModel))
      val result = Await.result(sut.getCapitalGains(taxYear)(hc, request), 1500 millis)
      result mustEqual genericViewModel
    }
  }

  "CapitalGainsService capitalGains" must {

    "return a complete CapitalGains when given complete AtsData" in {
      val atsData = AtsTestData.capitalGainsData
      val result  = sut.capitalGains(atsData)

      result mustBe CapitalGains(
        2019,
        "1111111111",
        Amount.gbp(100),
        Amount.gbp(-200),
        Amount.gbp(300),
        Amount.gbp(400),
        Amount.gbp(500),
        Amount.gbp(600),
        Amount.gbp(700),
        Amount.gbp(800),
        Amount.gbp(900),
        Amount.gbp(1000),
        Amount.gbp(1100),
        Amount.gbp(1200),
        Amount.gbp(1300),
        Amount.gbp(1400),
        Amount.gbp(1500),
        Amount.gbp(1600),
        Rate("10%"),
        Rate("20%"),
        Rate("30%"),
        Rate("40%"),
        Rate("50%"),
        Rate("60%"),
        "Mr",
        "John",
        "Smith"
      )
    }
  }
}
