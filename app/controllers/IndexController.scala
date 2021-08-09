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
import controllers.auth.{AuthAction, AuthenticatedRequest}
import models.{AtsListData, ErrorResponse}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{AtsListService, AtsYearListService, AuditService}
import uk.gov.hmrc.renderer.TemplateRenderer
import utils._
import view_models.{AtsForms, AtsList, NoATSViewModel, TaxYearEnd}
import views.html.TaxsIndexView
import views.html.errors.{GenericErrorView, TokenErrorView}

import scala.concurrent.{ExecutionContext, Future}

class IndexController @Inject()(
  dataCacheConnector: DataCacheConnector,
  atsYearListService: AtsYearListService,
  atsListService: AtsListService,
  val auditService: AuditService,
  authAction: AuthAction,
  mcc: MessagesControllerComponents,
  taxsIndexView: TaxsIndexView,
  genericErrorView: GenericErrorView,
  tokenErrorView: TokenErrorView,
  atsForms: AtsForms)(
  implicit override val templateRenderer: TemplateRenderer,
  appConfig: ApplicationConfig,
  ec: ExecutionContext)
    extends TaxsController(mcc, genericErrorView, tokenErrorView) {

  def authorisedIndex: Action[AnyContent] = authAction.async { request: AuthenticatedRequest[_] =>
    agentAwareShow(request)
  }

  def authorisedOnSubmit: Action[AnyContent] = authAction.async { request =>
    onSubmit(request)
  }

  //FIXME add extra check - agent tries multiple ids in same session
  def agentAwareShow(implicit request: AuthenticatedRequest[_]): Future[Result] =
    request.getQueryString(Globals.TAXS_USER_TYPE_QUERY_PARAMETER) match {
      case Some(Globals.TAXS_PORTAL_REFERENCE) => {

        val session = request.session + (Globals.TAXS_USER_TYPE_KEY -> Globals.TAXS_PORTAL_REFERENCE)
        val agentToken = request.getQueryString(Globals.TAXS_AGENT_TOKEN_ID)

        agentToken.fold[Future[_]] {
          Future.successful(None)
        } { token =>
          if (AccountUtils.isAgent(request)) {
            dataCacheConnector.storeAgentToken(token) recover { case e: Throwable => throw e }
          } else {
            Future.successful(None)
          }
        } map { _ =>
          Redirect(routes.IndexController.authorisedIndex).withSession(session)
        }
      }
      case _ => show(request)
    }

  type ViewModel = AtsList

  override def extractViewModel()(
    implicit request: AuthenticatedRequest[_]): Future[Either[ErrorResponse, GenericViewModel]] =
    atsYearListService.getAtsListData.map(Right(_))

  def getViewModel(result: ViewModel)(implicit request: AuthenticatedRequest[_]): Future[Result] =
    result.yearList match {
      case TaxYearEnd(year) :: Nil => redirectWithYear(year.get.toInt)
      case _ =>
        Future.successful(
          Ok(
            taxsIndexView(
              result,
              atsForms.atsYearFormMapping,
              getActingAsAttorneyFor(request, result.forename, result.surname, result.utr)))
            .withSession(request.session + ("atsList" -> result.toString)))
    }

  override def transformation(implicit request: AuthenticatedRequest[_]): Future[Result] =
    extractViewModel flatMap {
      case Right(result: ViewModel) => getViewModel(result)
      case Right(_: NoATSViewModel) => Future.successful(Redirect(routes.ErrorController.authorisedNoAts))
      case _                        => Future.successful(InternalServerError(genericErrorView()))
    }

  def onSubmit(implicit request: AuthenticatedRequest[_]): Future[Result] =
    atsForms.atsYearFormMapping.bindFromRequest.fold(
      formWithErrors => {
        val session = request.session + (Globals.TAXS_USER_TYPE_KEY -> Globals.TAXS_PORTAL_REFERENCE)
        atsListService.getAtsYearList map { atsListData =>
          handleServiceResult(
            atsListData,
            data => {
              val atsList = AtsList(
                data.utr,
                data.taxPayer.get.taxpayer_name.get("forename"),
                data.taxPayer.get.taxpayer_name.get("surname"),
                data.atsYearList.get.map(year => TaxYearEnd(Some(year.toString)))
              )
              Ok(taxsIndexView(atsList, formWithErrors)).withSession(session)
            }
          )
        }
      },
      value => redirectWithYear(value.year.get.toInt)
    )

  def redirectWithYear(year: Int)(implicit request: AuthenticatedRequest[_]): Future[Result] =
    atsListService.getAtsYearList map { atsListData =>
      handleServiceResult(
        atsListData,
        data => {
          val taxYearListLength = data.atsYearList.get.map(year => TaxYearEnd(Some(year.toString))).length
          Redirect(routes.AtsMainController.authorisedAtsMain.url + "?taxYear=" + year)
            .withSession(request.session + ("TaxYearListLength" -> taxYearListLength.toString))
        }
      )
    }

  // This is unused, it is only implemented to adhere to the interface
  override def obtainResult(result: ViewModel)(implicit request: AuthenticatedRequest[_]): Result =
    Ok(
      taxsIndexView(
        result,
        atsForms.atsYearFormMapping,
        getActingAsAttorneyFor(request, result.forename, result.surname, result.utr)))

  private def handleServiceResult(optData: Either[Int, AtsListData], block: AtsListData => Result): Result =
    optData match {
      case Right(value)                      => block(value)
      case Left(value) if value == NOT_FOUND => NotFound(routes.ErrorController.authorisedNoAts.url)
      case _                                 => InternalServerError(routes.ErrorController.serviceUnavailable.url)
    }

}
