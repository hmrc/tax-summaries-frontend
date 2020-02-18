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

import connectors.MiddleConnector
import models.PayeAtsData
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.libs.json.{JsResultException, JsValue, Json}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.test.UnitSpec
import utils.JsonUtil
import utils.TestConstants.testNino
import org.mockito.Matchers.{any, eq => eqTo}

import scala.io.Source
import scala.concurrent.Future

class PayeAtsServiceSpec extends UnitSpec with MockitoSugar with JsonUtil with GuiceOneAppPerTest with ScalaFutures with IntegrationPatience {

  implicit val hc = HeaderCarrier()

  private def readJson(path: String) = {
    val resource = getClass.getResourceAsStream(path)
    Json.parse(Source.fromInputStream(resource).getLines().mkString)
  }

  val expectedResponse: JsValue = readJson("/paye_ats.json")
  class TestService extends PayeAtsService {
    lazy val middleConnector: MiddleConnector = mock[MiddleConnector]
  }

  private val currentYear = 2018

  "getPayeATSData" should {

    "return a successful response after transforming tax-summaries data to PAYE model" in new TestService {

      when(middleConnector.connectToPayeATS(eqTo(testNino), eqTo(currentYear))(any[HeaderCarrier]))
        .thenReturn(Future.successful(
          HttpResponse(responseStatus = 200, responseJson = Some(expectedResponse), responseHeaders = Map.empty)))


      val result = getPayeATSData(testNino, currentYear).futureValue

      result shouldBe Right(expectedResponse.as[PayeAtsData])
    }

    "return a INTERNAL_SERVER_ERROR response after receiving JsResultException while json parsing" in new TestService {

      when(middleConnector.connectToPayeATS(eqTo(testNino), eqTo(currentYear))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new JsResultException(List())))


      val result = getPayeATSData(testNino, currentYear).futureValue

//      result shouldBe Left(HttpResponse(500))
      result.left.get.status shouldBe 500
    }

    "return a BAD_REQUEST response after receiving BadRequestException from connector" in new TestService {

      when(middleConnector.connectToPayeATS(eqTo(testNino), eqTo(currentYear))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new BadRequestException("Bad Request")))

      val result = getPayeATSData(testNino, currentYear).futureValue

      result.left.get.status shouldBe 400
    }

    "return a NOT_FOUND response after receiving NotFoundException from connector" in new TestService {

      when(middleConnector.connectToPayeATS(eqTo(testNino), eqTo(currentYear))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new NotFoundException("Not Found")))

      val result = getPayeATSData(testNino, currentYear).futureValue

      result.left.get.status shouldBe 404
    }
    }

}
