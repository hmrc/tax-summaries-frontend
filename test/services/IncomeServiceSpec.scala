/*
 * Copyright 2021 HM Revenue & Customs
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
import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest.MustMatchers._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import services.atsData.AtsTestData
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestConstants._
import utils.{BaseSpec, GenericViewModel}
import view_models.{Amount, AtsList, IncomeBeforeTax, TaxYearEnd}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class IncomeServiceSpec extends BaseSpec {

  val genericViewModel: GenericViewModel = AtsList(
    utr = "3000024376",
    forename = "forename",
    surname = "surname",
    yearList = List(
      TaxYearEnd(Some("2015"))
    )
  )

  implicit val hc = new HeaderCarrier

  val mockAtsService = mock[AtsService]
  val mockAtsYearListService: AtsYearListService = mock[AtsYearListService]
  override val taxYear = 2015
  val request = AuthenticatedRequest(
    "userId",
    None,
    Some(SaUtr(testUtr)),
    None,
    None,
    None,
    None,
    true,
    false,
    fakeCredentials,
    FakeRequest("GET", s"?taxYear=$taxYear"))

  def sut = new IncomeService(mockAtsService, mockAtsYearListService) with MockitoSugar

  "IncomeService getIncomeData" must {

    "return a GenericViewModel when atsYearListService returns Success(taxYear)" in {
      when(
        mockAtsService.createModel(Matchers.eq(taxYear), Matchers.any[Function1[AtsData, GenericViewModel]]())(
          Matchers.any(),
          Matchers.any())).thenReturn(Future(genericViewModel))
      val result = Await.result(sut.getIncomeData(taxYear)(hc, request), 1500 millis)
      result mustEqual genericViewModel
    }

  }

  "IncomeService.createIncomeConverter" must {

    "return complete IncomeBeforeTaxData when given complete AtsData" in {

      val incomeData: AtsData = AtsTestData.incomeData
      val result: IncomeBeforeTax = sut.createIncomeConverter(incomeData)
      result mustEqual IncomeBeforeTax(
        2019,
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
