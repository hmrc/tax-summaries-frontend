/*
 * Copyright 2019 HM Revenue & Customs
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

import controllers.FakeTaxsPlayApplication
import controllers.auth.AuthenticatedRequest
import models.AtsData
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.MustMatchers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import utils.GenericViewModel
import view_models.{AtsList, TaxYearEnd}

import scala.concurrent.Await
import scala.concurrent.duration._

class GovernmentSpendServiceTest extends UnitSpec with FakeTaxsPlayApplication with ScalaFutures with MockitoSugar {

  val genericViewModel: GenericViewModel =  AtsList(
    utr = "3000024376",
    forename = "forename",
    surname = "surname",
    yearList = List(
      TaxYearEnd(Some("2015"))
    )
  )

  class TestService extends GovernmentSpendService with MockitoSugar {
    override lazy val atsService: AtsService = mock[AtsService]
    override lazy val atsYearListService: AtsYearListService = mock[AtsYearListService]
    implicit val hc = new HeaderCarrier
    val request = AuthenticatedRequest("userId", None, Some(SaUtr("1111111111")), None, None, None, None, FakeRequest("GET","?taxYear=2015"))
    val taxYear = 2015
  }

  "GovernmentSpendService getGovernmentSpendData" should {

    "return a GenericViewModel when atsYearListService returns Success(taxYear)" in new TestService{
      when(atsService.createModel(Matchers.eq(taxYear),Matchers.any[Function1[AtsData,GenericViewModel]]())(Matchers.any(), Matchers.any())).thenReturn(genericViewModel)
      val result = Await.result(getGovernmentSpendData(taxYear)(hc, request), 1500 millis)
      result mustEqual genericViewModel
    }

  }
}
