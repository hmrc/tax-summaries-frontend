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

class SelfAssessmentActionImpl @Inject() (
  citizenDetailsService: CitizenDetailsService,
  ninoAuthAction: NinoAuthAction,
  appConfig: ApplicationConfig,
  override val authConnector: DefaultAuthConnector,
  cc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends SelfAssessmentAction
    with AuthorisedFunctions
    with Logging {

  override val parser: BodyParser[AnyContent]               = cc.parsers.defaultBodyParser
  override protected val executionContext: ExecutionContext = cc.executionContext

  val saShuttered: Boolean                                                                                           = appConfig.saShuttered
  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] =
    if (saShuttered) {
      Future.successful(Redirect(controllers.routes.ErrorController.serviceUnavailable))
    } else {
      implicit val hc: HeaderCarrier =
        HeaderCarrierConverter.fromRequestAndSession(request, request.session)

// Below is what is in pertax backend authorise:-
//def authorise(): Action[AnyContent] = authenticate.async { implicit request =>
//  implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request.request)
//  request.version match {
//    case "2.0" =>
//      val responseF = (for {
//        _ <- authorisationUtils.checkAffinity(request)
//        _ <- authorisationUtils.checkConfidenceLevel(request)
//        _ <- authorisationUtils.checkCredentialStrength(request)
//        _ <- authorisationUtils.checkDesignatoryDetails(request)
//        _ <- enrolmentChecks.checkSingleAccountEnrolment(request)
//      } yield ApiResponse("ACCESS_GRANTED", "Access granted", reportAs = OK)).merge
//
//      responseF.map(response => Status(response.reportAs)(response))
//
//    case _ =>
//      Future.successful(
//        ApiResponse("INVALID_API_VERSION", "Invalid version of the API", reportAs = NOT_IMPLEMENTED).toResult
//      )
//  }
//}

// Below is the original auth action:-
      println("\nAA1")
      authorised(ConfidenceLevel.L50)
        .retrieve(
          Retrievals.allEnrolments and Retrievals.externalId and Retrievals.credentials and Retrievals.saUtr and Retrievals.confidenceLevel
        ) {
          case Enrolments(enrolments) ~ Some(externalId) ~ Some(credentials) ~ saUtr ~ confidenceLevel =>
            println("\nAA2")
            val agentRef: Option[Uar] = enrolments.find(_.key == "IR-SA-AGENT").flatMap { enrolment =>
              enrolment.identifiers
                .find(id => id.key == "IRAgentReference")
                .map(key => Uar(key.value))
            }

            val isAgentActive: Boolean = enrolments.find(_.key == "IR-SA-AGENT").exists(_.isActivated)

            val rq = requests.AuthenticatedRequest(
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

            implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
// Below is the original selfassessmentaction

            (agentRef, isAgentActive, saUtr) match {
              case (Some(_), false, _) => Future.successful(Redirect(controllers.routes.ErrorController.notAuthorised))
              case (None, _, _) => // Not an agent: get the nino:-
                ninoAuthAction.getNino().flatMap { atsNinoResponse =>
                  println("\natsNinoResponse=" + atsNinoResponse)
                  handleResponse(rq, atsNinoResponse).flatMap {
                    case Left(r)  => Future.successful(r)
                    case Right(r) => block(r)
                  }
                }


//
//                val x = authorised(ConfidenceLevel.L200 and CredentialStrength(CredentialStrength.strong)).retrieve(Retrievals.nino) {
//                  case Some(nino) => Future(SuccessAtsNino(nino))
//                  case _          => Future(NoAtsNinoFound)
//                } recover {
//                  case _: InsufficientConfidenceLevel => UpliftRequiredAtsNino
//                  case _: IncorrectCredentialStrength => InsufficientCredsNino
//                }
              
              
              
              case _ => block(rq)
            }
            
            
//            if (agentRef.isDefined && !isAgentActive) {
//              println("\n**** IS AGENT")
//              Future.successful(Redirect(controllers.routes.ErrorController.notAuthorised))
//            } else if (saUtr.isEmpty && agentRef.isEmpty) {
//              println("\n**** IS NOT AGENT 1")
//              ninoAuthAction.getNino().flatMap { atsNinoResponse =>
//                println("\natsNinoResponse=" + atsNinoResponse)
//                handleResponse(rq, atsNinoResponse).flatMap {
//                  case Left(r)  => Future.successful(r)
//                  case Right(r) => block(r)
//                }
//              }
//            } else {
//              println("\n**** IS NOT AGENT - 2" + saUtr + "/" + agentRef)
//              block(rq)
//            }
          case _                                                                                       => throw new RuntimeException("Can't find credentials for user")
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

  private def handleResponse[T](request: AuthenticatedRequest[T], atsNinoResponse: AtsNino)(implicit
    hc: HeaderCarrier
  ): Future[Either[Result, AuthenticatedRequest[T]]] =
    atsNinoResponse match {
      case SuccessAtsNino(nino)  =>
        citizenDetailsService.getMatchingDetails(nino).map {
          case SucccessMatchingDetailsResponse(value) =>
            if (value.saUtr.isDefined) { Right(createAuthenticatedRequest(request, value.saUtr)) }
            else {
              Left(Redirect(controllers.routes.ErrorController.notAuthorised))
            }
          case _                                      =>
            Left(
              Redirect(controllers.routes.ErrorController.notAuthorised)
            )
        }
      case NoAtsNinoFound        =>
        println("\nNO NINO FOUND REDIRECT")
        Future(
          Left(
            Redirect(controllers.routes.ErrorController.notAuthorised)
          )
        )
      case InsufficientCredsNino =>
        Future(
          Left(
            Redirect(controllers.routes.ErrorController.notAuthorised)
          )
        )
      case UpliftRequiredAtsNino =>
        Future(
          Left(
            Redirect(
              appConfig.identityVerificationUpliftUrl,
              Map(
                "origin"          -> Seq(appConfig.appName),
                "confidenceLevel" -> Seq(ConfidenceLevel.L200.toString),
                "completionURL"   -> Seq(appConfig.loginCallback),
                "failureURL"      -> Seq(appConfig.iVUpliftFailureCallback)
              )
            )
          )
        )
    }

  private def createAuthenticatedRequest[T](
    request: AuthenticatedRequest[T],
    newSaUtr: Option[SaUtr]
  ): AuthenticatedRequest[T] =
    requests.AuthenticatedRequest(
      userId = request.userId,
      agentRef = request.agentRef,
      saUtr = newSaUtr,
      nino = request.nino,
      isSa = request.isSa,
      isAgentActive = request.isAgentActive,
      confidenceLevel = request.confidenceLevel,
      credentials = request.credentials,
      request = request
    )
}

@ImplementedBy(classOf[SelfAssessmentActionImpl])
trait SelfAssessmentAction
    extends ActionBuilder[AuthenticatedRequest, AnyContent]
    with ActionFunction[Request, AuthenticatedRequest]
