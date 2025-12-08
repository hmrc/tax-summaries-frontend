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

package common.services

import cats.data.EitherT
import common.connectors.CitizenDetailsConnector
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CitizenDetailsService @Inject() (citizenDetailsConnector: CitizenDetailsConnector)(implicit
  ec: ExecutionContext
) {
  def getMatchingSaUtr(
    nino: String
  )(implicit hc: HeaderCarrier): EitherT[Future, UpstreamErrorResponse, Option[SaUtr]] =
    citizenDetailsConnector.connectToCid(nino).transform {
      case Right(httpResponse)                          =>
        Right((httpResponse.json \ "ids" \ "sautr").asOpt[String].map(SaUtr.apply))
      case Left(error) if error.statusCode == NOT_FOUND =>
        Right(None)
      case Left(error)                                  => Left(error)
    }
}
