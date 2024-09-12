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

import cats.data.EitherT
import connectors.CitizenDetailsConnector
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.domain.{Generator, Nino, SaUtr, SaUtrGenerator}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import utils.BaseSpec

class CitizenDetailsServiceSpec extends BaseSpec with ScalaFutures {

  implicit val hc: HeaderCarrier                       = HeaderCarrier()
  val citizenDetailsConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]
  val service: CitizenDetailsService                   = new CitizenDetailsService(citizenDetailsConnector)

  val nino: Nino = new Generator().nextNino
  val utr: SaUtr = new SaUtrGenerator().nextSaUtr

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
        .thenReturn(EitherT.rightT(response))

      val result = service.getMatchingSaUtr(nino.toString()).value.futureValue
      result mustBe Right(Some(utr))
    }

    List(BAD_REQUEST, LOCKED, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE).foreach { httpStatus =>
      s"when cid sends a $httpStatus, return a Left[UpstreamErrorResponse]" in {
        val response = UpstreamErrorResponse.apply("body", httpStatus)

        when(citizenDetailsConnector.connectToCid(any())(any()))
          .thenReturn(EitherT.leftT(response))

        val result = service.getMatchingSaUtr(nino.toString()).value.futureValue
        result mustBe a[Left[UpstreamErrorResponse, _]]
      }
    }

    "when cid sends a NOT_FOUND, return a Right(None)" in {
      val response = UpstreamErrorResponse.apply("body", NOT_FOUND)

      when(citizenDetailsConnector.connectToCid(any())(any()))
        .thenReturn(EitherT.leftT(response))

      val result = service.getMatchingSaUtr(nino.toString()).value.futureValue
      result mustBe Right(None)
    }
  }
}
