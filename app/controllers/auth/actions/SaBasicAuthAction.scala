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

import com.google.inject.ImplementedBy
import config.ApplicationConfig
import controllers.auth.requests
import controllers.auth.requests.AuthenticatedRequest
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc._
import services.{CitizenDetailsService, SucccessMatchingDetailsResponse}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.domain.{SaUtr, Uar}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SaBasicAuthActionImpl @Inject() (
  citizenDetailsService: CitizenDetailsService,
  appConfig: ApplicationConfig,
  override val authConnector: DefaultAuthConnector,
  cc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends SaBasicAuthAction
    with AuthorisedFunctions
    with Logging {

  override val parser: BodyParser[AnyContent]               = cc.parsers.defaultBodyParser
  override protected val executionContext: ExecutionContext = cc.executionContext

  private val saShuttered: Boolean = appConfig.saShuttered

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] =
    if (saShuttered) {
      Future.successful(Redirect(controllers.routes.ErrorController.serviceUnavailable))
    } else {
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      authorised(ConfidenceLevel.L50).retrieve(
        Retrievals.allEnrolments and Retrievals.externalId and Retrievals.credentials and Retrievals.saUtr and Retrievals.confidenceLevel
      ) {
        case Enrolments(enrolments) ~ Some(externalId) ~ Some(credentials) ~ saUtr ~ confidenceLevel =>
          val (agentRef, isAgentActive) = agentInfo(enrolments)
          processAuth(agentRef, isAgentActive, saUtr) { saUtr =>
            block(
              requests.AuthenticatedRequest(
                userId = externalId,
                agentRef = agentRef,
                saUtr = saUtr,
                nino = None,
                isSa = saUtr.isDefined,
                isAgentActive = isAgentActive,
                confidenceLevel = confidenceLevel,
                credentials = credentials,
                request = request
              )
            )
          }
        case _                                                                                       => throw new RuntimeException("Can't find credentials for user")
      } recover {
        case _: NoActiveSession        =>
          Redirect(
            appConfig.loginUrl,
            Map(
              "continue_url" -> Seq(appConfig.loginCallback),
              "origin"       -> Seq(appConfig.appName)
            )
          )
        case _: InsufficientEnrolments => notAuthorisedPage
      }
    }

  private def processAuth(agentRef: Option[Uar], isAgentActive: Boolean, saUtr: Option[String])(
    block: Option[SaUtr] => Future[Result]
  )(implicit hc: HeaderCarrier): Future[Result] =
    (agentRef, isAgentActive, saUtr) match {
      case (Some(_), true, _)  => block(saUtr.map(SaUtr)) // Active agent
      case (Some(_), false, _) => Future.successful(notAuthorisedPage) // Inactive agent
      case (None, _, _)        => // Not an agent
        getSAUTRFromCitizenDetails.flatMap {
          case optUTR @ Some(_) => block(optUTR)
          case None             => Future.successful(notAuthorisedPage)
        } recover {
          case _: InsufficientConfidenceLevel =>
            Redirect(
              appConfig.identityVerificationUpliftUrl,
              Map(
                "origin"          -> Seq(appConfig.appName),
                "confidenceLevel" -> Seq(ConfidenceLevel.L200.toString),
                "completionURL"   -> Seq(appConfig.loginCallback),
                "failureURL"      -> Seq(appConfig.iVUpliftFailureCallback)
              )
            )
          case _: IncorrectCredentialStrength => notAuthorisedPage
        }
    }

  private def notAuthorisedPage: Result = Redirect(controllers.routes.ErrorController.notAuthorised)

  private def getSAUTRFromCitizenDetails(implicit hc: HeaderCarrier): Future[Option[SaUtr]] =
    authorised(ConfidenceLevel.L200 and CredentialStrength(CredentialStrength.strong)).retrieve(Retrievals.nino) {
      case Some(nino) =>
        citizenDetailsService.getMatchingDetails(nino).map {
          case SucccessMatchingDetailsResponse(matchingDetails) =>
            matchingDetails.saUtr match {
              case Some(_) => matchingDetails.saUtr
              case _       => None
            }
          case _                                                => None
        }
      case _          => Future.successful(None)
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
  //
  //            val agentRef: Option[Uar] = enrolments.find(_.key == "IR-SA-AGENT").flatMap { enrolment =>
  //              enrolment.identifiers
  //                .find(id => id.key == "IRAgentReference")
  //                .map(key => Uar(key.value))
  //            }
  //            val isAgentActive: Boolean = enrolments.find(_.key == "IR-SA-AGENT").exists(_.isActivated)
}

@ImplementedBy(classOf[SaBasicAuthActionImpl])
trait SaBasicAuthAction
    extends ActionBuilder[AuthenticatedRequest, AnyContent]
    with ActionFunction[Request, AuthenticatedRequest]
