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

import com.google.inject.Inject
import com.typesafe.scalalogging.LazyLogging
import config.ApplicationConfig
import controllers.auth.{AuthAction, MergePageAuthAction, MinAuthAction}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import services.GovernmentSpendService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.time.CurrentTaxYear
import views.html.HowTaxIsSpentView
import views.html.errors.{NotAuthorisedView, ServiceUnavailableView}

import java.time.LocalDate
import scala.concurrent.ExecutionContext

class ErrorController @Inject()(
  governmentSpendService: GovernmentSpendService,
  authAction: AuthAction,
  mergePageAuthAction: MergePageAuthAction,
  minAuthAction: MinAuthAction,
  mcc: MessagesControllerComponents,
  notAuthorisedView: NotAuthorisedView,
  howTaxIsSpentView: HowTaxIsSpentView,
  serviceUnavailableView: ServiceUnavailableView)(
  implicit val templateRenderer: TemplateRenderer,
  appConfig: ApplicationConfig,
  ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with CurrentTaxYear with LazyLogging {

  override def now: () => LocalDate = () => LocalDate.now()

  def authorisedNoAts(taxYear: Int): Action[AnyContent] = mergePageAuthAction.async { implicit request =>
    governmentSpendService.getGovernmentSpendFigures(taxYear) map { spendData =>
      if (!appConfig.currentTaxYearSpendData && taxYear == appConfig.taxYear) {
        logger.error(s"$taxYear is unavailable at this time")
        BadRequest(serviceUnavailableView())
      } else {
        Ok(howTaxIsSpentView(spendData, taxYear))
      }
    } recover {
      case e: IllegalArgumentException =>
        logger.error(e.getMessage)
        BadRequest(serviceUnavailableView())
      case e =>
        logger.error(e.getMessage)
        InternalServerError(serviceUnavailableView())
    }
  }

  def notAuthorised: Action[AnyContent] = minAuthAction { implicit request =>
    Ok(notAuthorisedView())
  }

  def serviceUnavailable: Action[AnyContent] = Action { implicit request: Request[_] =>
    Ok(serviceUnavailableView())
  }
}
