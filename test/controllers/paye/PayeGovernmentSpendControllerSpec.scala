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

package controllers.paye

import connectors.MiddleConnector
import controllers.auth.{AuthenticatedRequest, PayeAuthAction, PayeAuthenticatedRequest}
import models.PayeAtsData
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.contentAsJson
import services.PayeAtsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import utils.JsonUtil
import utils.TestConstants.{testNino, testUtr}
import org.mockito.Matchers.{eq => eqTo, _}
import play.api.mvc.Result
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.{Nino, SaUtr}

import scala.concurrent.Future
import scala.io.Source

class PayeGovernmentSpendControllerSpec  extends UnitSpec with MockitoSugar with JsonUtil with GuiceOneAppPerTest with ScalaFutures with IntegrationPatience {

  implicit val hc = HeaderCarrier()
  val taxYear = 2014
  val request = FakeRequest()
  val fakeAuthenticatedRequest = PayeAuthenticatedRequest("userId", Some(testNino),FakeRequest("GET", s"?taxYear=$taxYear"))

  class TestController extends PayeGovernmentSpendController {
    lazy val payeAtsService: PayeAtsService = mock[PayeAtsService]
    override val payeAuthAction: PayeAuthAction = mock[PayeAuthAction]
  }

  private def readJson(path: String) = {
    val resource = getClass.getResourceAsStream(path)
    Json.parse(Source.fromInputStream(resource).getLines().mkString)
  }

  val expectedResponse: JsValue = readJson("/paye_ats.json")

  "Government spend controller" should {

    "return OK response" in new TestController {

      when(payeAtsService.getPayeATSData(eqTo(testNino), eqTo(2018))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(expectedResponse.as[PayeAtsData])))

//      val result = show(fakeAuthenticatedRequest)

//      result shouldBe 200
    }

    "return bad request and errors when receiving any errors from service" in new TestController {


    }
  }

}
