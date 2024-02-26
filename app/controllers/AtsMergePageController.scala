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

package controllers

import com.google.inject.Inject
import config.ApplicationConfig
import controllers.auth.AuthJourney
import controllers.auth.requests.AuthenticatedRequest
import models.{AtsYearChoice, PAYE, SA}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services._
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
  atsForms: AtsForms
)(implicit appConfig: ApplicationConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with AttorneyUtils
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = authJourney.authForIndividualsAndAgents.async {
    implicit request: AuthenticatedRequest[_] =>
      println("\nAGENT:" + request.isAgent)
      if (appConfig.saShuttered && appConfig.payeShuttered) {
        Future.successful(Redirect(routes.ErrorController.serviceUnavailable.url))
      } else getSaAndPayeYearList()
  }

  private def getSaAndPayeYearList(
    formWithErrors: Option[Form[AtsYearChoice]] = None
  )(implicit request: AuthenticatedRequest[_]) = {
    val session = request
      .getQueryString(Globals.TAXS_USER_TYPE_QUERY_PARAMETER)
      .fold(
        request.session
      )(parameter => request.session + (Globals.TAXS_USER_TYPE_KEY -> parameter))

    val form    = formWithErrors.getOrElse(
      request.session
        .get("yearChoice")
        .fold(
          atsForms.atsYearFormMapping
        )(value => atsForms.atsYearFormMapping.fill(AtsYearChoice.fromString(Some(value))))
    )

    atsMergePageService.getSaAndPayeYearList.map {
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
            )
          )
        )
          .withSession(session + ("atsList" -> atsMergePageViewModel.saData.toString))

      case _                            =>
        InternalServerError(genericErrorView())
    }
  }

  def onSubmit: Action[AnyContent] = authJourney.authForIndividualsAndAgents.async { implicit request =>
    atsForms.atsYearFormMapping
      .bindFromRequest()
      .fold(
        formWithErrors => getSaAndPayeYearList(Some(formWithErrors))(request),
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

}
