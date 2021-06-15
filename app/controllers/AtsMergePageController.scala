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
import config.ApplicationConfig
import connectors.DataCacheConnector
import controllers.auth.{AuthenticatedRequest, MergePageAuthAction}
import controllers.paye.routes.PayeAtsMainController
import models.{AtsListData, AtsType, AtsYearChoice, PAYE, SA}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import utils.{AccountUtils, AttorneyUtils, Globals}
import view_models.AtsForms.atsYearFormMapping
import view_models.{AtsList, TaxYearEnd}
import views.html.AtsMergePageView
import views.html.errors.{GenericErrorView, TokenErrorView}

import scala.concurrent.{ExecutionContext, Future}

class AtsMergePageController @Inject()(
  atsMergePageService: AtsMergePageService,
  authAction: MergePageAuthAction,
  mcc: MessagesControllerComponents,
  atsMergePageView: AtsMergePageView,
  genericErrorView: GenericErrorView)(
  implicit formPartialRetriever: FormPartialRetriever,
  templateRenderer: TemplateRenderer,
  appConfig: ApplicationConfig,
  ec: ExecutionContext)
    extends FrontendController(mcc) with AttorneyUtils with I18nSupport {

  def onPageLoad: Action[AnyContent] = authAction.async { implicit request: AuthenticatedRequest[_] =>
    if (appConfig.saShuttered && appConfig.payeShuttered)
      Future.successful(Redirect(routes.ErrorController.serviceUnavailable().url))
    else getSaAndPayeYearList()
  }

  private def getSaAndPayeYearList(formWithErrors: Option[Form[AtsYearChoice]] = None)(
    implicit request: AuthenticatedRequest[_]) = {
    val session = request
      .getQueryString(Globals.TAXS_USER_TYPE_QUERY_PARAMETER)
      .fold(
        request.session
      )(parameter => request.session + (Globals.TAXS_USER_TYPE_KEY -> parameter))

    atsMergePageService.getSaAndPayeYearList.map {
      case Right(atsMergePageViewModel) =>
        Ok(
          atsMergePageView(
            atsMergePageViewModel,
            formWithErrors.getOrElse(atsYearFormMapping),
            getActingAsAttorneyFor(
              request,
              atsMergePageViewModel.saData.forename,
              atsMergePageViewModel.saData.surname,
              atsMergePageViewModel.saData.utr)
          ))
          .withSession(session + ("atsList" -> atsMergePageViewModel.saData.toString))

      case _ => InternalServerError(genericErrorView())
    }

  }

  def onSubmit: Action[AnyContent] = authAction.async { implicit request =>
    atsYearFormMapping.bindFromRequest.fold(
      formWithErrors => {
        getSaAndPayeYearList(Some(formWithErrors))(request)
      },
      value => {
        Future.successful(redirectWithYear(value))
      }
    )
  }

  private def redirectWithYear(taxYearChoice: AtsYearChoice)(implicit request: AuthenticatedRequest[_]): Result =
    taxYearChoice.atsType match {
      case SA =>
        Redirect(controllers.routes.AtsMainController.authorisedAtsMain().url + "?taxYear=" + taxYearChoice.year)
      case PAYE =>
        Redirect(controllers.paye.routes.PayeAtsMainController.show(taxYearChoice.year))
      case _ => {
        Redirect(controllers.routes.ErrorController.authorisedNoAts(selectedTaxYear = Some(taxYearChoice.year)))
      }
    }

}
