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

package common.controllers

import com.google.inject.{Inject, Singleton}
import common.config.ApplicationConfig
import common.controllers.auth.AuthJourney
import common.services.GovernmentSpendService
import common.utils.TaxYearUtil
import common.views.html.HowTaxIsSpentView
import common.views.html.errors.{PageNotFoundTemplateView, ServiceUnavailableView}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import sa.views.html.errors.NotAuthorisedView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.time.CurrentTaxYear

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ErrorController @Inject() (
  governmentSpendService: GovernmentSpendService,
  authJourney: AuthJourney,
  mcc: MessagesControllerComponents,
  notAuthorisedView: NotAuthorisedView,
  howTaxIsSpentView: HowTaxIsSpentView,
  serviceUnavailableView: ServiceUnavailableView,
  pageNotFoundTemplateView: PageNotFoundTemplateView,
  taxYearUtil: TaxYearUtil
)(implicit val appConfig: ApplicationConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport
    with CurrentTaxYear
    with Logging {

  override def now: () => LocalDate = () => LocalDate.now()

  def authorisedNoAts(taxYear: Int): Action[AnyContent] = authJourney.authForIndividualsOrAgents.async {
    implicit request =>
      if (taxYearUtil.isValidTaxYear(taxYear)) {
        governmentSpendService
          .getGovernmentSpendFigures(taxYear)
          .fold(
            errorResponse => {
              logger.error(errorResponse.message)
              InternalServerError(serviceUnavailableView())
            },
            spendData => Ok(howTaxIsSpentView(spendData, taxYear))
          )
      } else {
        Future.successful(NotFound(pageNotFoundTemplateView()))
      }
  }

  def authorisedNoTaxYear: Action[AnyContent] = authJourney.authForIndividualsOrAgents.async { implicit request =>
    Future.successful(NotFound(pageNotFoundTemplateView()))
  }

  def notAuthorised: Action[AnyContent] = authJourney.authMinimal { implicit request =>
    Ok(notAuthorisedView())
  }

  def serviceUnavailable: Action[AnyContent] = Action { implicit request: Request[_] =>
    Ok(serviceUnavailableView())
  }
}
