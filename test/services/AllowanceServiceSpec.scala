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

import controllers.auth.requests
import controllers.auth.requests.AuthenticatedRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.mvc.AnyContentAsEmpty
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

class AllowanceServiceSpec extends BaseSpec {
  val genericViewModel: GenericViewModel = AtsList(
    utr = "3000024376",
    forename = "forename",
    surname = "surname",
    yearList = List(taxYear)
  )

  val noAtsaViewModel: NoATSViewModel = NoATSViewModel(taxYear)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val request: AuthenticatedRequest[AnyContentAsEmpty.type] = requests.AuthenticatedRequest(
    userId = "userId",
    agentRef = None,
    saUtr = Some(SaUtr(testUtr)),
    nino = None,
    isAgentActive = false,
    confidenceLevel = ConfidenceLevel.L50,
    credentials = fakeCredentials,
    request = FakeRequest("GET", s"?taxYear=$taxYear")
  )

  val mockAtsService: AtsService = mock[AtsService]

  def sut: AllowanceService = new AllowanceService(mockAtsService)

  "AllowanceService.getAllowances" must {

    "return a GenericViewModel when TaxYearUtil.extractTaxYear returns a taxYear" in {
      when(
        mockAtsService.createModel(any(), any())(
          any(),
          any()
        )
      ).thenReturn(Future(genericViewModel))
      val result = Await.result(sut.getAllowances(taxYear)(request, hc), 1500 millis)
      result mustEqual genericViewModel
    }
  }

  "AllowanceService.allowanceDataConverter" must {
    "return a complete AllowancesData when given complete AtsData" in {

      val atsData = AtsTestData.atsAllowancesData
      val result  = sut.allowanceDataConverter(atsData)

      result mustBe Allowances(
        currentTaxYear,
        "1111111111",
        Amount(100, "GBP"),
        Amount(200, "GBP"),
        Amount(300, "GBP"),
        Amount(400, "GBP"),
        "Mr",
        "John",
        "Smith"
      )
    }
  }
}
