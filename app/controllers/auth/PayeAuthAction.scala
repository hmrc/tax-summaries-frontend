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
import play.api.Logging
import play.api.http.Status.SEE_OTHER
import play.api.mvc.Results.Redirect
import play.api.mvc._
import services.PertaxService
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, ConfidenceLevel, CredentialStrength, InsufficientConfidenceLevel, NoActiveSession, Nino => AuthNino}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class PayeAuthActionImpl @Inject()(override val authConnector: DefaultAuthConnector, cc: MessagesControllerComponents, pertaxService: PertaxService)(
  implicit ec: ExecutionContext,
  appConfig: ApplicationConfig)
    extends PayeAuthAction with AuthorisedFunctions with Logging {

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

            pertaxService.pertaxAuth(nino).fold(
              error => {
                val redirect = error.redirect
                val errorView = error.errorView

                if (redirect.isDefined) {
                  redirect.map(url => Future.successful(Redirect(url, SEE_OTHER)))
                    .getOrElse(Future.successful(Redirect(controllers.paye.routes.PayeErrorController.notAuthorised)))
                } else if (errorView.isDefined) {
                  errorView.map(data => Future.successful(Redirect(data.url, data.statusCode)))
                    .getOrElse(Future.successful(Redirect(controllers.paye.routes.PayeErrorController.notAuthorised)))
                } else {
                  Future.successful(Redirect(controllers.paye.routes.PayeErrorController.notAuthorised))
                }
              },
              _ =>
                block {
                  PayeAuthenticatedRequest(
                    Nino(nino),
                    isSa,
                    credentials,
                    request
                  )
                }
            )

          case _ => throw new RuntimeException("Auth retrieval failed for user")
        } recover {
        case _: NoActiveSession =>
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
