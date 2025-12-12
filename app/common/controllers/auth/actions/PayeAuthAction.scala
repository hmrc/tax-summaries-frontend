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

package common.controllers.auth.actions

import com.google.inject.Inject
import common.controllers.routes
import common.models.admin.PAYEServiceToggle
import common.models.requests
import common.models.requests.*
import play.api.Logging
import play.api.mvc.*
import play.api.mvc.Results.Redirect
import common.services.PertaxAuthService
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import common.utils.TaxYearUtil

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}
class PayeAuthActionImpl(
  override val authConnector: DefaultAuthConnector,
  cc: MessagesControllerComponents,
  pertaxAuthService: PertaxAuthService,
  featureFlagService: FeatureFlagService,
  taxYearUtil: TaxYearUtil,
  taxYear: Int
)(implicit ec: ExecutionContext)
    extends ActionBuilder[PayeAuthenticatedRequest, AnyContent]
    with ActionFunction[Request, PayeAuthenticatedRequest]
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
    if (taxYearUtil.isValidTaxYear(taxYear)) {
      isPayeEnabled.flatMap {
        case true  => handleAuthorisation(request, block)
        case false => redirectToServiceUnavailable
      }
    } else {
      Future.successful(Redirect(routes.ErrorController.authorisedNoAts(taxYear)))
    }
  }

  private def redirectToServiceUnavailable: Future[Result] =
    Future.successful(Redirect(paye.controllers.routes.PayeErrorController.serviceUnavailable))

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
    Future.successful(Redirect(paye.controllers.routes.PayeErrorController.notAuthorised))
}

@Singleton
class PayeAuthAction @Inject() (
  authConnector: DefaultAuthConnector,
  cc: MessagesControllerComponents,
  pertaxAuthService: PertaxAuthService,
  featureFlagService: FeatureFlagService,
  taxYearUtil: TaxYearUtil
)(implicit
  ec: ExecutionContext
) {
  def apply(taxYear: Int) =
    new PayeAuthActionImpl(authConnector, cc, pertaxAuthService, featureFlagService, taxYearUtil, taxYear)
}
