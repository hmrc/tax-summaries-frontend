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

package common.services

import common.connectors.GovSpendConnector
import common.models.requests.AuthenticatedRequest
import common.models.{SpendData, requests}
import common.utils.TestConstants.*
import common.utils.{BaseSpec, GenericViewModel}
import common.view_models.{Amount, AtsList, GovernmentSpend}
import org.mockito.ArgumentMatchers.{any, eq as meq}
import org.mockito.Mockito.when
import play.api.http.Status.OK
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import sa.models.AtsData
import sa.services.AtsService
import sa.utils.AtsTestData
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class GovernmentSpendServiceSpec extends BaseSpec {

  val genericViewModel: GenericViewModel = AtsList(
    utr = "3000024376",
    forename = "forename",
    surname = "surname",
    yearList = List(currentTaxYearSA)
  )

  val mockAtsService: AtsService             = mock[AtsService]
  val mockMiddleConnector: GovSpendConnector = mock[GovSpendConnector]

  implicit val hc: HeaderCarrier = new HeaderCarrier

  val request: AuthenticatedRequest[AnyContentAsEmpty.type] = requests.AuthenticatedRequest(
    userId = "userId",
    agentRef = None,
    saUtr = Some(SaUtr(testUtr)),
    nino = None,
    isAgentActive = false,
    ConfidenceLevel.L50,
    fakeCredentials,
    FakeRequest("GET", s"?taxYear=$currentTaxYearSA")
  )

  def sut: GovernmentSpendService = new GovernmentSpendService(mockAtsService, mockMiddleConnector)

  "GovernmentSpendService getGovernmentSpendData" must {

    "return a GenericViewModel when atsYearListService returns Success(taxYear)" in {
      when(
        mockAtsService
          .createFutureModel(meq(currentTaxYearSA), any[AtsData => Future[GenericViewModel]]())(any(), any())
      )
        .thenReturn(Future(genericViewModel))
      val result = Await.result(sut.getGovernmentSpendData(currentTaxYearSA)(hc, request, ec), 1500 millis)
      result mustEqual genericViewModel
    }
  }

  "GovernmentSpendService govSpend" must {
    "return a complete GovernmentSpend with sorted spending when given complete AtsData" in {
      val atsData = AtsTestData.govSpendingData
      when(mockMiddleConnector.get(any())(any[HeaderCarrier]))
        .thenReturn(
          Future.successful(
            Right(
              HttpResponse(
                OK,
                """{"Health":21.9,"Welfare":19.6,"StatePensions":10.1,"Education":9.6,"NationalDebtInterest":4.1,
                  |"BusinessAndIndustry":14.4,"Defence":4.5,"Transport":4.5,"PublicOrderAndSafety":3.9,"GovernmentAdministration":2,"HousingAndUtilities":1.4,"Environment":1.3,"Culture":1.2,"OutstandingPaymentsToTheEU":0.6,"OverseasAid":0.9}""".stripMargin,
                Map[String, Seq[String]]()
              )
            )
          )
        )

      val result = Await.result(sut.govSpend(atsData), 1500 millis)
      result mustBe GovernmentSpend(
        currentTaxYearSA,
        "1111111111",
        List(
          ("Health", SpendData(Amount(100, "GBP"), 10)),
          ("Welfare", SpendData(Amount(100, "GBP"), 10)),
          ("StatePensions", SpendData(Amount(100, "GBP"), 10)),
          ("Education", SpendData(Amount(100, "GBP"), 10)),
          ("NationalDebtInterest", SpendData(Amount(100, "GBP"), 10)),
          ("BusinessAndIndustry", SpendData(Amount(100, "GBP"), 10)),
          ("Defence", SpendData(Amount(100, "GBP"), 10)),
          ("Transport", SpendData(Amount(100, "GBP"), 10)),
          ("PublicOrderAndSafety", SpendData(Amount(100, "GBP"), 10)),
          ("GovernmentAdministration", SpendData(Amount(100, "GBP"), 10)),
          ("HousingAndUtilities", SpendData(Amount(100, "GBP"), 10)),
          ("Environment", SpendData(Amount(100, "GBP"), 10)),
          ("Culture", SpendData(Amount(100, "GBP"), 10)),
          ("OutstandingPaymentsToTheEU", SpendData(Amount(100, "GBP"), 10)),
          ("OverseasAid", SpendData(Amount(100, "GBP"), 10))
        ),
        "Mr",
        "John",
        "Smith",
        Amount(200, "GBP"),
        "0002",
        Amount(500, "GBP")
      )
    }

    "return a isScottishTaxPayer as true when incomeTaxStatus is 0002" in {
      val atsData = AtsTestData.govSpendingData
      when(mockMiddleConnector.get(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(HttpResponse(OK, """{"Environment" : 5.5}""", Map[String, Seq[String]]()))))

      val result = Await.result(sut.govSpend(atsData), 1500 millis)

      result.isScottishTaxPayer mustBe true
    }

    "return a isScottishTaxPayer as false when incomeTaxStatus is not 0002" in {
      val atsData = AtsTestData.govSpendingDataForWelshUser

      when(mockMiddleConnector.get(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(HttpResponse(OK, """{"Environment" : 5.5}""", Map[String, Seq[String]]()))))
      val result = Await.result(sut.govSpend(atsData), 1500 millis)

      result.isScottishTaxPayer mustBe false
    }
  }
}
