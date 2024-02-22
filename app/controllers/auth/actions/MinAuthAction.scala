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

import com.google.inject.{ImplementedBy, Inject}
import config.ApplicationConfig
import controllers.auth.requests
import controllers.auth.requests.AuthenticatedRequest
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.domain.{Nino, Uar}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class MinAuthActionImpl @Inject() (
  override val authConnector: DefaultAuthConnector,
  cc: MessagesControllerComponents
)(implicit
  ec: ExecutionContext,
  appConfig: ApplicationConfig
) extends MinAuthAction
    with AuthorisedFunctions {

  override val parser: BodyParser[AnyContent]               = cc.parsers.defaultBodyParser
  override protected val executionContext: ExecutionContext = cc.executionContext

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised(ConfidenceLevel.L50)
      .retrieve(
        Retrievals.allEnrolments and Retrievals.externalId and Retrievals.credentials and Retrievals.saUtr and Retrievals.nino and Retrievals.confidenceLevel
      ) {
        case Enrolments(enrolments) ~ Some(externalId) ~ Some(credentials) ~ saUtr ~ nino ~ confidenceLevel =>
          val (agentRef, isAgentActive) = agentInfo(enrolments)

          block(
            requests.AuthenticatedRequest(
              userId = externalId,
              agentRef = agentRef,
              saUtr = None,
              nino = nino.map(Nino(_)),
              isSa = saUtr.isDefined,
              isAgentActive = isAgentActive,
              confidenceLevel = confidenceLevel,
              credentials = credentials,
              request = request
            )
          )

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

    case _: InsufficientEnrolments => throw InsufficientEnrolments("")
  }

  private def agentInfo(enrolments: Set[Enrolment]): (Option[Uar], Boolean) =
    enrolments
      .find(_.key == "IR-SA-AGENT")
      .map { enrolment =>
        val d = enrolment.identifiers
          .find(id => id.key == "IRAgentReference")
          .map(key => Uar(key.value))
        Tuple2(d, enrolment.isActivated)
      }
      .getOrElse(Tuple2(None, false))
}

@ImplementedBy(classOf[MinAuthActionImpl])
trait MinAuthAction
    extends ActionBuilder[AuthenticatedRequest, AnyContent]
    with ActionFunction[Request, AuthenticatedRequest]