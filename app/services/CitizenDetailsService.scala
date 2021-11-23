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

import cats.data.EitherT
import connectors.CitizenDetailsConnector
import models.MatchingDetails
import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

sealed trait MatchingDetailsResponse
case class SucccessMatchingDetailsResponse(matchingDetails: MatchingDetails) extends MatchingDetailsResponse
object FailedMatchingDetailsResponse extends MatchingDetailsResponse

class CitizenDetailsService @Inject()(citizenDetailsConnector: CitizenDetailsConnector)(implicit ec: ExecutionContext) {
  def getMatchingDetails(nino: String)(implicit hc: HeaderCarrier): Future[MatchingDetailsResponse] =
    EitherT(citizenDetailsConnector.connectToCid(nino)).fold(
      _ => FailedMatchingDetailsResponse,
      httpResponse =>
        httpResponse.status match {
          case OK => SucccessMatchingDetailsResponse(MatchingDetails.fromJsonMatchingDetails(httpResponse.json))
          case _  => FailedMatchingDetailsResponse
      }
    )
}
