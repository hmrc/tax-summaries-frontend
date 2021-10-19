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
import models.{AtsErrorResponse, AtsNotFoundResponse, AtsResponse, AtsSuccessResponseWithPayload}
import play.api.http.Status._
import play.api.libs.json.{JsValue, Reads}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpException, HttpResponse, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HttpHandler @Inject()(http: HttpClient)(implicit ec: ExecutionContext) extends LazyLogging {

  def get[A](url: String)(implicit reads: Reads[A], hc: HeaderCarrier): Future[AtsResponse] =
    http.GET[Either[UpstreamErrorResponse, HttpResponse]](url) map { response =>
      response match {
        case Left(upstreamErrorResponse) =>
          upstreamErrorResponse.statusCode match {
            case NOT_FOUND =>
              logger.warn(upstreamErrorResponse.getMessage())
              AtsNotFoundResponse(upstreamErrorResponse.getMessage())
            case _ =>
              logger.error(upstreamErrorResponse.message)
              AtsErrorResponse(upstreamErrorResponse.message)
          }
        case Right(response) => extractJson[A](response.json)
      }
    } recover {
      case e: HttpException =>
        logger.error(e.message)
        AtsErrorResponse(e.message)
    }

  private def extractJson[A](value: JsValue)(implicit reads: Reads[A]): AtsResponse =
    value.asOpt[A] match {
      case Some(value) => AtsSuccessResponseWithPayload[A](value)
      case None        => AtsErrorResponse("Could not parse Json")
    }
}
