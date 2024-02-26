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

package services

import com.google.inject.Inject
import connectors.PertaxConnector
import models.PertaxApiResponse
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.{InternalServerError, Redirect, Status}
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.play.partials.HtmlPartial
import views.MainTemplate
import views.html.errors.ServiceUnavailableView

import scala.concurrent.{ExecutionContext, Future}

class PertaxAuthService @Inject() (
  val authConnector: DefaultAuthConnector,
  val messagesApi: MessagesApi,
  pertaxConnector: PertaxConnector,
  serviceUnavailableView: ServiceUnavailableView,
  mainTemplate: MainTemplate
)(implicit
  ec: ExecutionContext
) extends AuthorisedFunctions
    with I18nSupport
    with Logging {
  def authorise[T, M <: Request[T]](request: M): Future[Option[Result]] = {
    println("\nAUTHORISING IN BACKEND:" + request)
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    pertaxConnector
      .pertaxPostAuthorise()
      .value
      .flatMap {
        case Right(PertaxApiResponse("ACCESS_GRANTED", _, _, _))                    =>
          Future.successful(None)
        case Right(PertaxApiResponse("NO_HMRC_PT_ENROLMENT", _, _, Some(redirect))) =>
          Future.successful(Some(Redirect(s"$redirect/?redirectUrl=${SafeRedirectUrl(request.uri).encodedUrl}")))
        case Right(PertaxApiResponse(code, message, Some(errorView), _))            =>
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
        case Right(response)                                                        =>
          val ex =
            new RuntimeException(
              s"Pertax response `${response.code}` with message ${response.message} is not handled"
            )
          logger.error(ex.getMessage, ex)
          Future.successful(
            Some(InternalServerError(serviceUnavailableView()(request, messagesApi.preferred(request))))
          )

        case _ =>
          Future.successful(
            Some(InternalServerError(serviceUnavailableView()(request, messagesApi.preferred(request))))
          )
      }

    /* Code below from Pascal's benefits fe pertax auth code. I need to add parameters to uplift url passed back and check other cases too
        pertaxConnector.pertaxPostAuthorise.value.flatMap {
      case Left(UpstreamErrorResponse(_, status, _, _)) if status == UNAUTHORIZED          =>
        Future.successful(Some(signInJourney))
      case Left(_)                                                                     =>
        Future.successful(Some(InternalServerError(internalServerErrorView())))
      case Right(PertaxResponse("ACCESS_GRANTED", _, _, _))                                =>
        Future.successful(None)
      case Right(PertaxResponse("NO_HMRC_PT_ENROLMENT", _, _, Some(redirect)))             =>
        Future.successful(Some(Redirect(s"$redirect?redirectUrl=${SafeRedirectUrl(request.uri).encodedUrl}")))
      case Right(PertaxResponse("CONFIDENCE_LEVEL_UPLIFT_REQUIRED", _, _, Some(redirect))) =>
        Future.successful(Some(upliftJourney(redirect)))
      case Right(PertaxResponse("CREDENTIAL_STRENGTH_UPLIFT_REQUIRED", _, _, Some(_)))     =>
        val ex =
          new RuntimeException(
            s"Weak credentials should be dealt before the service"
          )
        logger.error(ex.getMessage, ex)
        Future.successful(Some(InternalServerError(internalServerErrorView())))

      case Right(PertaxResponse(_, _, Some(errorView), _)) =>
        pertaxConnector.loadPartial(errorView.url).map {
          case partial: HtmlPartial.Success =>
            Some(Status(errorView.statusCode)(mainTemplate(partial.title.getOrElse(""))(partial.content)))
          case _: HtmlPartial.Failure       =>
            logger.error(s"The partial ${errorView.url} failed to be retrieved")
            Some(InternalServerError(internalServerErrorView()))
        }
      case Right(response)                                 =>
        val ex =
          new RuntimeException(
            s"Pertax response `${response.code}` with message ${response.message} is not handled"
          )
        logger.error(ex.getMessage, ex)
        Future.successful(Some(InternalServerError(internalServerErrorView())))
    }
     */

  }
}
