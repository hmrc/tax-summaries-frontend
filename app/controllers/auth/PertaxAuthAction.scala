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
import connectors.PertaxConnector
import models.PertaxApiResponse
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.{InternalServerError, Redirect}
import play.api.mvc.{ActionBuilder, ActionFunction, ActionRefiner, AnyContent, BodyParser, ControllerComponents, Request, Result}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, ConfidenceLevel, CredentialStrength, Nino => AuthNino}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.errors.ServiceUnavailableView

import scala.concurrent.{ExecutionContext, Future}

class PertaxAuthActionImpl @Inject() (
  override val authConnector: DefaultAuthConnector,
  cc: ControllerComponents,
  pertaxConnector: PertaxConnector,
  serviceUnavailableView: ServiceUnavailableView
)(implicit ec: ExecutionContext, appConfig: ApplicationConfig)
    extends PertaxAuthAction
    with I18nSupport
    with AuthorisedFunctions
    with Logging {

  override def messagesApi: MessagesApi = cc.messagesApi

  override protected def refine[A](
    request: PayeAuthenticatedRequest[A]
  ): Future[Either[Result, PayeAuthenticatedRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    pertaxConnector
      .pertaxAuth(request.nino.nino)
      .transform {
        case Right(PertaxApiResponse("ACCESS_GRANTED", _, _, _))                    =>
          Right(request.copy(nino = request.nino))
        case Right(PertaxApiResponse("NO_HMRC_PT_ENROLMENT", _, Some(redirect), _)) =>
          Left(Redirect(s"$redirect?redirectUrl=${SafeRedirectUrl(request.uri).encodedUrl}"))
        case Right(error)                                                           =>
          logger.error(s"Invalid code response from pertax with message: ${error.message}")
          Left(Redirect(controllers.paye.routes.PayeErrorController.notAuthorised))
        case _                                                                      =>
          Left(
            InternalServerError(
              serviceUnavailableView()(request, request2Messages(request), implicitly, implicitly)
            )
          )
      }
      .value
  }

  override protected def executionContext: ExecutionContext = ec
}

@ImplementedBy(classOf[PertaxAuthActionImpl])
trait PertaxAuthAction extends ActionRefiner[PayeAuthenticatedRequest, PayeAuthenticatedRequest]
