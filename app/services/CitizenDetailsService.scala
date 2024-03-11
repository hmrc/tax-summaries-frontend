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
import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

sealed trait MatchingDetailsResponse
case class SucccessMatchingDetailsResponse(matchingDetails: MatchingDetails) extends MatchingDetailsResponse
object FailedNotFoundMatchingDetailsResponse extends MatchingDetailsResponse
object FailedErrorMatchingDetailsResponse extends MatchingDetailsResponse

class CitizenDetailsService @Inject() (citizenDetailsConnector: CitizenDetailsConnector)(implicit
  ec: ExecutionContext
) {
  def getMatchingDetails(nino: String)(implicit hc: HeaderCarrier): Future[MatchingDetailsResponse] =
    citizenDetailsConnector.connectToCid(nino).map {
      case Left(e) if e.statusCode == NOT_FOUND             => FailedNotFoundMatchingDetailsResponse
      case Left(_)                                          => FailedErrorMatchingDetailsResponse
      case Right(httpResponse) if httpResponse.status == OK =>
        SucccessMatchingDetailsResponse(MatchingDetails.fromJsonMatchingDetails(httpResponse.json))
      case Right(_)                                         => FailedErrorMatchingDetailsResponse
    }
}
