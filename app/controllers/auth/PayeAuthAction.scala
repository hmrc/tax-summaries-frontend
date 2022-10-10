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

import cats.data.EitherT
import com.google.inject.{ImplementedBy, Inject}
import config.ApplicationConfig
import connectors.PertaxConnector
import models.PertaxApiResponse
import play.api.Logging
import play.api.http.Status.{SEE_OTHER, UNAUTHORIZED}
import play.api.i18n.Messages.implicitMessagesProviderToMessages
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.{InternalServerError, Redirect}
import play.api.mvc._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, ConfidenceLevel, CredentialStrength, InsufficientConfidenceLevel, NoActiveSession, Nino => AuthNino}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import views.html.errors.ServiceUnavailableView

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class PayeAuthActionImpl @Inject() (
                                     override val authConnector: DefaultAuthConnector,
                                     cc: ControllerComponents,
                                     pertaxConnector: PertaxConnector,
                                     serviceUnavailableView: ServiceUnavailableView
)(implicit ec: ExecutionContext, appConfig: ApplicationConfig)
    extends PayeAuthAction
      with I18nSupport
      with AuthorisedFunctions
      with Logging {

  override val parser: BodyParser[AnyContent]               = cc.parsers.defaultBodyParser
  override protected val executionContext: ExecutionContext = cc.executionContext

  val payeShuttered: Boolean = appConfig.payeShuttered

  override def invokeBlock[A](
    request: Request[A],
    block: PayeAuthenticatedRequest[A] => Future[Result]
  ): Future[Result] =
    if (payeShuttered) {
      Future.successful(Redirect(controllers.paye.routes.PayeErrorController.serviceUnavailable))
    } else {
      implicit val hc: HeaderCarrier =
        HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

      authorised(ConfidenceLevel.L200 and AuthNino(hasNino = true) and CredentialStrength(CredentialStrength.strong))
        .retrieve(Retrievals.allEnrolments and Retrievals.nino and Retrievals.credentials) {
          case enrolments ~ Some(nino) ~ Some(credentials) =>
            val isSa = enrolments.getEnrolment("IR-SA").isDefined
            singleGGAccountCheck(nino, isSa, credentials)(implicitly, request).semiflatMap { _ =>
              block {
                PayeAuthenticatedRequest(
                  Nino(nino),
                  isSa,
                  credentials,
                  request
                )
              }
            }.merge
          case _                                           =>
            throw new RuntimeException("Auth retrieval failed for user")
        } recover {
        case _: NoActiveSession             =>
          Redirect(
            appConfig.payeLoginUrl,
            Map(
              "continue_url" -> Seq(appConfig.payeLoginCallbackUrl),
              "origin"       -> Seq(appConfig.appName)
            )
          )
        case _: InsufficientConfidenceLevel =>
          upliftConfidenceLevel(request)
        case NonFatal(e)                    =>
          logger.error(s"Exception in PayeAuthAction: $e", e)
          Redirect(controllers.paye.routes.PayeErrorController.notAuthorised)
      }
    }

  private def singleGGAccountCheck(
    nino: String,
    isSa: Boolean,
    credentials: Credentials
  )(implicit hc: HeaderCarrier, request: Request[_]): EitherT[Future, Result, Unit] = {
    pertaxConnector
      .pertaxAuth(nino)
      .transform {
        case Right(PertaxApiResponse("ACCESS_GRANTED", _, _, _)) => Right(())
        case Right(PertaxApiResponse("NO_HMRC_PT_ENROLMENT", _, _, Some(redirect))) =>
          Left(Redirect(s"$redirect?redirectUrl=${SafeRedirectUrl(request.uri).encodedUrl}"))
        case Right(error) =>
          logger.error(s"Invalid code response from pertax with message: ${error.message}")
          Left(Redirect(controllers.paye.routes.PayeErrorController.notAuthorised))
        case _ => Left(InternalServerError(serviceUnavailableView()))
      }
  }


  private def upliftConfidenceLevel(request: Request[_]) =
    Redirect(
      appConfig.identityVerificationUpliftUrl,
      Map(
        "origin"          -> Seq(appConfig.appName),
        "confidenceLevel" -> Seq(ConfidenceLevel.L200.toString),
        "completionURL"   -> Seq(appConfig.loginCallback),
        "failureURL"      -> Seq(appConfig.iVUpliftFailureCallback)
      )
    )
}

@ImplementedBy(classOf[PayeAuthActionImpl])
trait PayeAuthAction
    extends ActionBuilder[PayeAuthenticatedRequest, AnyContent]
    with ActionFunction[Request, PayeAuthenticatedRequest]
