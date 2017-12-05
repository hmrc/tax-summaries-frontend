/*
 * Copyright 2017 HM Revenue & Customs
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
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import uk.gov.hmrc.play.test.UnitSpec
import utils.{GenericViewModel, AuthorityUtils}
import utils.TestConstants._
import view_models.{TaxYearEnd, AtsList}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class CapitalGainsServiceTest extends UnitSpec with FakeTaxsPlayApplication with ScalaFutures with MockitoSugar {

  class TestService extends CapitalGainsService with MockitoSugar {

    override lazy val atsService: AtsService = mock[AtsService]
    override lazy val atsYearListService: AtsYearListService = mock[AtsYearListService]
    implicit val hc = new HeaderCarrier
    implicit val request = FakeRequest()



  }

  "CapitalGainsService getCapitalGains" should {

    "return model " in new TestService {

      val model: GenericViewModel = AtsList(
        utr = testUtr,
        forename = "forename",
        surname = "surname",
        yearList = List(
          TaxYearEnd(Some("2014"))
        )
      )

      implicit val user = User(AuthorityUtils.saAuthority(testOid, testUtr))
      when(atsYearListService.getSelectedAtsTaxYear(Matchers.any[User](), Matchers.any[HeaderCarrier], Matchers.any())).thenReturn(Future.successful(2015))
//      when(atsService.createModel(Matchers.any(),Matchers.any())).thenReturn(model)
      val result = getCapitalGains(user, hc, request)

    }

  }
}
