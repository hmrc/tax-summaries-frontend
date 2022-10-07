/*
 * Copyright 2022 HM Revenue & Customs
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

import config.ApplicationConfig
import connectors.MiddleConnector
import controllers.auth.{AuthenticatedRequest, PayeAuthenticatedRequest}
import models.{AtsBadRequestResponse, AtsErrorResponse, AtsNotFoundResponse, PayeAtsData}
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import play.api.http.Status
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.{JsResultException, JsValue, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpResponse, NotFoundException, UpstreamErrorResponse}
import utils.BaseSpec
import utils.TestConstants.{testNino, testUtr}

import scala.concurrent.Future
import scala.io.Source

class PayeAtsServiceSpec extends BaseSpec {

  implicit val hc                           = HeaderCarrier()
  val expectedResponse: JsValue             = readJson("/paye_ats_2020.json")
  val expectedResponseCurrentYear: JsValue  = readJson("/paye_ats_2021.json")
  val expectedResponseMultipleYear: JsValue = readJson("/paye_ats_multiple_years.json")
  private val currentYearMinus1             = 2018
  private val currentYear                   = 2019
  val fakeCredentials                       = new Credentials("provider ID", "provider type")

  private def readJson(path: String) = {
    val resource = getClass.getResourceAsStream(path)
    Json.parse(Source.fromInputStream(resource).getLines().mkString)
  }

  val mockMiddleConnector      = mock[MiddleConnector]
  val payeAuthenticatedRequest =
    PayeAuthenticatedRequest(testNino, false, fakeCredentials, FakeRequest("GET", "/annual-tax-summary/paye/"))

  val authenticatedRequest           =
    AuthenticatedRequest(
      "userId",
      None,
      Some(SaUtr(testUtr)),
      Some(testNino),
      true,
      false,
      ConfidenceLevel.L50,
      fakeCredentials,
      FakeRequest()
    )
  val mockAuditService: AuditService = mock[AuditService]

  val sut = new PayeAtsService(mockMiddleConnector, mockAuditService)

  override protected def afterEach(): Unit =
    Mockito.reset(mockAuditService)

  "getPayeATSData" must {

    "return a successful response after transforming tax-summaries data to PAYE model" in {
      when(mockMiddleConnector.connectToPayeATS(eqTo(testNino), eqTo(currentYearMinus1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(HttpResponse(OK, expectedResponse, Map[String, Seq[String]]()))))

      val result = sut.getPayeATSData(testNino, currentYearMinus1)(hc, payeAuthenticatedRequest).futureValue

      result mustBe Right(expectedResponse.as[PayeAtsData])
    }

    "return will rethrow a JsResultException when the Json is invalid" in {
      val badJson = Json.parse("""
                                 |{
                                 | "some error": 12345
                                 |}
                                 |""".stripMargin)

      when(mockMiddleConnector.connectToPayeATS(eqTo(testNino), eqTo(currentYearMinus1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(HttpResponse(OK, badJson, Map[String, Seq[String]]()))))

      val result = sut.getPayeATSData(testNino, currentYearMinus1)(hc, payeAuthenticatedRequest).failed.futureValue

      result mustBe a[JsResultException]
    }

    "return a BAD_REQUEST response after receiving BadRequestException from connector" in {
      when(mockMiddleConnector.connectToPayeATS(eqTo(testNino), eqTo(currentYearMinus1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("bad request", BAD_REQUEST))))

      val result = sut.getPayeATSData(testNino, currentYearMinus1)(hc, payeAuthenticatedRequest).futureValue

      result.left.value mustBe an[AtsBadRequestResponse]
    }

    "return a NOT_FOUND response after receiving NOT_FOUND from connector" in {
      when(mockMiddleConnector.connectToPayeATS(eqTo(testNino), eqTo(currentYearMinus1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("not found", NOT_FOUND))))

      val result = sut.getPayeATSData(testNino, currentYearMinus1)(hc, payeAuthenticatedRequest).futureValue

      result.left.value mustBe an[AtsNotFoundResponse]
    }

    "return a INTERNAL_SERVER_ERROR response after receiving some other error status" in {
      when(mockMiddleConnector.connectToPayeATS(eqTo(testNino), eqTo(currentYearMinus1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("some error", INTERNAL_SERVER_ERROR))))

      val result = sut.getPayeATSData(testNino, currentYearMinus1)(hc, payeAuthenticatedRequest).futureValue

      result.left.value mustBe an[AtsErrorResponse]
    }

    "produce a 'success' audit event when returning a successful response" in {
      when(mockMiddleConnector.connectToPayeATS(eqTo(testNino), eqTo(currentYearMinus1))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(HttpResponse(OK, expectedResponse, Map[String, Seq[String]]()))))

      sut.getPayeATSData(testNino, currentYearMinus1)(hc, payeAuthenticatedRequest).futureValue

      verify(mockAuditService, times(1)).sendEvent(
        eqTo("TxSuccessful"),
        eqTo(Map("userNino" -> testNino.nino, "taxYear" -> currentYearMinus1.toString)),
        any[Option[String]]
      )(any[Request[_]], any[HeaderCarrier])
    }
  }

  "getPayeATSMultipleYearData" must {
    "return a successful response as list of tax years" in {
      when(
        mockMiddleConnector.connectToPayeATSMultipleYears(eqTo(testNino), eqTo(currentYearMinus1), eqTo(currentYear))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(
          Future.successful(Right(HttpResponse(OK, expectedResponseMultipleYear, Map[String, Seq[String]]())))
        )

      val result =
        sut.getPayeTaxYearData(testNino, currentYearMinus1, currentYear)(hc, authenticatedRequest).futureValue

      result mustBe Right(List(2020, 2019))
    }

    "return a left of response after receiving left from connector" in {

      when(
        mockMiddleConnector.connectToPayeATSMultipleYears(eqTo(testNino), eqTo(currentYearMinus1), eqTo(currentYear))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("body", Status.NOT_FOUND))))

      val result =
        sut.getPayeTaxYearData(testNino, currentYearMinus1, currentYear)(hc, authenticatedRequest).futureValue

      result.right.value mustBe Nil
    }

    "return a BAD_REQUEST response after receiving a BAD_REQUEST from connector" in {

      when(
        mockMiddleConnector.connectToPayeATSMultipleYears(eqTo(testNino), eqTo(currentYearMinus1), eqTo(currentYear))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("Bad Request", BAD_REQUEST))))

      val result =
        sut.getPayeTaxYearData(testNino, currentYearMinus1, currentYear)(hc, authenticatedRequest).futureValue

      result.left.value mustBe an[AtsBadRequestResponse]
    }

    "return an empty list after receiving NOT_FOUND from connector" in {
      when(
        mockMiddleConnector.connectToPayeATSMultipleYears(eqTo(testNino), eqTo(currentYearMinus1), eqTo(currentYear))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("Not Found", NOT_FOUND))))

      val result =
        sut.getPayeTaxYearData(testNino, currentYearMinus1, currentYear)(hc, authenticatedRequest).futureValue

      result mustBe Right(List.empty)
    }
  }
}
