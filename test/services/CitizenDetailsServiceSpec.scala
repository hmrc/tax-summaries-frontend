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

package services

import connectors.CitizenDetailsConnector
import models.MatchingDetails
import org.mockito.ArgumentMatchers.any
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.domain.{Generator, SaUtrGenerator}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import utils.BaseSpec

import scala.concurrent.Future

class CitizenDetailsServiceSpec extends BaseSpec with ScalaFutures {

  implicit val hc             = HeaderCarrier()
  val citizenDetailsConnector = mock[CitizenDetailsConnector]
  val service                 = new CitizenDetailsService(citizenDetailsConnector)

  val nino = new Generator().nextNino
  val utr  = new SaUtrGenerator().nextSaUtr

  "getUtr" must {
    "return the utr from CID for an OK response" in {
      val json = Json
        .parse(s"""
                               |{
                               |"ids":
                               |{
                               | "sautr": "$utr"
                               |}
                               |}
                               |""".stripMargin)
        .toString

      val response = HttpResponse.apply(OK, json)
      when(citizenDetailsConnector.connectToCid(any())(any()))
        .thenReturn(Future.successful(Right(response)))

      val result = service.getMatchingDetails(nino.toString()).futureValue
      result mustBe SucccessMatchingDetailsResponse(MatchingDetails(Some(utr)))
    }

    List(SEE_OTHER, CREATED, ACCEPTED).foreach { httpStatus =>
      s"when cid sends a $httpStatus, return a FailedMatchingDetailsResponse" in {
        val response = HttpResponse.apply(httpStatus, "body")

        when(citizenDetailsConnector.connectToCid(any())(any()))
          .thenReturn(Future.successful(Right(response)))

        val result = service.getMatchingDetails(nino.toString()).futureValue
        result mustBe FailedMatchingDetailsResponse
      }
    }

    List(BAD_REQUEST, NOT_FOUND, LOCKED, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE).foreach { httpStatus =>
      s"when cid sends a $httpStatus, return a FailedMatchingDetailsResponse" in {
        val response = UpstreamErrorResponse.apply("body", httpStatus)

        when(citizenDetailsConnector.connectToCid(any())(any()))
          .thenReturn(Future.successful(Left(response)))

        val result = service.getMatchingDetails(nino.toString()).futureValue
        result mustBe FailedMatchingDetailsResponse
      }
    }
  }
}
