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
import view_models.TaxYearEnd
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

  private val payeYear: Int = appConfig.payeYear
  private val isMultipleYearsEnabled: Boolean = appConfig.payeMultipleYears
  private val taxYearFromKey = "taxYearFrom"
  private val taxYearToKey = "taxYearTo"

  def show: Action[AnyContent] = payeAuthAction.async { implicit request =>
    if (isMultipleYearsEnabled) getPayeAtsMultipleYear else getPayeAts(payeYear)
  }

  def onSubmit: Action[AnyContent] = payeAuthAction { request =>
    handleOnSubmit(request)
  }

  private def getPayeAts(taxYear: Int)(implicit request: PayeAuthenticatedRequest[_]): Future[Result] =
    payeAtsService.getPayeATSData(request.nino, taxYear).map {

      case Right(_: PayeAtsData) =>
        Ok(payeTaxsMainView(PayeAtsMain(taxYear), needsBackButton = false))
      case Left(response: HttpResponse) =>
        response.status match {
          case NOT_FOUND => Redirect(controllers.paye.routes.PayeErrorController.authorisedNoAts())
          case _ =>
            Logger.error(s"Error received, Http status: ${response.status}")
            redirectToError(response.status)
        }
    }

  private def getPayeAtsMultipleYear(implicit request: PayeAuthenticatedRequest[_]): Future[Result] =
    payeAtsService.getPayeATSMultipleYearData(request.nino, payeYear - 1, payeYear) map {
      case Right(value) => routeMultipleYearResponse(value)
      case Left(response) =>
        response.status match {
          case NOT_FOUND => Redirect(controllers.paye.routes.PayeErrorController.authorisedNoAts())
          case _ =>
            Logger.error(s"Error received, Http status: ${response.status}")
            redirectToError(response.status)
        }
    }

  private def routeMultipleYearResponse(data: List[PayeAtsData])(
    implicit request: PayeAuthenticatedRequest[_]): Result =
    data.length match {
      case 0 => Redirect(controllers.paye.routes.PayeErrorController.authorisedNoAts())
      case 1 => Ok(payeTaxsMainView(PayeAtsMain(data.head.taxYear), needsBackButton = false))
      case _ =>
        val taxYears = data.map(_.taxYear)
        Ok(payeMultipleYearsView(taxYears, atsYearFormMapping)).addingToSession(
          taxYearFromKey -> taxYears.head.toString,
          taxYearToKey   -> taxYears.last.toString
        )
    }

  private def handleOnSubmit(implicit request: PayeAuthenticatedRequest[_]): Result = {
    def yearsFrom: Int = request.session(taxYearFromKey).toInt
    def yearsTo: Int = request.session(taxYearToKey).toInt
    atsYearFormMapping.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(payeMultipleYearsView((yearsFrom to yearsTo).toList, formWithErrors))
      },
      value => {
        value.year.map(_.toInt) match {
          case Some(taxYear) => Ok(payeTaxsMainView(PayeAtsMain(taxYear)))
          case _ =>
            val emptyForm = atsYearFormMapping.fill(TaxYearEnd(None))
            BadRequest(payeMultipleYearsView((yearsFrom to yearsTo).toList, emptyForm))
        }
      }
    )
  }

  private def redirectToError(status: Int): Result =
    Redirect(controllers.paye.routes.PayeErrorController.genericError(status))
}
