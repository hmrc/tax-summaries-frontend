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

package services

import controllers.auth.AuthenticatedRequest
import models.{AtsData, SpendData}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.MustMatchers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.FakeRequest
import services.atsData.AtsTestData
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import utils.GenericViewModel
import utils.TestConstants._
import view_models.{Amount, AtsList, GovernmentSpend, TaxYearEnd}

import scala.concurrent.Await
import scala.concurrent.duration._

class GovernmentSpendServiceSpec extends UnitSpec with GuiceOneAppPerSuite with ScalaFutures with MockitoSugar {

  val genericViewModel: GenericViewModel = AtsList(
    utr = "3000024376",
    forename = "forename",
    surname = "surname",
    yearList = List(
      TaxYearEnd(Some("2015"))
    )
  )

  val taxYear = 2015

  val mockAtsService = mock[AtsService]

  implicit val hc = new HeaderCarrier
  val request = AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest("GET","?taxYear=2015"))

  def sut = new GovernmentSpendService(mockAtsService) with MockitoSugar {
    override lazy val atsYearListService: AtsYearListService = mock[AtsYearListService]
  }

  "GovernmentSpendService getGovernmentSpendData" should {

    "return a GenericViewModel when atsYearListService returns Success(taxYear)" in {
      when(mockAtsService.createModel(Matchers.eq(taxYear),Matchers.any[Function1[AtsData,GenericViewModel]]())(Matchers.any(), Matchers.any())).thenReturn(genericViewModel)
      val result = Await.result(sut.getGovernmentSpendData(taxYear)(hc, request), 1500 millis)
      result mustEqual genericViewModel
    }
  }

  "GovernmentSpendService govSpend" should {

    "return a complete GovernmentSpend when given complete AtsData" in {
      val atsData = AtsTestData.govSpendingData
      val result = sut.govSpend(atsData)

      result shouldBe GovernmentSpend(
       2019,
        "1111111111",
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
