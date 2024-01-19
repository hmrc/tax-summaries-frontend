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

package controllers.auth

import com.google.inject.{ImplementedBy, Inject}
import connectors.PertaxConnector
import models.PertaxApiResponse
import models.admin.PertaxBackendToggle
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.{InternalServerError, Redirect, Status}
import play.api.mvc.{ActionRefiner, ControllerComponents, Result}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.play.partials.HtmlPartial
import views.MainTemplate
import views.html.errors.ServiceUnavailableView

import scala.concurrent.{ExecutionContext, Future}

class PertaxAuthActionImpl @Inject() (
  override val authConnector: DefaultAuthConnector,
  cc: ControllerComponents,
  pertaxConnector: PertaxConnector,
  featureFlagService: FeatureFlagService,
  serviceUnavailableView: ServiceUnavailableView,
  mainTemplate: MainTemplate
) extends PertaxAuthAction
    with I18nSupport
    with AuthorisedFunctions
    with Logging {

  override def messagesApi: MessagesApi = cc.messagesApi

  override protected def refine[A](
    request: PayeAuthenticatedRequest[A]
  ): Future[Either[Result, PayeAuthenticatedRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    featureFlagService.get(PertaxBackendToggle).flatMap { toggle =>
      if (toggle.isEnabled) {
        pertaxConnector
          .pertaxAuth(request.nino.nino)
          .value
          .flatMap {
            case Right(PertaxApiResponse("ACCESS_GRANTED", _, _, _))                    =>
              Future.successful(Right(request))
            case Right(PertaxApiResponse("NO_HMRC_PT_ENROLMENT", _, _, Some(redirect))) =>
              Future.successful(Left(Redirect(s"$redirect/?redirectUrl=${SafeRedirectUrl(request.uri).encodedUrl}")))
            case Right(PertaxApiResponse(code, message, Some(errorView), _))                     =>
              logger.warn(s"Error response during authentication: $code $message")(implicitly)
              pertaxConnector.loadPartial(errorView.url)(request, executionContext).map {
                case partial: HtmlPartial.Success =>
                  Left(
                    Status(errorView.statusCode)(
                      mainTemplate(partial.title.getOrElse(""))(partial.content)(
                        request,
                        messagesApi.preferred(request)
                      )
                    )
                  )
                case _: HtmlPartial.Failure       =>
                  logger.error(s"The partial ${errorView.url} failed to be retrieved")
                  Left(InternalServerError(serviceUnavailableView()(request, messagesApi.preferred(request))))
              }
            case Right(response)                                                        =>
              val ex =
                new RuntimeException(
                  s"Pertax response `${response.code}` with message ${response.message} is not handled"
                )
              logger.error(ex.getMessage, ex)
              Future.successful(
                Left(InternalServerError(serviceUnavailableView()(request, messagesApi.preferred(request))))
              )

            case _ =>
              Future.successful(
                Left(InternalServerError(serviceUnavailableView()(request, messagesApi.preferred(request))))
              )
          }
      } else {
        Future.successful(Right(request))
      }
    }
  }

  override protected implicit val executionContext: ExecutionContext = cc.executionContext
}

@ImplementedBy(classOf[PertaxAuthActionImpl])
trait PertaxAuthAction extends ActionRefiner[PayeAuthenticatedRequest, PayeAuthenticatedRequest]
