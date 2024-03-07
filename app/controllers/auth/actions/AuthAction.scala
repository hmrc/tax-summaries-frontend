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
import connectors.DataCacheConnector
import controllers.auth.requests
import controllers.auth.requests.AuthenticatedRequest
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc._
import services.{CitizenDetailsService, FailedMatchingDetailsResponse, PertaxAuthService, SucccessMatchingDetailsResponse}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.domain.{Nino, SaUtr, Uar}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.Globals

import scala.concurrent.{ExecutionContext, Future}

class AuthImpl(
  override val authConnector: DefaultAuthConnector,
  cc: MessagesControllerComponents,
  dataCacheConnector: DataCacheConnector,
  citizenDetailsService: CitizenDetailsService,
  pertaxAuthService: PertaxAuthService,
  saShutterCheck: Boolean,
  agentTokenCheck: Boolean,
  utrCheck: Boolean
)(implicit
  ec: ExecutionContext,
  appConfig: ApplicationConfig
) extends Auth
    with AuthorisedFunctions
    with Logging {

  protected val saShuttered: Boolean = appConfig.saShuttered

  override val parser: BodyParser[AnyContent]               = cc.parsers.defaultBodyParser
  override protected val executionContext: ExecutionContext = cc.executionContext

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    if (saShutterCheck && saShuttered) {
      Future.successful(Redirect(controllers.routes.ErrorController.serviceUnavailable))
    } else {
      createAuthenticatedRequest(request).flatMap {
        case Right(authenticatedRequest) =>
          citizenDetailsCheck(authenticatedRequest).flatMap { authReq =>
            (utrCheck, authReq.isAgent, authReq.saUtr) match {
              case (true, false, None) => Future.successful(notAuthorisedPage)
              case _                   => block(authReq)
            }
          }
        case Left(r)                     => Future.successful(r)
      }
    }
  }

  private def agentTokenCheck[A](request: Request[A], rq: => AuthenticatedRequest[A])(implicit
    hc: HeaderCarrier
  ): Future[Either[Result, AuthenticatedRequest[A]]] =
    if (agentTokenCheck) {
      dataCacheConnector.getAgentToken.map { agentToken =>
        if (
          (request
            .getQueryString(Globals.TAXS_USER_TYPE_QUERY_PARAMETER)
            .isEmpty || request
            .getQueryString(Globals.TAXS_AGENT_TOKEN_ID)
            .isEmpty) &&
          agentToken.isEmpty
        ) {
          Left(notAuthorisedPage)
        } else {
          Right(rq)
        }
      }
    } else {
      Future.successful(Right(rq))
    }

  private def createAuthenticatedRequest[A](request: Request[A])(implicit
    hc: HeaderCarrier
  ): Future[Either[Result, AuthenticatedRequest[A]]] =
    authorised(ConfidenceLevel.L50)
      .retrieve(
        Retrievals.allEnrolments and Retrievals.externalId and Retrievals.credentials and Retrievals.saUtr and Retrievals.nino and Retrievals.confidenceLevel
      ) {
        case Enrolments(enrolments) ~ Some(externalId) ~ Some(credentials) ~ saUtr ~ nino ~ confidenceLevel =>
          val (agentRef, isAgentActive)           = agentInfo(enrolments)
          def newRequest: AuthenticatedRequest[A] =
            requests.AuthenticatedRequest(
              userId = externalId,
              agentRef = agentRef,
              saUtr = saUtr.map(SaUtr),
              nino = nino.map(Nino(_)),
              isAgentActive = isAgentActive,
              confidenceLevel = confidenceLevel,
              credentials = credentials,
              request = request
            )
          (agentRef.isDefined, isAgentActive) match {
            case (true, false) =>
              Future.successful(Left(Redirect(controllers.routes.ErrorController.notAuthorised)))
            case (true, true)  =>
              agentTokenCheck(request, newRequest)
            case _             =>
              pertaxAuthService.authorise[A, Request[A]](request).map {
                case Some(r) => Left(r)
                case _       => Right(newRequest)
              }
          }
        case _                                                                                              => throw new RuntimeException("Can't find credentials for user")
      } recover {
      case _: NoActiveSession        =>
        lazy val ggSignIn    = appConfig.loginUrl
        lazy val callbackUrl = appConfig.loginCallback
        Left(
          Redirect(
            ggSignIn,
            Map(
              "continue_url" -> Seq(callbackUrl),
              "origin"       -> Seq(appConfig.appName)
            )
          )
        )
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

  private def citizenDetailsCheck[A](request: AuthenticatedRequest[A])(implicit
    hc: HeaderCarrier
  ): Future[AuthenticatedRequest[A]] =
    (request.nino, request.saUtr, request.isAgent) match {
      case (Some(nino), None, false) =>
        getSAUTRFromCitizenDetails(nino).map {
          case retrievedSAUtr @ Some(_) => request.copy(saUtr = retrievedSAUtr)
          case None                     => request
        }
      case _                         => Future.successful(request)
    }

  private def getSAUTRFromCitizenDetails(nino: Nino)(implicit hc: HeaderCarrier): Future[Option[SaUtr]] =
    citizenDetailsService.getMatchingDetails(nino.nino).map {
      case SucccessMatchingDetailsResponse(matchingDetails) =>
        matchingDetails.saUtr match {
          case Some(_) => matchingDetails.saUtr
          case _       => None
        }
      case FailedMatchingDetailsResponse                                                => None
    }

  private def notAuthorisedPage: Result = Redirect(controllers.routes.ErrorController.notAuthorised)
}

@ImplementedBy(classOf[AuthImpl])
trait Auth extends ActionBuilder[AuthenticatedRequest, AnyContent] with ActionFunction[Request, AuthenticatedRequest]

class AuthAction @Inject() (
  authConnector: DefaultAuthConnector,
  cc: MessagesControllerComponents,
  dataCacheConnector: DataCacheConnector,
  citizenDetailsService: CitizenDetailsService,
  pertaxAuthService: PertaxAuthService
)(implicit
  ec: ExecutionContext,
  appConfig: ApplicationConfig
) {
  def apply(saShutterCheck: Boolean, agentTokenCheck: Boolean, utrCheck: Boolean): Auth =
    new AuthImpl(
      authConnector,
      cc,
      dataCacheConnector,
      citizenDetailsService,
      pertaxAuthService,
      saShutterCheck,
      agentTokenCheck,
      utrCheck
    )
}
