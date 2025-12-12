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

package common.services

import com.google.inject.Inject
import common.config.ApplicationConfig
import common.connectors.PertaxConnector
import common.models.PertaxApiResponse
import play.api.Logging
import play.api.http.Status.UNAUTHORIZED
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.{InternalServerError, Redirect, Status}
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, ConfidenceLevel}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.play.partials.HtmlPartial
import common.views.MainTemplate
import common.views.html.errors.ServiceUnavailableView

import scala.concurrent.{ExecutionContext, Future}

class PertaxAuthService @Inject() (
  val authConnector: DefaultAuthConnector,
  val messagesApi: MessagesApi,
  pertaxConnector: PertaxConnector,
  serviceUnavailableView: ServiceUnavailableView,
  mainTemplate: MainTemplate,
  appConfig: ApplicationConfig
)(implicit
  ec: ExecutionContext
) extends AuthorisedFunctions
    with I18nSupport
    with Logging {

  private def upliftConfidenceLevel(redirect: String): Result =
    Redirect(
      redirect,
      Map(
        "origin"          -> Seq(appConfig.appName),
        "confidenceLevel" -> Seq(ConfidenceLevel.L200.toString),
        "completionURL"   -> Seq(appConfig.loginCallback),
        "failureURL"      -> Seq(appConfig.iVUpliftFailureCallback)
      )
    )

  def authorise[T, M <: Request[T]](request: M): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    pertaxConnector
      .pertaxPostAuthorise()
      .value
      .flatMap {
        case Left(UpstreamErrorResponse(_, UNAUTHORIZED, _, _))                                 =>
          Future.successful(Some(redirectToLogin))
        case Right(PertaxApiResponse("ACCESS_GRANTED", _, _, _))                                =>
          Future.successful(None)
        case Right(PertaxApiResponse("NO_HMRC_PT_ENROLMENT", _, _, Some(redirect)))             =>
          Future.successful(Some(Redirect(s"$redirect?redirectUrl=${SafeRedirectUrl(request.uri).encodedUrl}")))
        case Right(PertaxApiResponse("CONFIDENCE_LEVEL_UPLIFT_REQUIRED", _, _, Some(redirect))) =>
          Future.successful(Some(upliftConfidenceLevel(redirect)))
        case Right(PertaxApiResponse("CREDENTIAL_STRENGTH_UPLIFT_REQUIRED", _, _, Some(_)))     =>
          val ex = new RuntimeException("Weak credentials should be dealt before the service")
          logger.error(ex.getMessage, ex)
          Future.successful(
            Some(InternalServerError(serviceUnavailableView()(request, messagesApi.preferred(request))))
          )
        case Right(PertaxApiResponse(code, message, Some(errorView), _))                        =>
          logger.warn(s"Error response during authentication: $code $message")(implicitly)
          pertaxConnector.loadPartial(errorView.url)(request, implicitly).map {
            case partial: HtmlPartial.Success =>
              Some(
                Status(errorView.statusCode)(
                  mainTemplate(partial.title.getOrElse(""))(partial.content)(
                    request,
                    messagesApi.preferred(request)
                  )
                )
              )
            case _: HtmlPartial.Failure       =>
              logger.error(s"The partial ${errorView.url} failed to be retrieved")
              Some(InternalServerError(serviceUnavailableView()(request, messagesApi.preferred(request))))
          }
        case Right(response)                                                                    =>
          val ex =
            new RuntimeException(s"Pertax response `${response.code}` with message ${response.message} is not handled")
          logger.error(ex.getMessage, ex)
          Future.successful(
            Some(InternalServerError(serviceUnavailableView()(request, messagesApi.preferred(request))))
          )
        case _                                                                                  =>
          Future.successful(
            Some(InternalServerError(serviceUnavailableView()(request, messagesApi.preferred(request))))
          )
      }
  }

  private def redirectToLogin: Result =
    Redirect(
      appConfig.loginUrl,
      Map(
        "continue_url" -> Seq(appConfig.loginCallback),
        "origin"       -> Seq(appConfig.appName)
      )
    )
}
