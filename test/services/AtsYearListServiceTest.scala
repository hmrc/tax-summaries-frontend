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

import connectors.{DataCacheConnector, MiddleConnector}
import controllers.FakeTaxsPlayApplication
import models.AtsListData
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.{SaUtr, Uar}
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Account
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestConstants._
import utils.{GenericViewModel, AccountUtils, AgentTokenException, AuthorityUtils}
import view_models.{TaxYearEnd, AtsList}

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

class AtsYearListServiceTest extends UnitSpec with FakeTaxsPlayApplication with MockitoSugar with ScalaFutures {

  val data = {
    val source = Source.fromURL(getClass.getResource("/test_list_utr.json")).mkString
    val json = Json.parse(source)
    Json.fromJson[AtsListData](json).get
  }

  class TestService extends AtsYearListService {

    implicit val request = FakeRequest()
    implicit val user = User(AuthorityUtils.saAuthority(testOid, testUtr))
    implicit val hc = new HeaderCarrier

    val agentToken = AgentToken(
      agentUar = testUar,
      clientUtr = testUtr,
      timestamp = 0
    )

    override val atsListService = mock[AtsListService]

  }

  "storeSelectedAtsTaxYear" should {

    "Return a successful future upon success" in new TestService {

      when(atsListService.storeSelectedTaxYear(eqTo(2014))(any[User], any[HeaderCarrier])).thenReturn(Future.successful(2014))

      val result = storeSelectedAtsTaxYear(2014)

      whenReady(result) { result =>
        result shouldBe 2014
      }
    }

    "Return a failed future when None is returned from the dataCache" in new TestService {

      when(atsListService.storeSelectedTaxYear(eqTo(2014))(any[User], any[HeaderCarrier])).thenReturn(Future.failed(new Exception("failed")))

      val result = storeSelectedAtsTaxYear(2014)

      whenReady(result.failed) { exception =>
        exception shouldBe a [Exception]
      }
    }

  }

  "getSelectedAtsTaxYear" should {

    "Return a successful future upon success" in new TestService {

      val fakeRequest = FakeRequest("GET","?taxYear=2014")

      val result = getSelectedAtsTaxYear(user , hc, fakeRequest)

      whenReady(result) { result =>
        result shouldBe 2014
      }
    }

  }

  "getAtsListData" should {

    "Return a successful future upon success" in new TestService {

      val atsList: AtsList = AtsList(
        utr = testUtr,
        forename = "forename",
        surname = "surname",
        yearList = List(
          TaxYearEnd(Some("2014")),
          TaxYearEnd(Some("2015"))
        )
      )

      val model: GenericViewModel = atsList

      when(atsListService.createModel(eqTo(atsList => model))(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)

      val result = getAtsListData(user, hc, request)

    }

  }





  }
