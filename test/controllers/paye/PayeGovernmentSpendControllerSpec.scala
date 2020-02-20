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

import controllers.auth.{FakePayeAuthAction, PayeAuthAction, PayeAuthenticatedRequest}
import controllers.routes
import models.PayeAtsData
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, redirectLocation}
import services.PayeAtsService
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec
import utils.JsonUtil
import utils.TestConstants.testNino
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation}
import org.mockito.Matchers.{eq => eqTo, _}

import scala.io.Source

class PayeGovernmentSpendControllerSpec  extends UnitSpec with MockitoSugar with JsonUtil with GuiceOneAppPerTest with ScalaFutures with IntegrationPatience {

  implicit val hc = HeaderCarrier()
  val taxYear = 2014
  val fakeAuthenticatedRequest = PayeAuthenticatedRequest("userId", testNino, FakeRequest("GET", s"?taxYear=$taxYear"))

  class TestController extends PayeGovernmentSpendController {
    override val payeAuthAction: PayeAuthAction = FakePayeAuthAction
    override val payeAtsService = mock[PayeAtsService]
    override val payeYear = 2019

    private def readJson(path: String) = {
      val resource = getClass.getResourceAsStream(path)
      Json.parse(Source.fromInputStream(resource).getLines().mkString)
    }

    val expectedResponse: JsValue = readJson("/paye_ats.json")
  }

  "Government spend controller" should {

    "return OK response" in new TestController {

      when(payeAtsService.getPayeATSData(eqTo(testNino), eqTo(2019))(any[HeaderCarrier]))
        .thenReturn(Right(expectedResponse.as[PayeAtsData]))

      val result = show(fakeAuthenticatedRequest)

      status(result) shouldBe OK
    }

    "return bad request and errors when receiving any errors from service" in new TestController {

      when(payeAtsService.getPayeATSData(eqTo(testNino), eqTo(2019))(any[HeaderCarrier]))
        .thenReturn(Left(HttpResponse(responseStatus = NOT_FOUND, responseJson = Some(Json.toJson(NOT_FOUND)))))

      val result = show(fakeAuthenticatedRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.ErrorController.authorisedNoAts().url

    }
  }

}
