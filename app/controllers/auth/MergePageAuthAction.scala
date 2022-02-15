/*
 * Copyright 2022 HM Revenue & Customs
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
import connectors.DataCacheConnector
import models.AgentToken
import play.api.mvc.Results.Redirect
import play.api.mvc._
import services.{CitizenDetailsService, SucccessMatchingDetailsResponse}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.domain.{Nino, SaUtr, Uar}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import utils.Globals

import scala.concurrent.{ExecutionContext, Future}

class MergePageAuthActionImpl @Inject()(
  citizenDetailsService: CitizenDetailsService,
  dataCacheConnector: DataCacheConnector,
  override val authConnector: DefaultAuthConnector,
  cc: MessagesControllerComponents)(implicit ec: ExecutionContext, appConfig: ApplicationConfig)
    extends MergePageAuthAction with AuthorisedFunctions {

  override val parser: BodyParser[AnyContent] = cc.parsers.defaultBodyParser
  override protected val executionContext: ExecutionContext = ec

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised(ConfidenceLevel.L50)
      .retrieve(
        Retrievals.allEnrolments and Retrievals.externalId and Retrievals.credentials and Retrievals.saUtr and Retrievals.nino and Retrievals.confidenceLevel) {
        case Enrolments(enrolments) ~ Some(externalId) ~ Some(credentials) ~ saUtr ~ nino ~ confidenceLevel => {

          val agentRef: Option[Uar] = enrolments.find(_.key == "IR-SA-AGENT").flatMap { enrolment =>
            enrolment.identifiers
              .find(id => id.key == "IRAgentReference")
              .map(key => Uar(key.value))
          }

          val isAgentActive: Boolean = enrolments.find(_.key == "IR-SA-AGENT").exists(_.isActivated)

          for {
            getAgentTokenCache <- dataCacheConnector.getAgentToken
            blockData <- executeAuthActions(
                          request,
                          block,
                          externalId,
                          credentials,
                          saUtr,
                          nino,
                          confidenceLevel,
                          agentRef,
                          isAgentActive,
                          getAgentTokenCache)
          } yield {
            blockData
          }
        }

        case _ => throw new RuntimeException("Can't find credentials for user")
      }
  } recover {
    case _: NoActiveSession => {
      lazy val ggSignIn = appConfig.loginUrl
      lazy val callbackUrl = appConfig.loginCallback
      Redirect(
        ggSignIn,
        Map(
          "continue_url" -> Seq(callbackUrl),
          "origin"       -> Seq(appConfig.appName)
        )
      )
    }

    case _: InsufficientEnrolments => {
      Redirect(controllers.routes.ErrorController.notAuthorised)
    }
  }

  private def executeAuthActions[A](
    request: Request[A],
    block: AuthenticatedRequest[A] => Future[Result],
    externalId: String,
    credentials: Credentials,
    saUtr: Option[String],
    nino: Option[String],
    confidenceLevel: ConfidenceLevel,
    agentRef: Option[Uar],
    isAgentActive: Boolean,
    agentToken: Option[AgentToken])(implicit hc: HeaderCarrier) =
    if (saUtr.isEmpty && nino.isEmpty && agentRef.isEmpty) {
      Future.successful(Redirect(controllers.routes.ErrorController.notAuthorised))
    } else {

      val authenticatedRequest = AuthenticatedRequest(
        externalId,
        agentRef,
        saUtr.map(SaUtr(_)),
        nino.map(Nino(_)),
        saUtr.nonEmpty,
        isAgentActive,
        confidenceLevel,
        credentials,
        request
      )

      val isAgentTokenMissing = isAgentActive && (request
        .getQueryString(Globals.TAXS_USER_TYPE_QUERY_PARAMETER)
        .isEmpty || request
        .getQueryString(Globals.TAXS_AGENT_TOKEN_ID)
        .isEmpty) &&
        (agentToken match {
          case None => true
          case _    => false
        })

      if (agentRef.isDefined && !isAgentActive) {
        Future(Redirect(controllers.routes.ErrorController.notAuthorised))
      } else if (isAgentActive && isAgentTokenMissing.equals(true)) {
        println("Token is empty...")
        Future(Redirect(controllers.routes.ErrorController.notAuthorised))
      } else if (saUtr.isEmpty && agentRef.isEmpty) {
        println("Executing block even if Token is empty...1")
        nino
          .map { n =>
            handleResponse(authenticatedRequest, n).flatMap(
              response => block(response)
            )
          }
          .getOrElse(block(authenticatedRequest))
      } else {
        println("Executing block even if Token is empty...2")
        block(authenticatedRequest)
      }
    }

  private def handleResponse[T](request: AuthenticatedRequest[T], nino: String)(
    implicit hc: HeaderCarrier): Future[AuthenticatedRequest[T]] =
    for {
      detailsResponse <- citizenDetailsService.getMatchingDetails(nino)
    } yield {
      detailsResponse match {
        case SucccessMatchingDetailsResponse(value) =>
          if (value.saUtr.isDefined) {
            createAuthenticatedRequest(request, value.saUtr)
          } else {
            request
          }
        case _ => request
      }
    }

  private def createAuthenticatedRequest[T](
    request: AuthenticatedRequest[T],
    newSaUtr: Option[SaUtr]): AuthenticatedRequest[T] =
    AuthenticatedRequest(
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

@ImplementedBy(classOf[MergePageAuthActionImpl])
trait MergePageAuthAction
    extends ActionBuilder[AuthenticatedRequest, AnyContent] with ActionFunction[Request, AuthenticatedRequest]
