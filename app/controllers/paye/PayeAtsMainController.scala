/*
 * Copyright 2020 HM Revenue & Customs
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

import com.google.inject.Inject
import config.ApplicationConfig
import controllers.auth.{PayeAuthAction, PayeAuthenticatedRequest}
import models.PayeAtsData
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PayeAtsService
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import uk.gov.hmrc.play.partials.FormPartialRetriever
import view_models.AtsForms.atsYearFormMapping
import view_models.paye.PayeAtsMain
import views.html.paye.{PayeMultipleYearsView, PayeTaxsMainView}

import scala.concurrent.{ExecutionContext, Future}

class PayeAtsMainController @Inject()(
  payeAtsService: PayeAtsService,
  payeAuthAction: PayeAuthAction,
  mcc: MessagesControllerComponents,
  payeTaxsMainView: PayeTaxsMainView,
  payeMultipleYearsView: PayeMultipleYearsView)(
  implicit formPartialRetriever: FormPartialRetriever,
  appConfig: ApplicationConfig,
  ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  val payeYear: Int = appConfig.payeYear
  val isMultipleYearsEnabled: Boolean = appConfig.payeMultipleYears
  val taxYearFromKey = "taxYearFrom"
  val taxYearToKey = "taxYearTo"

  def show: Action[AnyContent] = payeAuthAction.async { implicit request =>
    if (isMultipleYearsEnabled) getPayeAtsMultipleYear else getPayeAts(payeYear)
  }

  def onSubmit: Action[AnyContent] = payeAuthAction { request =>
    handleOnSubmit(request)
  }

  private def getPayeAts(taxYear: Int = payeYear)(implicit request: PayeAuthenticatedRequest[_]): Future[Result] =
    payeAtsService.getPayeATSData(request.nino, taxYear).map {

      case Right(_: PayeAtsData) =>
        Ok(payeTaxsMainView(PayeAtsMain(taxYear), needsBackButton = false))
      case Left(response: HttpResponse) =>
        response.status match {
          case NOT_FOUND => Redirect(controllers.paye.routes.PayeErrorController.authorisedNoAts())
          case _ =>
            Logger.error(s"Error received, Http status: ${response.status}")
            Redirect(controllers.paye.routes.PayeErrorController.genericError(response.status))
        }
    }

  private def getPayeAtsMultipleYear(implicit request: PayeAuthenticatedRequest[_]): Future[Result] =
    payeAtsService.getPayeATSMultipleYearData(request.nino, payeYear, payeYear + 1) map {
      case Right(value) => routeMultipleYearResponse(value)
      case Left(response) =>
        response.status match {
          case NOT_FOUND => Redirect(controllers.paye.routes.PayeErrorController.authorisedNoAts())
          case _ =>
            Logger.error(s"Error received, Http status: ${response.status}")
            Redirect(controllers.paye.routes.PayeErrorController.genericError(response.status))
        }
    }

  private def routeMultipleYearResponse(data: List[PayeAtsData])(
    implicit request: PayeAuthenticatedRequest[_]): Result =
    data.length match {
      case 0 => Redirect(controllers.paye.routes.PayeErrorController.authorisedNoAts())
      case 1 => Ok(payeTaxsMainView(PayeAtsMain(payeYear), needsBackButton = false))
      case _ =>
        val taxYears = data.map(_.taxYear)
        Ok(payeMultipleYearsView(taxYears, atsYearFormMapping)).addingToSession(
          taxYearFromKey -> taxYears.head.toString,
          taxYearToKey   -> taxYears.last.toString
        )
    }

  private def handleOnSubmit(implicit request: PayeAuthenticatedRequest[_]): Result =
    atsYearFormMapping.bindFromRequest.fold(
      formWithErrors => {
        def yearsFrom: Int = request.session(taxYearFromKey).toInt
        def yearsTo: Int = request.session(taxYearToKey).toInt
        BadRequest(payeMultipleYearsView((yearsFrom to yearsTo).toList, formWithErrors))
      },
      value => {
        val year = value.year.map(_.toInt).getOrElse(payeYear)
        Ok(payeTaxsMainView(PayeAtsMain(year)))
      }
    )
}
