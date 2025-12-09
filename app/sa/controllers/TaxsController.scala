/*
 * Copyright 2025 HM Revenue & Customs
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

package sa.controllers

import com.google.inject.Inject
import common.config.ApplicationConfig
import common.controllers.routes
import common.models.requests.AuthenticatedRequest
import common.models.{ErrorResponse, InvalidTaxYear, MissingTaxYear}
import common.services.*
import common.utils.*
import common.view_models.{ATSUnavailableViewModel, NoATSViewModel}
import common.views.html.errors.{GenericErrorView, TokenErrorView}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import java.util.Date
import scala.annotation.nowarn
import scala.concurrent.{ExecutionContext, Future}

abstract class TaxsController @Inject() (
  mcc: MessagesControllerComponents,
  genericErrorView: GenericErrorView,
  tokenErrorView: TokenErrorView
)(implicit val appConfig: ApplicationConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with AccountUtils
    with AttorneyUtils
    with I18nSupport
    with Logging {

  def auditService: AuditService

  type ViewModel <: GenericViewModel

  def obtainResult(data: ViewModel)(implicit request: AuthenticatedRequest[_]): Result

  def extractViewModel()(implicit request: AuthenticatedRequest[_]): Future[Either[ErrorResponse, GenericViewModel]]

  @nowarn("msg=match may not be exhaustive")
  @nowarn("msg=the type test for .*ViewModel cannot be checked at runtime")
  private def transformation(implicit request: AuthenticatedRequest[_]): Future[Result] =
    extractViewModel() map {
      case Right(NoATSViewModel(year))       => Redirect(routes.ErrorController.authorisedNoAts(year))
      case Right(_: ATSUnavailableViewModel) => InternalServerError(genericErrorView())
      case Right(result: ViewModel)          => obtainResult(result)
      case Left(InvalidTaxYear(year))        => Redirect(routes.ErrorController.authorisedNoAts(year))
      case Left(MissingTaxYear)              => Redirect(routes.ErrorController.authorisedNoTaxYear)
    }

  def show(implicit request: AuthenticatedRequest[_]): Future[Result] =
    transformation recover { case error =>
      logger.info(Globals.TAXS_LOGGER_ERROR_DESCR, error)
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
        case ex                               =>
          logger.error(ex.getMessage)
          InternalServerError(genericErrorView())
      }
    }
}
