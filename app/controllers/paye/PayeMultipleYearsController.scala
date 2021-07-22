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

import com.typesafe.scalalogging.LazyLogging
import config.ApplicationConfig
import controllers.auth.{PayeAuthAction, PayeAuthenticatedRequest}
import javax.inject.Inject
import models.PayeAtsData
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PayeAtsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import view_models.AtsForms
import view_models.TaxYearEnd
import views.html.paye.PayeMultipleYearsView

import scala.concurrent.ExecutionContext

class PayeMultipleYearsController @Inject()(
  payeAtsService: PayeAtsService,
  payeAuthAction: PayeAuthAction,
  mcc: MessagesControllerComponents,
  payeMultipleYearsView: PayeMultipleYearsView,
  atsForms: AtsForms)(
  implicit formPartialRetriever: FormPartialRetriever,
  templateRenderer: TemplateRenderer,
  appConfig: ApplicationConfig,
  ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with LazyLogging {

  private val payeYear: Int = appConfig.payeYear
  private val taxYearFromKey = "taxYearFrom"
  private val taxYearToKey = "taxYearTo"

  def onPageLoad: Action[AnyContent] = payeAuthAction.async { implicit request =>
    payeAtsService.getPayeATSMultipleYearData(request.nino, payeYear - 1, payeYear) map {
      case Right(value) => routeMultipleYearResponse(value)
      case Left(response) =>
        response.status match {
          case NOT_FOUND => redirectToNoAts
          case _ =>
            logger.error(s"Error received, Http status: ${response.status}")
            Redirect(routes.PayeErrorController.genericError(response.status))
        }
    }
  }

  def onSubmit: Action[AnyContent] = payeAuthAction { request =>
    handleOnSubmit(request)
  }

  private def routeMultipleYearResponse(data: List[PayeAtsData])(
    implicit request: PayeAuthenticatedRequest[_]): Result =
    data.length match {
      case 0 => redirectToNoAts
      case 1 => redirectToMain(data.head.taxYear)
      case _ =>
        val taxYears = data.map(_.taxYear).reverse
        Ok(payeMultipleYearsView(taxYears, atsForms.atsYearFormMapping)).addingToSession(
          taxYearFromKey -> taxYears.last.toString,
          taxYearToKey   -> taxYears.head.toString
        )
    }

  private def handleOnSubmit(implicit request: PayeAuthenticatedRequest[_]): Result = {
    def yearsFrom: Int = request.session(taxYearFromKey).toInt
    def yearsTo: Int = request.session(taxYearToKey).toInt
    atsForms.atsYearFormMapping.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(payeMultipleYearsView((yearsFrom to yearsTo).toList.reverse, formWithErrors))
      },
      value => {
        value.year.map(_.toInt) match {
          case Some(taxYear) => redirectToMain(taxYear)
          case _ =>
            val emptyForm = atsForms.atsYearFormMapping.fill(TaxYearEnd(None))
            BadRequest(payeMultipleYearsView((yearsFrom to yearsTo).toList.reverse, emptyForm))
        }
      }
    )
  }

  private def redirectToMain(taxYear: Int): Result =
    Redirect(routes.PayeAtsMainController.show(taxYear))

  private def redirectToNoAts: Result =
    Redirect(routes.PayeErrorController.authorisedNoAts())
}
