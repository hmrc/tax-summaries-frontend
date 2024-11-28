/*
 * Copyright 2024 HM Revenue & Customs
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

import cats.data.EitherT
import cats.implicits._
import com.google.inject.{ImplementedBy, Inject}
import config.ApplicationConfig
import controllers.auth.requests
import controllers.auth.requests.AuthenticatedRequest
import models.AgentToken
import models.admin.SelfAssessmentServiceToggle
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc._
import repository.TaxsAgentTokenSessionCacheRepository
import services._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.domain.{Nino, SaUtr, Uar}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.DataKey
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.Globals

import scala.concurrent.{ExecutionContext, Future}
class AuthImpl(
  override val authConnector: DefaultAuthConnector,
  cc: MessagesControllerComponents,
  taxsAgentTokenSessionCacheRepository: TaxsAgentTokenSessionCacheRepository,
  citizenDetailsService: CitizenDetailsService,
  pertaxAuthService: PertaxAuthService,
  featureFlagService: FeatureFlagService,
  saShutterCheck: Boolean,
  agentTokenCheck: Boolean,
  utrCheck: Boolean
)(implicit
  ec: ExecutionContext,
  appConfig: ApplicationConfig
) extends Auth
    with AuthorisedFunctions
    with Logging {

  override val parser: BodyParser[AnyContent]               = cc.parsers.defaultBodyParser
  override protected val executionContext: ExecutionContext = cc.executionContext

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    if (saShutterCheck) {
      isSaEnabled.flatMap {
        case true  => handleRequest(request, block)
        case false => Future.successful(serviceUnavailablePage)
      }
    } else {
      handleRequest(request, block)
    }
  }

  private def isSaEnabled: Future[Boolean] =
    featureFlagService.get(SelfAssessmentServiceToggle).map(_.isEnabled)

  private def handleRequest[A](
    request: Request[A],
    block: AuthenticatedRequest[A] => Future[Result]
  )(implicit hc: HeaderCarrier): Future[Result] =
    createAuthenticatedRequest(request).flatMap {
      case Right(authenticatedRequest) =>
        validateCitizenDetails(authenticatedRequest).flatMap {
          case Left(result)   => Future.successful(result)
          case Right(authReq) => validateUtrCheck(authReq, block)
        }
      case Left(result)                => Future.successful(result)
    }

  private def validateUtrCheck[A](
    authReq: AuthenticatedRequest[A],
    block: AuthenticatedRequest[A] => Future[Result]
  ): Future[Result] =
    (utrCheck, authReq.isAgent, authReq.saUtr) match {
      case (true, false, None) => Future.successful(notAuthorisedPage)
      case _                   => block(authReq)
    }

  private def agentTokenCheck[A](
    request: Request[A],
    rq: => AuthenticatedRequest[A]
  )(implicit hc: HeaderCarrier): Future[Either[Result, AuthenticatedRequest[A]]] =
    if (agentTokenCheck) {
      taxsAgentTokenSessionCacheRepository.getFromSession[AgentToken](DataKey(Globals.TAXS_AGENT_TOKEN_KEY)).map {
        case Some(_)                                  => Right(rq)
        case None if missingAgentTokenParams(request) => Left(notAuthorisedPage)
        case None                                     => Right(rq)
      }
    } else {
      Future.successful(Right(rq))
    }

  private def missingAgentTokenParams[A](request: Request[A]): Boolean =
    request.getQueryString(Globals.TAXS_USER_TYPE_QUERY_PARAMETER).isEmpty ||
      request.getQueryString(Globals.TAXS_AGENT_TOKEN_ID).isEmpty

  private def createAuthenticatedRequest[A](request: Request[A])(implicit
    hc: HeaderCarrier
  ): Future[Either[Result, AuthenticatedRequest[A]]] =
    authorised(ConfidenceLevel.L50)
      .retrieve(
        Retrievals.allEnrolments and Retrievals.externalId and Retrievals.credentials and Retrievals.saUtr and Retrievals.nino and Retrievals.confidenceLevel
      ) {
        case Enrolments(enrolments) ~ Some(externalId) ~ Some(credentials) ~ saUtr ~ nino ~ confidenceLevel =>
          val (agentRef, isAgentActive) = extractAgentInfo(enrolments)

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
            case (true, false) => Future.successful(Left(notAuthorisedPage))
            case (true, true)  => agentTokenCheck(request, newRequest)
            case _             => validatePertaxAuth(request, newRequest)
          }
        case _                                                                                              => Future.failed(new RuntimeException("Can't find credentials for user"))
      } recover { case _: NoActiveSession =>
      Left(loginRedirect)
    }

  private def validatePertaxAuth[A](
    request: Request[A],
    newRequest: AuthenticatedRequest[A]
  ): Future[Either[Result, AuthenticatedRequest[A]]] =
    pertaxAuthService.authorise[A, Request[A]](request).map {
      case Some(result) => Left(result)
      case None         => Right(newRequest)
    }

  private def extractAgentInfo(enrolments: Set[Enrolment]): (Option[Uar], Boolean) =
    enrolments
      .find(_.key == "IR-SA-AGENT")
      .map { enrolment =>
        val agentReference = enrolment.identifiers.find(_.key == "IRAgentReference").map(key => Uar(key.value))
        (agentReference, enrolment.isActivated)
      }
      .getOrElse((None, false))

  private def validateCitizenDetails[A](
    request: AuthenticatedRequest[A]
  )(implicit hc: HeaderCarrier): Future[Either[Result, AuthenticatedRequest[A]]] =
    citizenDetailsCheck(request).value

  private def citizenDetailsCheck[A](
    request: AuthenticatedRequest[A]
  )(implicit hc: HeaderCarrier): EitherT[Future, Result, AuthenticatedRequest[A]] =
    (request.nino, request.saUtr, request.isAgent) match {
      case (Some(nino), None, false) =>
        citizenDetailsService
          .getMatchingSaUtr(nino.nino)
          .bimap(
            _ => serviceUnavailablePage,
            maybeSaUtr => request.copy(saUtr = maybeSaUtr)
          )
      case _                         => EitherT.rightT(request)
    }

  private def notAuthorisedPage: Result = Redirect(controllers.routes.ErrorController.notAuthorised)

  private def serviceUnavailablePage: Result = Redirect(controllers.routes.ErrorController.serviceUnavailable)

  private def loginRedirect: Result =
    Redirect(
      appConfig.loginUrl,
      Map(
        "continue_url" -> Seq(appConfig.loginCallback),
        "origin"       -> Seq(appConfig.appName)
      )
    )
}

@ImplementedBy(classOf[AuthImpl])
trait Auth extends ActionBuilder[AuthenticatedRequest, AnyContent] with ActionFunction[Request, AuthenticatedRequest]

class AuthAction @Inject() (
  authConnector: DefaultAuthConnector,
  cc: MessagesControllerComponents,
  taxsAgentTokenSessionCacheRepository: TaxsAgentTokenSessionCacheRepository,
  citizenDetailsService: CitizenDetailsService,
  pertaxAuthService: PertaxAuthService,
  featureFlagService: FeatureFlagService
)(implicit
  ec: ExecutionContext,
  appConfig: ApplicationConfig
) {
  def apply(saShutterCheck: Boolean, agentTokenCheck: Boolean, utrCheck: Boolean): Auth =
    new AuthImpl(
      authConnector,
      cc,
      taxsAgentTokenSessionCacheRepository,
      citizenDetailsService,
      pertaxAuthService,
      featureFlagService,
      saShutterCheck,
      agentTokenCheck,
      utrCheck
    )
}
