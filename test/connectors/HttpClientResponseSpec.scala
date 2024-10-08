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

package connectors

import cats.data.EitherT
import org.mockito.Mockito.{reset, times, when}
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.RecoverMethods
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status._
import uk.gov.hmrc.http._
import utils.WireMockHelper
import org.slf4j.{Logger => UnderlyingLogger}
import play.api.Logger

import scala.concurrent.Future

class HttpClientResponseSpec
    extends ConnectorSpec
    with WireMockHelper
    with ScalaFutures
    with IntegrationPatience
    with RecoverMethods
    with MockitoSugar {
  private val mockLogger = mock[UnderlyingLogger]

  private lazy val httpClientResponseUsingMockLogger: HttpClientResponse = new HttpClientResponse {
    override protected val logger: Logger = new Logger(mockLogger)
  }

  private val dummyContent = "error message"

  "read" must {
    behave like clientResponseLogger(
      httpClientResponseUsingMockLogger.read,
      infoLevel = Set(NOT_FOUND, UNPROCESSABLE_ENTITY),
      warnLevel = Set.empty,
      errorLevelWithThrowable = Set(UNAUTHORIZED),
      errorLevelWithoutThrowable = Set(TOO_MANY_REQUESTS, INTERNAL_SERVER_ERROR)
    )
  }

  "readIgnoreUnauthorised" must {
    behave like clientResponseLogger(
      httpClientResponseUsingMockLogger.readIgnoreUnauthorised,
      infoLevel = Set(NOT_FOUND, UNPROCESSABLE_ENTITY),
      warnLevel = Set.empty,
      errorLevelWithThrowable = Set.empty,
      errorLevelWithoutThrowable = Set(TOO_MANY_REQUESTS, INTERNAL_SERVER_ERROR)
    )
  }

  // scalastyle:off method.length
  private def clientResponseLogger(
    block: Future[Either[UpstreamErrorResponse, HttpResponse]] => EitherT[Future, UpstreamErrorResponse, HttpResponse],
    infoLevel: Set[Int],
    warnLevel: Set[Int],
    errorLevelWithThrowable: Set[Int],
    errorLevelWithoutThrowable: Set[Int]
  ): Unit = {
    infoLevel.foreach { httpResponseCode =>
      s"log message: INFO level only when response code is $httpResponseCode" in {
        reset(mockLogger)
        when(mockLogger.isErrorEnabled).thenReturn(true)
        when(mockLogger.isInfoEnabled).thenReturn(true)
        val response: Future[Either[UpstreamErrorResponse, HttpResponse]] =
          Future(Left(UpstreamErrorResponse(dummyContent, httpResponseCode)))
        whenReady(block(response).value) { actual =>
          actual mustBe Left(UpstreamErrorResponse(dummyContent, httpResponseCode))
          verifyCalls(info = Some(dummyContent))
        }
      }
    }
    warnLevel.foreach { httpResponseCode =>
      s"log message: WARNING level only when response is $httpResponseCode" in {
        reset(mockLogger)
        when(mockLogger.isErrorEnabled).thenReturn(true)
        when(mockLogger.isInfoEnabled).thenReturn(true)
        val response: Future[Either[UpstreamErrorResponse, HttpResponse]] =
          Future(Left(UpstreamErrorResponse(dummyContent, httpResponseCode)))
        whenReady(block(response).value) { actual =>
          actual mustBe Left(UpstreamErrorResponse(dummyContent, httpResponseCode))
          verifyCalls(warn = Some(dummyContent))
        }
      }
    }
    errorLevelWithThrowable.foreach { httpResponseCode =>
      s"log message: ERROR level only WITH throwable when response code is $httpResponseCode" in {
        reset(mockLogger)
        when(mockLogger.isErrorEnabled).thenReturn(true)
        when(mockLogger.isInfoEnabled).thenReturn(true)
        val response: Future[Either[UpstreamErrorResponse, HttpResponse]] =
          Future(Left(UpstreamErrorResponse(dummyContent, httpResponseCode)))
        whenReady(block(response).value) { actual =>
          actual mustBe Left(UpstreamErrorResponse(dummyContent, httpResponseCode))
          verifyCalls(errorWithThrowable = Some(dummyContent))
        }
      }
    }
    errorLevelWithoutThrowable.foreach { httpResponseCode =>
      s"log message: ERROR level only WITHOUT throwable when response code is $httpResponseCode" in {
        reset(mockLogger)
        when(mockLogger.isErrorEnabled).thenReturn(true)
        when(mockLogger.isInfoEnabled).thenReturn(true)
        val response: Future[Either[UpstreamErrorResponse, HttpResponse]] =
          Future(Left(UpstreamErrorResponse(dummyContent, httpResponseCode)))
        whenReady(block(response).value) { actual =>
          actual mustBe Left(UpstreamErrorResponse(dummyContent, httpResponseCode))
          verifyCalls(errorWithoutThrowable = Some(dummyContent))
        }
      }
    }
    "log message: ERROR level only WITHOUT throwable when future failed with HttpException & " +
      "recover to BAD GATEWAY" in {
        reset(mockLogger)
        when(mockLogger.isErrorEnabled).thenReturn(true)
        when(mockLogger.isInfoEnabled).thenReturn(true)
        val response: Future[Either[UpstreamErrorResponse, HttpResponse]] =
          Future.failed(new HttpException(dummyContent, GATEWAY_TIMEOUT))
        whenReady(block(response).value) { actual =>
          actual mustBe Left(UpstreamErrorResponse(dummyContent, BAD_GATEWAY))
          verifyCalls(errorWithoutThrowable = Some(dummyContent))
        }
      }
    "log nothing at all when future failed with non-HTTPException" in {
      reset(mockLogger)
      when(mockLogger.isErrorEnabled).thenReturn(true)
      when(mockLogger.isInfoEnabled).thenReturn(true)
      val response: Future[Either[UpstreamErrorResponse, HttpResponse]] =
        Future.failed(new RuntimeException(dummyContent))

      recoverToSucceededIf[RuntimeException] {
        block(response).value
      }
      verifyCalls()
    }
  }

  private def verifyCalls(
    info: Option[String] = None,
    warn: Option[String] = None,
    errorWithThrowable: Option[String] = None,
    errorWithoutThrowable: Option[String] = None
  ): Unit = {

    val infoTimes                  = info.map(_ => 1).getOrElse(0)
    val warnTimes                  = warn.map(_ => 1).getOrElse(0)
    val errorWithThrowableTimes    = errorWithThrowable.map(_ => 1).getOrElse(0)
    val errorWithoutThrowableTimes = errorWithoutThrowable.map(_ => 1).getOrElse(0)

    def argumentMatcher(content: Option[String]): String = content match {
      case None    => ArgumentMatchers.any()
      case Some(c) => ArgumentMatchers.eq(c)
    }

    Mockito
      .verify(mockLogger, times(infoTimes))
      .info(argumentMatcher(info))
    Mockito
      .verify(mockLogger, times(warnTimes))
      .warn(argumentMatcher(warn))
    Mockito
      .verify(mockLogger, times(errorWithThrowableTimes))
      .error(argumentMatcher(errorWithThrowable), ArgumentMatchers.any())
    Mockito
      .verify(mockLogger, times(errorWithoutThrowableTimes))
      .error(argumentMatcher(errorWithoutThrowable))
  }

}
