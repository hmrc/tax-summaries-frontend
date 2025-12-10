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

package common.controllers

import com.google.inject.Inject
import common.config.ApplicationConfig
import common.controllers.auth.AuthJourney
import common.forms.AtsYearChoiceFormProvider
import common.models.admin.{PAYEServiceToggle, SelfAssessmentServiceToggle}
import common.models.requests.AuthenticatedRequest
import common.models.{ActingAsAttorneyFor, AtsYearChoice, PAYE, SA}
import common.services.*
import common.utils.{AttorneyUtils, Globals}
import common.view_models.YearListViewModel
import common.views.html.SelectTaxYearView
import common.views.html.errors.GenericErrorView
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.*
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class SelectTaxYearController @Inject() (
  yearListViewModelService: YearListViewModelService,
  authJourney: AuthJourney,
  mcc: MessagesControllerComponents,
  selectTaxYearView: SelectTaxYearView,
  genericErrorView: GenericErrorView,
  atsYearChoiceFormProvider: AtsYearChoiceFormProvider,
  featureFlagService: FeatureFlagService
)(implicit appConfig: ApplicationConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with AttorneyUtils
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = authJourney.authForIndividualsOrAgents.async { implicit request =>
    areServicesEnabled.flatMap {
      case false => Future.successful(Redirect(routes.ErrorController.serviceUnavailable.url))
      case true  => render()
    }
  }

  private def areServicesEnabled: Future[Boolean] =
    for {
      saEnabled   <- featureFlagService.get(SelfAssessmentServiceToggle).map(_.isEnabled)
      payeEnabled <- featureFlagService.get(PAYEServiceToggle).map(_.isEnabled)
    } yield saEnabled || payeEnabled

  private def render(
    formWithErrors: Option[Form[AtsYearChoice]] = None
  )(implicit request: AuthenticatedRequest[_]): Future[Result] = {
    val session = putTaxUserTypeInSession(request)
    val form    = getYearChoiceForm(formWithErrors, request)

    for {
      saEnabled   <- featureFlagService.get(SelfAssessmentServiceToggle).map(_.isEnabled)
      payeEnabled <- featureFlagService.get(PAYEServiceToggle).map(_.isEnabled)
      result      <- yearListViewModelService.getSaAndPayeYearList.map {
                       case Right(vm) =>
                         Ok(selectTaxYearView(vm, form, actingAsAttorneyFor(vm), saEnabled, payeEnabled))
                           .withSession(session + ("atsList" -> vm.saData.toString))
                       case _         =>
                         InternalServerError(genericErrorView())
                     }
    } yield result
  }

  private def actingAsAttorneyFor(
    vm: YearListViewModel
  )(implicit request: AuthenticatedRequest[_]): Option[ActingAsAttorneyFor] = getActingAsAttorneyFor(
    request,
    vm.saData.forename,
    vm.saData.surname,
    vm.saData.utr
  )

  def onSubmit: Action[AnyContent] = authJourney.authForIndividualsOrAgents.async { implicit request =>
    atsYearChoiceFormProvider.atsYearChoiceForm
      .bindFromRequest()
      .fold(
        formWithErrors => render(Some(formWithErrors)),
        value =>
          Future.successful(
            redirectWithYear(value).withSession(request.session + ("yearChoice" -> AtsYearChoice.toString(value)))
          )
      )
  }

  private def redirectWithYear(taxYearChoice: AtsYearChoice): Result =
    taxYearChoice.atsType match {
      case SA   =>
        Redirect(
          sa.controllers.routes.AtsMainController.authorisedAtsMain.url + "?taxYear=" + taxYearChoice.year
        )
      case PAYE =>
        Redirect(paye.controllers.routes.PayeAtsMainController.show(taxYearChoice.year))
      case _    =>
        Redirect(common.controllers.routes.ErrorController.authorisedNoAts(taxYearChoice.year))
    }

  private def putTaxUserTypeInSession(request: AuthenticatedRequest[_]): Session =
    request
      .getQueryString(Globals.TAXS_USER_TYPE_QUERY_PARAMETER)
      .fold(request.session)(parameter => request.session + (Globals.TAXS_USER_TYPE_KEY -> parameter))

  private def getYearChoiceForm(
    formWithErrors: Option[Form[AtsYearChoice]],
    request: AuthenticatedRequest[_]
  ): Form[AtsYearChoice] =
    formWithErrors.getOrElse {
      request.session
        .get("yearChoice")
        .fold(atsYearChoiceFormProvider.atsYearChoiceForm)(value =>
          atsYearChoiceFormProvider.atsYearChoiceForm.fill(AtsYearChoice.fromString(Some(value)))
        )
    }
}
