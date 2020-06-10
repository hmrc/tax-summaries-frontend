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
import controllers.auth.PayeAuthenticatedRequest
import models.PayeAtsData
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.libs.json.{JsResultException, JsValue, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestConstants.testNino

import scala.concurrent.Future
import scala.io.Source

class PayeAtsServiceSpec extends UnitSpec with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  implicit val hc = HeaderCarrier()
  val expectedResponse: JsValue = readJson("/paye_ats.json")
  private val currentYear = 2018

  private def readJson(path: String) = {
    val resource = getClass.getResourceAsStream(path)
    Json.parse(Source.fromInputStream(resource).getLines().mkString)
  }

  val mockMiddleConnector = mock[MiddleConnector]
  implicit val request = PayeAuthenticatedRequest(testNino, FakeRequest("GET", "/annual-tax-summary/paye/"))
  //ToDo need to find what is causing intermittent failure for this unit test
  val mockAuditService: AuditService = mock[AuditService]

  def sut = new PayeAtsService(mockMiddleConnector,mockAuditService)

  override protected def afterEach(): Unit = {
    Mockito.reset(mockAuditService)
  }

  "getPayeATSData" should {

    "return a successful response after transforming tax-summaries data to PAYE model" in {

      when(mockMiddleConnector.connectToPayeATS(eqTo(testNino), eqTo(currentYear))(any[HeaderCarrier]))
        .thenReturn(Future.successful(
          HttpResponse(responseStatus = 200, responseJson = Some(expectedResponse), responseHeaders = Map.empty)))


      val result = sut.getPayeATSData(testNino, currentYear).futureValue

      result shouldBe Right(expectedResponse.as[PayeAtsData])
    }

    "return a INTERNAL_SERVER_ERROR response after receiving JsResultException while json parsing" in {

      when(mockMiddleConnector.connectToPayeATS(eqTo(testNino), eqTo(currentYear))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new JsResultException(List())))


      val result = sut.getPayeATSData(testNino, currentYear).futureValue

      result.left.get.status shouldBe 500
    }

    "return a BAD_REQUEST response after receiving BadRequestException from connector" in {

      when(mockMiddleConnector.connectToPayeATS(eqTo(testNino), eqTo(currentYear))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new BadRequestException("Bad Request")))

      val result = sut.getPayeATSData(testNino, currentYear).futureValue

      result.left.get.status shouldBe 400
    }

    "return a NOT_FOUND response after receiving NotFoundException from connector" in {

      when(mockMiddleConnector.connectToPayeATS(eqTo(testNino), eqTo(currentYear))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new NotFoundException("Not Found")))

      val result = sut.getPayeATSData(testNino, currentYear).futureValue

      result.left.get.status shouldBe 404
    }

    "produce a 'success' audit event when returning a successful response" in {
      when(mockMiddleConnector.connectToPayeATS(eqTo(testNino), eqTo(currentYear))(any[HeaderCarrier]))
        .thenReturn(Future.successful(
          HttpResponse(responseStatus = 200, responseJson = Some(expectedResponse), responseHeaders = Map.empty)))

      sut.getPayeATSData(testNino, currentYear)

      verify(mockAuditService, times(1)).sendEvent(
        eqTo("TxSuccessful"),
        eqTo(Map("userNino" -> testNino.nino, "taxYear" -> currentYear.toString)),
        any[Option[String]]
      )(any[Request[_]], any[HeaderCarrier])
    }
  }

}
