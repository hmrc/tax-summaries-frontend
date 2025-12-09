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

import common.models.requests.AuthenticatedRequest
import common.models.{AtsData, requests}
import common.services.AtsService
import common.services.atsData.AtsTestData
import common.services.atsData.AtsTestData.currentTaxYearSA
import common.utils.TestConstants.*
import common.utils.{BaseSpec, GenericViewModel}
import common.view_models.{Amount, AtsList, IncomeBeforeTax}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class IncomeServiceSpec extends BaseSpec {

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

  def sut: IncomeService = new IncomeService(mockAtsService)

  "IncomeService getIncomeData" must {

    "return a GenericViewModel when atsYearListService returns Success(taxYear)" in {
      when(
        mockAtsService.createModel(any(), any())(
          any(),
          any()
        )
      ).thenReturn(Future(genericViewModel))
      val result = Await.result(sut.getIncomeData(currentTaxYearSA)(hc, request), 1500 millis)
      result mustEqual genericViewModel
    }

  }

  "IncomeService.createIncomeConverter" must {

    "return complete IncomeBeforeTaxData when given complete AtsData" in {

      val incomeData: AtsData     = AtsTestData.incomeData
      val result: IncomeBeforeTax = sut.createIncomeConverter(incomeData)
      result mustEqual IncomeBeforeTax(
        currentTaxYearSA,
        "1111111111",
        Amount(100, "GBP"),
        Amount(200, "GBP"),
        Amount(300, "GBP"),
        Amount(400, "GBP"),
        Amount(500, "GBP"),
        Amount(600, "GBP"),
        Amount(700, "GBP"),
        Amount(800, "GBP"),
        "Mr",
        "John",
        "Smith"
      )
    }

  }
}
