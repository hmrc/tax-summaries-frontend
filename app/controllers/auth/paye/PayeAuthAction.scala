/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.auth.paye

import com.google.inject.{ImplementedBy, Inject}
import config.ApplicationConfig
import controllers.auth.{AuthConnector, AuthenticatedRequest}
import play.api.Configuration
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, ConfidenceLevel, InsufficientEnrolments, NoActiveSession, Nino => AuthNino}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class PayeAuthActionImpl @Inject()(override val authConnector: AuthConnector,
                               configuration: Configuration)(implicit ec: ExecutionContext)
  extends PayeAuthAction with AuthorisedFunctions {

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised(ConfidenceLevel.L200 and AuthNino(hasNino = true))
      .retrieve(Retrievals.externalId and Retrievals.nino) {
        case Some(externalId) ~ nino => {
          block {
            AuthenticatedRequest(
              externalId,
              None,
              None,
              nino.map(Nino),
              None,
              None,
              None,
              request
            )
          }
        }
        case _ => throw new RuntimeException("Can't find credentials for user")
      }
  } recover {
    case _: NoActiveSession => {
      lazy val ggSignIn = ApplicationConfig.payeLoginUrl
      lazy val callbackUrl = ApplicationConfig.loginCallback
      Redirect(
        ggSignIn,
        Map(
          "continue"    -> Seq(callbackUrl),
          "origin"      -> Seq(ApplicationConfig.appName)
        )
      )
    }

    case _: InsufficientEnrolments => Redirect(controllers.routes.ErrorController.notAuthorised())
  }
}

@ImplementedBy(classOf[PayeAuthActionImpl])
trait PayeAuthAction extends ActionBuilder[AuthenticatedRequest] with ActionFunction[Request, AuthenticatedRequest]


