/*
 * Copyright 2021 HM Revenue & Customs
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

import com.google.inject.ImplementedBy
import config.ApplicationConfig
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import services.{CitizenDetailsService, SucccessMatchingDetailsResponse}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}

class SelfAssessmentActionImpl @Inject()(
  citizenDetailsService: CitizenDetailsService,
  ninoAuthAction: NinoAuthAction,
  appConfig: ApplicationConfig)(implicit ec: ExecutionContext)
    extends SelfAssessmentAction {

  override protected def refine[A](
    request: AuthenticatedRequest[A]): Future[Either[Result, AuthenticatedRequest[A]]] = {

    implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    if (request.saUtr.isEmpty) {
      ninoAuthAction.getNino().flatMap { atsNinoResponse =>
        handleResponse(request, atsNinoResponse)
      }
    } else {
      Future(Right(request))
    }
  }

  override protected def executionContext: ExecutionContext = ec

  private def handleResponse[T](request: AuthenticatedRequest[T], atsNinoResponse: AtsNino)(
    implicit hc: HeaderCarrier) =
    atsNinoResponse match {
      case SuccessAtsNino(nino) =>
        for {
          utr <- citizenDetailsService.getUtr(nino)
        } yield {
          utr match {
            case SucccessMatchingDetailsResponse(value) =>
              Right(createAuthenticatedRequest(request, value.saUtr))
            case _ =>
              Left(
                Redirect(controllers.routes.ErrorController.notAuthorised())
              )
          }
        }
      case NoAtsNinoFound =>
        Future(
          Left(
            Redirect(controllers.routes.ErrorController.notAuthorised())
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
    newSaUtr: Option[SaUtr]): AuthenticatedRequest[T] =
    AuthenticatedRequest(
      userId = request.userId,
      agentRef = request.agentRef,
      saUtr = newSaUtr,
      nino = request.nino,
      None,
      None,
      None,
      true,
      request.credentials,
      request
    )
}

@ImplementedBy(classOf[SelfAssessmentActionImpl])
trait SelfAssessmentAction extends ActionRefiner[AuthenticatedRequest, AuthenticatedRequest]
