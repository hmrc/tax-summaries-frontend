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

package controllers

import com.google.inject.Inject
import config.ApplicationConfig
import controllers.auth.AuthJourney
import controllers.auth.requests.AuthenticatedRequest
import models.admin.{PAYEServiceToggle, SelfAssessmentServiceToggle}
import models.{AtsYearChoice, PAYE, SA}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result, Session}
import services._
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{AttorneyUtils, Globals}
import view_models.AtsForms
import views.html.AtsMergePageView
import views.html.errors.GenericErrorView

import scala.concurrent.{ExecutionContext, Future}

class AtsMergePageController @Inject() (
  atsMergePageService: AtsMergePageService,
  authJourney: AuthJourney,
  mcc: MessagesControllerComponents,
  atsMergePageView: AtsMergePageView,
  genericErrorView: GenericErrorView,
  atsForms: AtsForms,
  featureFlagService: FeatureFlagService
)(implicit appConfig: ApplicationConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with AttorneyUtils
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = authJourney.authForIndividualsOrAgents.async { implicit request =>
    areServicesEnabled.flatMap {
      case false => Future.successful(Redirect(routes.ErrorController.serviceUnavailable.url))
      case true  => getSaAndPayeYearList()
    }
  }

  private def areServicesEnabled: Future[Boolean] =
    for {
      saEnabled   <- featureFlagService.get(SelfAssessmentServiceToggle).map(_.isEnabled)
      payeEnabled <- featureFlagService.get(PAYEServiceToggle).map(_.isEnabled)
    } yield saEnabled && payeEnabled

  private def getSaAndPayeYearList(
    formWithErrors: Option[Form[AtsYearChoice]] = None
  )(implicit request: AuthenticatedRequest[_]): Future[Result] = {
    val session = putTaxUserTypeInSession(request)
    val form    = getYearChoiceForm(formWithErrors, request)

    for {
      saEnabled   <- featureFlagService.get(SelfAssessmentServiceToggle).map(_.isEnabled)
      payeEnabled <- featureFlagService.get(PAYEServiceToggle).map(_.isEnabled)
      result      <- atsMergePageService.getSaAndPayeYearList.map {
                       case Right(atsMergePageViewModel) =>
                         Ok(
                           atsMergePageView(
                             atsMergePageViewModel,
                             form,
                             getActingAsAttorneyFor(
                               request,
                               atsMergePageViewModel.saData.forename,
                               atsMergePageViewModel.saData.surname,
                               atsMergePageViewModel.saData.utr
                             ),
                             saEnabled,
                             payeEnabled
                           )
                         ).withSession(session + ("atsList" -> atsMergePageViewModel.saData.toString))

                       case _                            =>
                         InternalServerError(genericErrorView())
                     }
    } yield result
  }

  def onSubmit: Action[AnyContent] = authJourney.authForIndividualsOrAgents.async { implicit request =>
    atsForms.atsYearFormMapping
      .bindFromRequest()
      .fold(
        formWithErrors => getSaAndPayeYearList(Some(formWithErrors)),
        value =>
          Future.successful(
            redirectWithYear(value).withSession(request.session + ("yearChoice" -> AtsYearChoice.toString(value)))
          )
      )
  }

  private def redirectWithYear(taxYearChoice: AtsYearChoice): Result =
    taxYearChoice.atsType match {
      case SA   =>
        Redirect(controllers.routes.AtsMainController.authorisedAtsMain.url + "?taxYear=" + taxYearChoice.year)
      case PAYE =>
        Redirect(controllers.paye.routes.PayeAtsMainController.show(taxYearChoice.year))
      case _    =>
        Redirect(controllers.routes.ErrorController.authorisedNoAts(taxYearChoice.year))
    }

  private def putTaxUserTypeInSession(request: AuthenticatedRequest[_]): Session =
    request
      .getQueryString(Globals.TAXS_USER_TYPE_QUERY_PARAMETER)
      .fold(request.session)(parameter => request.session + (Globals.TAXS_USER_TYPE_KEY -> parameter))

  private def getYearChoiceForm(
    formWithErrors: Option[Form[AtsYearChoice]],
    request: AuthenticatedRequest[_]
  ): Form[AtsYearChoice]                                                         =
    formWithErrors.getOrElse {
      request.session
        .get("yearChoice")
        .fold(atsForms.atsYearFormMapping)(value =>
          atsForms.atsYearFormMapping.fill(AtsYearChoice.fromString(Some(value)))
        )
    }
}
