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

package controllers.auth.actions

import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

sealed trait AtsNino
case class SuccessAtsNino(nino: String) extends AtsNino
object NoAtsNinoFound extends AtsNino
object UpliftRequiredAtsNino extends AtsNino
object InsufficientCredsNino extends AtsNino

class NinoAuthAction @Inject() (override val authConnector: DefaultAuthConnector)(implicit ec: ExecutionContext)
    extends AuthorisedFunctions {

  def getNino()(implicit hc: HeaderCarrier): Future[AtsNino] =
    authorised(ConfidenceLevel.L200 and CredentialStrength(CredentialStrength.strong)).retrieve(Retrievals.nino) {
      case Some(nino) => Future(SuccessAtsNino(nino))
      case _          => Future(NoAtsNinoFound)
    } recover {
      case _: InsufficientConfidenceLevel => UpliftRequiredAtsNino
      case _: IncorrectCredentialStrength => InsufficientCredsNino
    }
}
