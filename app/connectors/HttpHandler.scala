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

package connectors

import com.typesafe.scalalogging.LazyLogging
import javax.inject.Inject
import models.{AtsErrorResponse, AtsNotFoundResponse, AtsResponse, AtsSuccessResponseWithPayload}
import play.api.http.Status._
import play.api.libs.json.{JsValue, Reads}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException, Upstream4xxResponse, Upstream5xxResponse}
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.concurrent.{ExecutionContext, Future}

class HttpHandler @Inject()(val http: DefaultHttpClient)(implicit ec: ExecutionContext) extends LazyLogging {

  def get[A](url: String)(implicit reads: Reads[A], hc: HeaderCarrier): Future[AtsResponse] =
    http.GET[HttpResponse](url) map { response =>
      response.status match {
        case OK => extractJson[A](response.json)
        case e @ _ =>
          val message = s"Connector returned $e: $url"
          println("..................")
          println("..................")
          println("..................")
          println("..................")
          logger.error(message)
          AtsErrorResponse(message)

      }
    } recover {
      case e: NotFoundException =>
        logger.warn(e.message)
        AtsNotFoundResponse(e.responseCode.toString)
      case e: Upstream4xxResponse if (e.upstreamResponseCode == UNAUTHORIZED) =>
        println(":::::::::")
        println(":::::::::")
        println(":::::::::")
        println(":::::::::")
        println(":::::::::")
        logger.error(e.getMessage)
        AtsErrorResponse(e.getMessage)
      case e @ (_: Upstream5xxResponse | _: Exception) =>
        logger.error(e.getMessage)
        AtsErrorResponse(e.getMessage)
    }

  private def extractJson[A](value: JsValue)(implicit reads: Reads[A]): AtsResponse =
    value.asOpt[A] match {
      case Some(value) => AtsSuccessResponseWithPayload[A](value)
      case None        => AtsErrorResponse("Could not parse Json")
    }
}
