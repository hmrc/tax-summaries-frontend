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
import play.api.mvc.Results.Redirect
import play.api.mvc._
import services.{CitizenDetailsService, PertaxAuthService, SucccessMatchingDetailsResponse}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.domain.{Nino, SaUtr, Uar}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.Globals

import scala.concurrent.{ExecutionContext, Future}

class SaAndAgentAuthImpl @Inject() (
  override val authConnector: DefaultAuthConnector,
  cc: MessagesControllerComponents,
  featureFlagService: FeatureFlagService,
  dataCacheConnector: DataCacheConnector,
  citizenDetailsService: CitizenDetailsService,
  pertaxAuthService: PertaxAuthService,
  shutterCheck: Boolean,
  agentTokenCheck: Boolean,
  utrCheck: Boolean
)(implicit
  ec: ExecutionContext,
  appConfig: ApplicationConfig
) extends SaAndAgentAuth
    with AuthorisedFunctions {

  private val saShuttered: Boolean = appConfig.saShuttered

  override val parser: BodyParser[AnyContent]               = cc.parsers.defaultBodyParser
  override protected val executionContext: ExecutionContext = cc.executionContext

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    if (shutterCheck && saShuttered) {
      Future.successful(Redirect(controllers.routes.ErrorController.serviceUnavailable))
    } else {
      createAuthenticatedRequest(request).flatMap {
        case Right(authenticatedRequest) =>
          isActiveAgentButTokenMissing(agentTokenCheck, authenticatedRequest).flatMap {
            case true  => Future.successful(Redirect(controllers.routes.ErrorController.notAuthorised))
            case false =>
              citizenDetailsCheck(authenticatedRequest).flatMap { authReq =>
                pertaxAuthService.authorise[A, AuthenticatedRequest[A]](authReq).flatMap {
                  case None =>
                    (utrCheck, authReq.agentRef, authReq.saUtr) match {
                      case (true, None, None) => Future.successful(notAuthorisedPage)
                      case _            =>
                        block(authReq)
                    }

                  case Some(r) => Future.successful(r)
                }
              }
          }
        case Left(r)                     => Future.successful(r)
      }
    }
  }

  private def isActiveAgentButTokenMissing[A](agentTokenCheck: Boolean, authenticatedRequest: AuthenticatedRequest[A])(
    implicit hc: HeaderCarrier
  ): Future[Boolean] =
    if (agentTokenCheck) {
      dataCacheConnector.getAgentToken.map { agentToken =>
        authenticatedRequest.isAgentActive && (authenticatedRequest
          .getQueryString(Globals.TAXS_USER_TYPE_QUERY_PARAMETER)
          .isEmpty || authenticatedRequest
          .getQueryString(Globals.TAXS_AGENT_TOKEN_ID)
          .isEmpty) &&
        agentToken.isDefined
      }
    } else {
      Future.successful(true)
    }

  private def createAuthenticatedRequest[A](request: Request[A])(implicit
    hc: HeaderCarrier
  ): Future[Either[Result, AuthenticatedRequest[A]]] =
    authorised(ConfidenceLevel.L50)
      .retrieve(
        Retrievals.allEnrolments and Retrievals.externalId and Retrievals.credentials and Retrievals.saUtr and Retrievals.nino and Retrievals.confidenceLevel
      ) {
        case Enrolments(enrolments) ~ Some(externalId) ~ Some(credentials) ~ saUtr ~ nino ~ confidenceLevel =>
          val (agentRef, isAgentActive) = agentInfo(enrolments)

          //          featureFlagService.get(PertaxBackendToggle).flatMap { toggle =>
          //            if (toggle.isEnabled) {
          //
          //            }
          //          }

          // TODO: If agent L50 is enough, if non-agent need to get L200 and uplift if need to

          // If toggle off:-
          //    authorised(ConfidenceLevel.L200 and AuthNino(hasNino = true) and CredentialStrength(CredentialStrength.strong))
          //      .retrieve(Retrievals.allEnrolments and Retrievals.nino and Retrievals.credentials) {
          //        case enrolments ~ Some(nino) ~ Some(credentials) =>
          //          block {
          //            requests.PayeAuthenticatedRequest(
          //              Nino(nino),
          //              enrolments.getEnrolment("IR-SA").isDefined,
          //              credentials,
          //              request
          //            )
          //          }
          //        case _                                           => throw new RuntimeException("Auth retrieval failed for user")
          //      } recover {
          //      case _: NoActiveSession => // Done also by backend pertax auth
          //        Redirect(
          //          appConfig.payeLoginUrl,
          //          Map(
          //            "continue_url" -> Seq(appConfig.payeLoginCallbackUrl),
          //            "origin"       -> Seq(appConfig.appName)
          //          )
          //        )
          //
          //      case _: InsufficientConfidenceLevel =>
          //        upliftConfidenceLevel
          //      case NonFatal(e)                    =>
          //        logger.error(s"Exception in PayeAuthAction: $e", e)
          //        Redirect(controllers.paye.routes.PayeErrorController.notAuthorised)
          //    }

          // If toggle on AND NOT agent:-
          //    pertaxAuthService.authorise[A, AuthenticatedRequest[A]](request).map{
          //      case None => Right(request)
          //      case Some(r) => Left(r)
          //    }

          val authenticatedRequest: AuthenticatedRequest[A] = requests.AuthenticatedRequest(
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

          Future.successful(Right(authenticatedRequest))

        case _ => throw new RuntimeException("Can't find credentials for user")

      } recover {
      case _: NoActiveSession =>
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

  private def citizenDetailsCheck[A](request: AuthenticatedRequest[A])(implicit
    hc: HeaderCarrier
  ): Future[AuthenticatedRequest[A]] =
    (request.nino, request.saUtr, request.agentRef) match {
      case (Some(nino), None, None) =>
        println("\na1:" + request.saUtr)
        getSAUTRFromCitizenDetails(nino).map {
          case retrievedSAUtr @ Some(_) => request.copy(saUtr = retrievedSAUtr)
          case None                     =>
            println("\na2")
            request
        }
      case _                        => Future.successful(request)
    }

  private def getSAUTRFromCitizenDetails(nino: Nino)(implicit hc: HeaderCarrier): Future[Option[SaUtr]] = {
    println("\nGETTING MATCHING DEETS:" + nino.nino)
    citizenDetailsService.getMatchingDetails(nino.nino).map {
      case SucccessMatchingDetailsResponse(matchingDetails) =>
        matchingDetails.saUtr match {
          case Some(_) =>
            println("\nRETURNED SAUTR")
            matchingDetails.saUtr
          case _       =>
            println("\nRETURNED NONE(2)")
            None
        }
      case _                                                =>
        println("\nRETURNED NONE")
        None
    }
  }

  private def notAuthorisedPage: Result = Redirect(controllers.routes.ErrorController.notAuthorised)
}

@ImplementedBy(classOf[SaAndAgentAuthImpl])
trait SaAndAgentAuth extends ActionBuilder[AuthenticatedRequest, AnyContent] with ActionFunction[Request, AuthenticatedRequest]

class SaAndAgentAuthAction @Inject() (
  authConnector: DefaultAuthConnector,
  cc: MessagesControllerComponents,
  featureFlagService: FeatureFlagService,
  dataCacheConnector: DataCacheConnector,
  citizenDetailsService: CitizenDetailsService,
  pertaxAuthService: PertaxAuthService
)(implicit
  ec: ExecutionContext,
  appConfig: ApplicationConfig
) {
  def apply(shutterCheck: Boolean, agentTokenCheck: Boolean, utrCheck: Boolean): SaAndAgentAuth =
    new SaAndAgentAuthImpl(
      authConnector,
      cc,
      featureFlagService,
      dataCacheConnector,
      citizenDetailsService,
      pertaxAuthService,
      shutterCheck,
      agentTokenCheck,
      utrCheck
    )
}
