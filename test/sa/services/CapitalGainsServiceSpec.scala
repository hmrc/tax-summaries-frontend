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

package sa.services

import common.models.requests
import common.models.requests.AuthenticatedRequest
import common.utils.TestConstants.*
import common.utils.{BaseSpec, GenericViewModel}
import common.view_models.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import sa.utils.AtsTestData
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class CapitalGainsServiceSpec extends BaseSpec {

  val genericViewModel: GenericViewModel = AtsList(
    utr = "3000024376",
    forename = "forename",
    surname = "surname",
    yearList = List(currentTaxYearSA)
  )

  implicit val hc: HeaderCarrier = new HeaderCarrier

  val mockAtsService: AtsService                            = mock[AtsService]
  val request: AuthenticatedRequest[AnyContentAsEmpty.type] = requests.AuthenticatedRequest(
    userId = "userId",
    agentRef = None,
    saUtr = Some(SaUtr(testUtr)),
    nino = None,
    isAgentActive = false,
    confidenceLevel = ConfidenceLevel.L50,
    credentials = fakeCredentials,
    request = FakeRequest("GET", s"?taxYear=$currentTaxYearSA")
  )

  val sut = new CapitalGainsService(mockAtsService)

  "CapitalGainsService getCapitalGains" must {

    "return a GenericViewModel when TaxYearUtil.extractTaxYear returns a taxYear" in {
      when(
        mockAtsService.createModel(any(), any())(
          any(),
          any()
        )
      ).thenReturn(Future(genericViewModel))
      val result = Await.result(sut.getCapitalGains(currentTaxYearSA)(hc, request), 1500 millis)
      result mustEqual genericViewModel
    }
  }

  "CapitalGainsService capitalGains" must {

    "return a complete CapitalGains when given complete AtsData" in {
      val atsData = AtsTestData.capitalGainsData
      val result  = sut.capitalGains(atsData)

      result mustBe CapitalGains(
        taxYear = currentTaxYearSA,
        utr = "1111111111",
        taxableGains = Amount.gbp(100),
        lessTaxFreeAmount = Amount.gbp(-200),
        payCgTaxOn = Amount.gbp(300),
        entrepreneursReliefRateBefore = Amount.gbp(400),
        entrepreneursReliefRateAmount = Amount.gbp(500),
        ordinaryRateBefore = Amount.gbp(600),
        ordinaryRateAmount = Amount.gbp(700),
        upperRateBefore = Amount.gbp(800),
        upperRateAmount = Amount.gbp(900),
        rpciLowerTax = Amount.gbp(1000),
        rpciLowerTotalAmount = Amount.gbp(1100),
        rpciHigherTax = Amount.gbp(1200),
        rpciHigherTotalAmount = Amount.gbp(1300),
        ciLowerTax = Amount.gbp(1310),
        ciLowerTotalAmount = Amount.gbp(1320),
        ciHigherTax = Amount.gbp(1330),
        ciHigherTotalAmount = Amount.gbp(1340),
        rpLowerTax = Amount.gbp(1350),
        rpLowerTotalAmount = Amount.gbp(1360),
        rpHigherTax = Amount.gbp(1370),
        rpHigherTotalAmount = Amount.gbp(1380),
        adjustmentsAmount = Amount.gbp(1400),
        totalCapitalGainsTaxAmount = Amount.gbp(1500),
        cgTaxPerCurrencyUnit = Amount.gbp(1600),
        entrepreneursReliefRateRate = Rate("10%"),
        ordinaryRateRate = Rate("20%"),
        upperRateRate = Rate("30%"),
        rpciLowerRate = Rate("40%"),
        rpciHigherRate = Rate("50%"),
        ciLowerRate = Rate("51%"),
        ciHigherRate = Rate("52%"),
        rpLowerRate = Rate("53%"),
        rpHigherRate = Rate("54%"),
        totalCgTaxRate = Rate("60%"),
        title = "Mr",
        forename = "John",
        surname = "Smith"
      )
    }
  }
}
