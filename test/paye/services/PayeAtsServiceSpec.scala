/*
 * Copyright 2023 HM Revenue & Customs
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

package paye.services

import common.models.*
import common.models.requests.{AuthenticatedRequest, PayeAuthenticatedRequest}
import common.services.AuditService
import common.utils.BaseSpec
import common.utils.TestConstants.{testNino, testUtr}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import paye.connectors.PayeConnector
import paye.models.PayeAtsData
import play.api.http.Status
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.{JsResultException, JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}

import scala.concurrent.Future

class PayeAtsServiceSpec extends BaseSpec {
  implicit val hc: HeaderCarrier            = HeaderCarrier()
  val expectedResponse: JsValue             = Json.parse(
    payeAtsData(currentTaxYearSA)
  )
  val expectedResponseCurrentYear: JsValue  = Json.parse(
    payeAtsData(currentTaxYearSA)
  )
  val expectedResponseMultipleYear: JsValue = Json.parse(
    payeAtsDataForYearRange()
  )
  private val currentYearMinus1: Int        = currentTaxYearSA - 1
  val fakeCredentials: Credentials          = new Credentials("provider ID", "provider type")

  val mockPayeConnector: PayeConnector                                           = mock[PayeConnector]
  val payeAuthenticatedRequest: PayeAuthenticatedRequest[AnyContentAsEmpty.type] =
    PayeAuthenticatedRequest(testNino, fakeCredentials, FakeRequest("GET", "/annual-tax-summary/paye/"))

  val authenticatedRequest: AuthenticatedRequest[AnyContentAsEmpty.type] =
    AuthenticatedRequest(
      userId = "userId",
      agentRef = None,
      saUtr = Some(SaUtr(testUtr)),
      nino = Some(testNino),
      isAgentActive = false,
      confidenceLevel = ConfidenceLevel.L50,
      credentials = fakeCredentials,
      request = FakeRequest()
    )
  val mockAuditService: AuditService                                     = mock[AuditService]

  val sut = new PayeAtsService(mockPayeConnector, mockAuditService)

  override protected def afterEach(): Unit =
    Mockito.reset(mockAuditService)

  "getPayeATSData" must {

    "return a successful response after transforming tax-summaries data to PAYE model" in {
      when(mockPayeConnector.getDetail(any(), any())(any()))
        .thenReturn(Future.successful(Right(HttpResponse(OK, expectedResponse, Map[String, Seq[String]]()))))

      val result = sut.getPayeATSData(testNino, currentYearMinus1)(hc).futureValue

      result mustBe Right(expectedResponse.as[PayeAtsData])
    }

    "return will rethrow a JsResultException when the Json is invalid" in {
      val badJson = Json.parse("""
                                 |{
                                 | "some error": 12345
                                 |}
                                 |""".stripMargin)

      when(mockPayeConnector.getDetail(any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(HttpResponse(OK, badJson, Map[String, Seq[String]]()))))

      val result = sut.getPayeATSData(testNino, currentYearMinus1)(hc).failed.futureValue

      result mustBe a[JsResultException]
    }

    "return a BAD_REQUEST response after receiving BadRequestException from connector" in {
      when(mockPayeConnector.getDetail(any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("bad request", BAD_REQUEST))))

      val result = sut.getPayeATSData(testNino, currentYearMinus1)(hc).futureValue

      result.left.value mustBe an[AtsBadRequestResponse]
    }

    "return a NOT_FOUND response after receiving NOT_FOUND from connector" in {
      when(mockPayeConnector.getDetail(any(), any())(any()))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("not found", NOT_FOUND))))

      val result = sut.getPayeATSData(testNino, currentYearMinus1)(hc).futureValue

      result.left.value mustBe an[AtsNotFoundResponse]
    }

    "return a INTERNAL_SERVER_ERROR response after receiving some other error status" in {
      when(mockPayeConnector.getDetail(any(), any())(any()))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("some error", INTERNAL_SERVER_ERROR))))

      val result = sut.getPayeATSData(testNino, currentYearMinus1)(hc).futureValue

      result.left.value mustBe an[AtsErrorResponse]
    }

    "produce a 'success' audit event when returning a successful response" in {
      when(mockPayeConnector.getDetail(any(), any())(any()))
        .thenReturn(Future.successful(Right(HttpResponse(OK, expectedResponse, Map[String, Seq[String]]()))))

      sut.getPayeATSData(testNino, currentYearMinus1)(hc).futureValue

      verify(mockAuditService, times(1)).sendEvent(
        eqTo("TxSuccessful"),
        eqTo(Map("userNino" -> testNino.nino, "taxYear" -> currentYearMinus1.toString))
      )(any())
    }
  }

  "getPayeATSMultipleYearData" must {
    "return a successful response as list of tax years" in {
      when(
        mockPayeConnector
          .getDetailMultipleYears(eqTo(testNino), eqTo(currentYearMinus1), eqTo(currentTaxYearSA))(
            any[HeaderCarrier]
          )
      )
        .thenReturn(
          Future.successful(Right(HttpResponse(OK, expectedResponseMultipleYear, Map[String, Seq[String]]())))
        )

      val result =
        sut.getPayeTaxYearData(testNino, currentYearMinus1, currentTaxYearSA)(hc).futureValue

      result mustBe Right(listOfTaxYearsPAYE().reverse)
    }

    "return a left of response after receiving left from connector" in {

      when(
        mockPayeConnector
          .getDetailMultipleYears(eqTo(testNino), eqTo(currentYearMinus1), eqTo(currentTaxYearSA))(
            any[HeaderCarrier]
          )
      )
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("body", Status.NOT_FOUND))))

      val result =
        sut.getPayeTaxYearData(testNino, currentYearMinus1, currentTaxYearSA)(hc).futureValue

      result.value mustBe Nil
    }

    "return a BAD_REQUEST response after receiving a BAD_REQUEST from connector" in {

      when(
        mockPayeConnector
          .getDetailMultipleYears(eqTo(testNino), eqTo(currentYearMinus1), eqTo(currentTaxYearSA))(
            any[HeaderCarrier]
          )
      )
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("Bad Request", BAD_REQUEST))))

      val result =
        sut.getPayeTaxYearData(testNino, currentYearMinus1, currentTaxYearSA)(hc).futureValue

      result.left.value mustBe an[AtsBadRequestResponse]
    }

    "return an empty list after receiving NOT_FOUND from connector" in {
      when(
        mockPayeConnector
          .getDetailMultipleYears(eqTo(testNino), eqTo(currentYearMinus1), eqTo(currentTaxYearSA))(
            any[HeaderCarrier]
          )
      )
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("Not Found", NOT_FOUND))))

      val result =
        sut.getPayeTaxYearData(testNino, currentYearMinus1, currentTaxYearSA)(hc).futureValue

      result mustBe Right(List.empty)
    }
  }
}
