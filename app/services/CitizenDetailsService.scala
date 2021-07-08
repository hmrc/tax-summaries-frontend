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
import models.AtsUtr
import play.api.http.Status._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CitizenDetailsService @Inject()(citizenDetailsConnector: CitizenDetailsConnector)(implicit ec: ExecutionContext) {
  def getUtr(nino: Nino)(implicit hc: HeaderCarrier): Future[Option[AtsUtr]] =
    citizenDetailsConnector.connectToCid(nino).flatMap {
      case response if response.status == OK => Future(Some(response.json.as[AtsUtr]))
      case _                                 => Future(None)
    }
}
