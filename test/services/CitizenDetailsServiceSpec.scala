/*
 * Copyright 2021 HM Revenue & Customs
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
import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.Injecting
import uk.gov.hmrc.domain.{Generator, SaUtrGenerator}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}

class CitizenDetailsServiceSpec
    extends UnitSpec with GuiceOneAppPerSuite with ScalaFutures with MockitoSugar with Injecting {

  implicit val hc = HeaderCarrier()
  lazy implicit val ec = inject[ExecutionContext]
  val citizenDetailsConnector = mock[CitizenDetailsConnector]
  val service = new CitizenDetailsService(citizenDetailsConnector)

  val nino = new Generator().nextNino
  val utr = new SaUtrGenerator().nextSaUtr

  "getUtr" should {
    "return the utr from CID" in {
      val json = Json.parse(s"""
                               |{
                               |"ids":
                               |{
                               | "sautr": "$utr"
                               |}
                               |}
                               |""".stripMargin).toString

      val response = HttpResponse.apply(OK, json)
      when(citizenDetailsConnector.connectToCid(meq(nino.toString()))(any())).thenReturn(Future.successful(response))

      val result = service.getUtr(nino.toString()).futureValue
      result shouldBe SucccessMatchingDetailsResponse(MatchingDetails(Some(utr)))
    }

    List(BAD_REQUEST, NOT_FOUND, LOCKED, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE).foreach { httpStatus =>
      s"when cid sends a $httpStatus, return a FailedMatchingDetailsResponse" in {
        val response = HttpResponse.apply(httpStatus, "body")

        when(citizenDetailsConnector.connectToCid(meq(nino.toString()))(any())).thenReturn(Future.successful(response))

        val result = service.getUtr(nino.toString()).futureValue
        result shouldBe FailedMatchingDetailsResponse
      }
    }
  }
}
