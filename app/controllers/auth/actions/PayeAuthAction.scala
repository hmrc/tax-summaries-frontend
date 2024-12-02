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
import controllers.auth.requests
import controllers.auth.requests.PayeAuthenticatedRequest
import models.admin.PAYEServiceToggle
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc._
import services.PertaxAuthService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}
class PayeAuthActionImpl @Inject() (
  override val authConnector: DefaultAuthConnector,
  cc: MessagesControllerComponents,
  pertaxAuthService: PertaxAuthService,
  featureFlagService: FeatureFlagService
)(implicit
  ec: ExecutionContext
) extends PayeAuthAction
    with AuthorisedFunctions
    with Logging {

  override val parser: BodyParser[AnyContent]               = cc.parsers.defaultBodyParser
  override protected val executionContext: ExecutionContext = cc.executionContext

  private def isAgent(enrolments: Set[Enrolment]): Boolean =
    enrolments.exists(_.key == "IR-SA-AGENT") &&
      enrolments.exists(_.identifiers.exists(_.key == "IRAgentReference"))

  private def isPayeEnabled: Future[Boolean] =
    featureFlagService.get(PAYEServiceToggle).map(_.isEnabled)

  override def invokeBlock[A](
    request: Request[A],
    block: PayeAuthenticatedRequest[A] => Future[Result]
  ): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    isPayeEnabled.flatMap {
      case true  => handleAuthorisation(request, block)
      case false => redirectToServiceUnavailable
    }
  }

  private def redirectToServiceUnavailable: Future[Result] =
    Future.successful(Redirect(controllers.paye.routes.PayeErrorController.serviceUnavailable))

  private def handleAuthorisation[A](
    request: Request[A],
    block: PayeAuthenticatedRequest[A] => Future[Result]
  )(implicit hc: HeaderCarrier): Future[Result] =
    pertaxAuthService.authorise[A, Request[A]](request).flatMap {
      case Some(result) => Future.successful(result)
      case None         => fetchUserDetails(request, block)
    }

  private def fetchUserDetails[A](
    request: Request[A],
    block: PayeAuthenticatedRequest[A] => Future[Result]
  )(implicit hc: HeaderCarrier): Future[Result] =
    authorised(ConfidenceLevel.L200)
      .retrieve(Retrievals.allEnrolments and Retrievals.nino and Retrievals.credentials) {
        case Enrolments(enrolments) ~ Some(_) ~ Some(_) if isAgent(enrolments) =>
          redirectToNotAuthorised
        case Enrolments(_) ~ Some(nino) ~ Some(credentials)                    =>
          block(
            requests.PayeAuthenticatedRequest(
              nino = Nino(nino),
              credentials = credentials,
              request = request
            )
          )
        case _                                                                 =>
          throw new RuntimeException("Retrieval succeeded but did not match expectation")
      }

  private def redirectToNotAuthorised: Future[Result] =
    Future.successful(Redirect(controllers.paye.routes.PayeErrorController.notAuthorised))
}

@ImplementedBy(classOf[PayeAuthActionImpl])
trait PayeAuthAction
    extends ActionBuilder[PayeAuthenticatedRequest, AnyContent]
    with ActionFunction[Request, PayeAuthenticatedRequest]
