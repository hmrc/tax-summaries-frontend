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

package controllers.paye

import java.time.LocalDate

import com.google.inject.Inject
import com.typesafe.scalalogging.LazyLogging
import config.ApplicationConfig
import controllers.auth.{PayeAuthAction, PayeAuthenticatedRequest}
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.GovernmentSpendService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.time.CurrentTaxYear
import views.html.HowTaxIsSpentView
import views.html.errors._
import uk.gov.hmrc.renderer.TemplateRenderer

import scala.concurrent.ExecutionContext

class PayeErrorController @Inject()(
  governmentSpendService: GovernmentSpendService,
  payeAuthAction: PayeAuthAction,
  mcc: MessagesControllerComponents,
  payeGenericErrorView: PayeGenericErrorView,
  howTaxIsSpentView: HowTaxIsSpentView,
  payeNotAuthorisedView: PayeNotAuthorisedView,
  payeServiceUnavailableView: PayeServiceUnavailableView)(
  implicit formPartialRetriever: FormPartialRetriever,
  templateRenderer: TemplateRenderer,
  appConfig: ApplicationConfig,
  ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with CurrentTaxYear with LazyLogging {

  val payeYear = appConfig.payeYear
  override def now: () => LocalDate = () => LocalDate.now()

  def genericError(status: Int): Action[AnyContent] = payeAuthAction { implicit request: PayeAuthenticatedRequest[_] =>
    {
      status match {
        case INTERNAL_SERVER_ERROR => InternalServerError(payeGenericErrorView())
        case _                     => BadGateway(payeGenericErrorView())
      }
    }
  }

  def authorisedNoAts: Action[AnyContent] = payeAuthAction.async { implicit request: PayeAuthenticatedRequest[_] =>
    {
      governmentSpendService.getGovernmentSpendFigures(payeYear, Some(request.nino)) map { data =>
        Ok(howTaxIsSpentView(data, payeYear))
      } recover {
        case e: IllegalArgumentException =>
          logger.error(e.getMessage)
          BadRequest(payeGenericErrorView())
        case e =>
          logger.error(e.getMessage)
          InternalServerError(payeGenericErrorView())
      }
    }
  }

  def notAuthorised: Action[AnyContent] = Action { implicit request: Request[_] =>
    {
      Ok(payeNotAuthorisedView())
    }
  }

  def serviceUnavailable: Action[AnyContent] = Action { implicit request: Request[_] =>
    {
      Ok(payeServiceUnavailableView())
    }
  }
}
