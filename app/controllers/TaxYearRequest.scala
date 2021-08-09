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
import controllers.auth.AuthenticatedRequest
import models.ErrorResponse
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.renderer.TemplateRenderer
import utils.{GenericViewModel, TaxYearUtil}
import views.html.errors.{GenericErrorView, TokenErrorView}

import scala.concurrent.{ExecutionContext, Future}

abstract class TaxYearRequest @Inject()(
  mcc: MessagesControllerComponents,
  genericErrorView: GenericErrorView,
  tokenErrorView: TokenErrorView)(
  implicit templateRenderer: TemplateRenderer,
  appConfig: ApplicationConfig,
  ec: ExecutionContext)
    extends TaxsController(mcc, genericErrorView, tokenErrorView) {

  def extractViewModelWithTaxYear(genericViewModel: Int => Future[GenericViewModel])(
    implicit request: AuthenticatedRequest[_]): Future[Either[ErrorResponse, GenericViewModel]] =
    TaxYearUtil.extractTaxYear match {
      case Right(taxYear)      => genericViewModel(taxYear).map(Right(_))
      case Left(errorResponse) => Future.successful(Left(errorResponse))
    }
}
