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
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.MustMatchers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestConstants._
import utils.{AuthorityUtils, GenericViewModel}
import view_models.{AtsList, NoTaxYearViewModel, TaxYearEnd}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

class GovernmentSpendServiceTest extends UnitSpec with FakeTaxsPlayApplication with ScalaFutures with MockitoSugar {

  val genericViewModel: GenericViewModel =  AtsList(
    utr = testUtr,
    forename = "forename",
    surname = "surname",
    yearList = List(
      TaxYearEnd(Some("2015"))
    )
  )

  val noTaxViewModel: NoTaxYearViewModel  = new NoTaxYearViewModel

  class TestService extends GovernmentSpendService with MockitoSugar {
    override lazy val atsService: AtsService = mock[AtsService]
    override lazy val atsYearListService: AtsYearListService = mock[AtsYearListService]
    implicit val hc = new HeaderCarrier
    implicit val request = FakeRequest()

    override def getGovernmentSpendData(implicit user: User, hc: HeaderCarrier, request: Request[AnyRef]): Future[GenericViewModel] = {

      atsYearListService.getSelectedAtsTaxYear flatMap {
        case Some(taxYear) => {
          Future.successful(genericViewModel)
        }
        case None => {
          Future.successful(noTaxViewModel)
        }
      }
    }
  }

  "GovernmentSpendService getGovernmentSpendData" should {

    "return a NoTaxYearViewModel when getSelectedAtsTaxYear returns None" in new TestService {

      implicit val user = User(AuthorityUtils.saAuthority(testOid, testUtr))
      when(atsYearListService.getSelectedAtsTaxYear(Matchers.any[User](), Matchers.any[HeaderCarrier], Matchers.any())).thenReturn(Future.successful(None))
      val result = getGovernmentSpendData(user, hc, request)
      result.onComplete(
        res => res mustBe Success(noTaxViewModel)
      )

    }

    "return a GenericViewModel when getSelectedAtsTaxYear returns Some(taxYear)" in new TestService {

      implicit val user = User(AuthorityUtils.saAuthority(testOid, testUtr))
      when(atsYearListService.getSelectedAtsTaxYear(Matchers.any[User](), Matchers.any[HeaderCarrier], Matchers.any())).thenReturn(Future.successful(Some(2015)))
      val result = getGovernmentSpendData(user, hc, request)
      result.onComplete(
        res => res mustBe Success(genericViewModel)
      )

    }

  }
}
