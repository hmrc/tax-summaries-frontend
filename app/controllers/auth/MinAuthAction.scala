/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.auth

import com.google.inject.{ImplementedBy, Inject}
import config.ApplicationConfig
import play.api.Configuration
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionBuilder, ActionFunction, Request, Result}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class MinAuthActionImpl @Inject()(override val authConnector: AuthConnector,
                                  configuration: Configuration)(implicit ec: ExecutionContext)
  extends MinAuthAction with AuthorisedFunctions {

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised(ConfidenceLevel.L50).retrieve(Retrievals.externalId) {
      case Some(externalId) =>
        block(AuthenticatedRequest(externalId, None, None, None, None, None, None, request))

      case _ => throw new RuntimeException("Can't find credentials for user")
    }
  } recover {
    case _: NoActiveSession => {
      lazy val ggSignIn = ApplicationConfig.loginUrl
      lazy val callbackUrl = ApplicationConfig.loginCallback
      Redirect(
        ggSignIn,
        Map(
          "continue" -> Seq(callbackUrl),
          "origin" -> Seq(ApplicationConfig.appName)
        )
      )
    }
    case _: InsufficientConfidenceLevel => {
      lazy val ggSignIn = ApplicationConfig.loginUrl
      lazy val callbackUrl = ApplicationConfig.loginCallback
      Redirect(
        ggSignIn,
        Map(
          "continue" -> Seq(callbackUrl),
          "origin" -> Seq(ApplicationConfig.appName)
        )
      )
    }
    case _: InsufficientEnrolments => throw InsufficientEnrolments("")
  }
}

@ImplementedBy(classOf[MinAuthActionImpl])
trait MinAuthAction extends ActionBuilder[AuthenticatedRequest] with ActionFunction[Request, AuthenticatedRequest]