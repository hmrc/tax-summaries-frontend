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

package controllers

import java.util.Date

import com.google.inject.Inject
import config.ApplicationConfig
import controllers.auth.AuthenticatedRequest
import models.ErrorResponse
import play.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{AnyContent, MessagesControllerComponents, Request, Result}
import services._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import utils._
import views.html.errors.{GenericErrorView, TokenErrorView}

import scala.concurrent.{ExecutionContext, Future}

abstract class TaxsController @Inject()(
  mcc: MessagesControllerComponents,
  genericErrorView: GenericErrorView,
  tokenErrorView: TokenErrorView)(
  implicit ormPartialRetriever: FormPartialRetriever,
  implicit val templateRenderer: TemplateRenderer,
  appConfig: ApplicationConfig,
  ec: ExecutionContext)
    extends FrontendController(mcc) with AccountUtils with AttorneyUtils with I18nSupport {

  def auditService: AuditService

  type ViewModel <: GenericViewModel

  def obtainResult(data: ViewModel)(implicit request: AuthenticatedRequest[_]): Result

  def extractViewModel()(implicit request: AuthenticatedRequest[_]): Future[Either[ErrorResponse, GenericViewModel]]

  def transformation(implicit request: AuthenticatedRequest[_]): Future[Result]

  def show(implicit request: AuthenticatedRequest[_]): Future[Result] =
    transformation recover {
      case error =>
        Logger.info(Globals.TAXS_LOGGER_ERROR_DESCR, error)
        error match {
          case token_error: AgentTokenException =>
            auditService.sendEvent(
              AuditTypes.Tx_FAILED,
              Map(
                "userId"         -> getAccountId(request),
                "error"          -> token_error.message,
                "time"           -> new Date().toString,
                "attemptedToken" -> request2flash.get(Globals.TAXS_AGENT_TOKEN_KEY).getOrElse("")
              )
            )
            Ok(tokenErrorView())
          case _ => Ok(genericErrorView())
        }
    }

  def getParamAsInt(param: String, block: Int => Future[GenericViewModel])(implicit request: Request[AnyContent]) = {
    val intParam = request.body.asFormUrlEncoded.map(_(param).head.toInt).getOrElse(0)
    block(intParam)
  }

}
