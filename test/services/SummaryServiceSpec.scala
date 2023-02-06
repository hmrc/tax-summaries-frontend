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

class SummaryServiceSpec extends BaseSpec {

  val genericViewModel: GenericViewModel = AtsList(
    utr = "3000024376",
    forename = "forename",
    surname = "surname",
    yearList = List(2015)
  )

  val mockAtsService = mock[AtsService]

  implicit val hc      = new HeaderCarrier
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

  def sut = new SummaryService(mockAtsService)

  "SummaryService getSummaryData" must {

    "return a GenericViewModel when TaxYearUtil.extractTaxYear returns a taxYear" in {
      when(
        mockAtsService.createModel(meq(taxYear), any[Function1[AtsData, GenericViewModel]]())(
          any(),
          any()
        )
      ).thenReturn(Future(genericViewModel))
      val result = Await.result(sut.getSummaryData(taxYear)(hc, request), 1500 millis)
      result mustEqual genericViewModel
    }
  }

  "SummaryService summaryConverter" must {

    "return a complete Summary when given complete AtsData" in {
      val atsData = AtsTestData.summaryData
      val result  = sut.summaryConverter(atsData)

      result mustBe Summary(
        2019,
        "1111111111",
        Amount(100, "GBP"),
        Amount(200, "GBP"),
        Amount(300, "GBP"),
        Amount(400, "GBP"),
        Amount(400, "GBP"),
        Amount(500, "GBP"),
        Amount(600, "GBP"),
        Amount(700, "GBP"),
        Amount(800, "GBP"),
        Amount(900, "GBP"),
        Amount(1000, "GBP"),
        Rate("10%"),
        Rate("20%"),
        "Mr",
        "John",
        "Smith"
      )
    }
  }
}
