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

package controllers.auth

import com.google.inject.{ImplementedBy, Inject}
import config.ApplicationConfig
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.domain._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class AuthActionImpl @Inject() (
  override val authConnector: DefaultAuthConnector,
  cc: MessagesControllerComponents
)(implicit
  ec: ExecutionContext,
  appConfig: ApplicationConfig
) extends AuthAction
    with AuthorisedFunctions
    with Logging {

  override val parser: BodyParser[AnyContent]               = cc.parsers.defaultBodyParser
  override protected val executionContext: ExecutionContext = cc.executionContext

  val saShuttered: Boolean = appConfig.saShuttered

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] =
    if (saShuttered) {
      Future.successful(Redirect(controllers.routes.ErrorController.serviceUnavailable))
    } else {
      implicit val hc: HeaderCarrier =
        HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      authorised(ConfidenceLevel.L50)
        .retrieve(
          Retrievals.allEnrolments and Retrievals.externalId and Retrievals.credentials and Retrievals.saUtr and Retrievals.confidenceLevel
        ) {
          case Enrolments(enrolments) ~ Some(externalId) ~ Some(credentials) ~ saUtr ~ confidenceLevel =>
            val agentRef: Option[Uar] = enrolments.find(_.key == "IR-SA-AGENT").flatMap { enrolment =>
              enrolment.identifiers
                .find(id => id.key == "IRAgentReference")
                .map(key => Uar(key.value))
            }

            val isAgentActive: Boolean = enrolments.find(_.key == "IR-SA-AGENT").map(_.isActivated).getOrElse(false)

            block {
              AuthenticatedRequest(
                externalId,
                agentRef,
                saUtr.map(s => SaUtr(s)),
                None,
                saUtr.isDefined,
                isAgentActive,
                confidenceLevel,
                credentials,
                request
              )
            }

          case _ => throw new RuntimeException("Can't find credentials for user")
        }
    } recover {
      case _: NoActiveSession =>
        lazy val ggSignIn    = appConfig.loginUrl
        lazy val callbackUrl = appConfig.loginCallback
        Redirect(
          ggSignIn,
          Map(
            "continue_url" -> Seq(callbackUrl),
            "origin"       -> Seq(appConfig.appName)
          )
        )

      case _: InsufficientEnrolments => Redirect(controllers.routes.ErrorController.notAuthorised)
    }
}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction
    extends ActionBuilder[AuthenticatedRequest, AnyContent]
    with ActionFunction[Request, AuthenticatedRequest]
