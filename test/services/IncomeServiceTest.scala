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
import models.AtsData
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.test.FakeRequest
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import uk.gov.hmrc.play.test.UnitSpec
import utils.{AuthorityUtils, GenericViewModel}
import utils.TestConstants._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier
import org.scalatest.MustMatchers._
import play.api.mvc.Request
import view_models.{AtsList, NoTaxYearViewModel, TaxYearEnd}

import scala.util.{Failure, Success}

class IncomeServiceTest extends UnitSpec with FakeTaxsPlayApplication with ScalaFutures with MockitoSugar {


  val genericViewModel: GenericViewModel =  AtsList(
    utr = "3000024376",
    forename = "forename",
    surname = "surname",
    yearList = List(
      TaxYearEnd(Some("2015"))
    )
  )

  class TestService extends IncomeService with MockitoSugar {
    override lazy val atsService: AtsService = mock[AtsService]
    override lazy val atsYearListService: AtsYearListService = mock[AtsYearListService]
    implicit val hc = new HeaderCarrier
    implicit val request = FakeRequest("GET","?taxYear=2015")
  }

  "IncomeService getIncomeData" should {

    "return a NoTaxYearViewModel when atsYearListService returns Failure" in new TestService {
      implicit val user = User(AuthorityUtils.saAuthority(testOid, testUtr))
      when(atsYearListService.getSelectedAtsTaxYear(Matchers.any[User](), Matchers.any[HeaderCarrier], Matchers.any())).thenReturn(Future.successful(Failure(new NumberFormatException())))
      val result = getIncomeData(user, hc, request)
      result.onComplete(
        res => {res.toString.split("\\@")(0) mustBe "Success(view_models.NoTaxYearViewModel"}
      )
    }

    "return a GenericViewModel when atsYearListService returns Success(taxYear)" in new TestService{
      implicit val user = User(AuthorityUtils.saAuthority(testOid, testUtr))
      when(atsYearListService.getSelectedAtsTaxYear(Matchers.any[User](), Matchers.any[HeaderCarrier], Matchers.any())).thenReturn(Future.successful(Success(2015)))
      when(atsService.createModel(Matchers.eq(2015),Matchers.any[Function1[AtsData,GenericViewModel]]())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(genericViewModel)
      val result = getIncomeData(user, hc, request)
      result.onComplete(
        result => result.toString mustBe "Success(AtsList(3000024376,forename,surname,List(TaxYearEnd(Some(2015)))))"
      )
    }

  }
}
