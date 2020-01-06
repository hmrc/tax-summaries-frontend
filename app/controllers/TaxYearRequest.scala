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

package controllers

import controllers.auth.AuthenticatedRequest
import models.{ErrorResponse, InvalidTaxYear}
import play.api.mvc.Result
import utils.{GenericViewModel, TaxYearUtil, TaxsController}
import view_models.NoATSViewModel
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import scala.concurrent.Future

trait TaxYearRequest extends TaxsController {

  def extractViewModelWithTaxYear(genericViewModel: Int => Future[GenericViewModel])(implicit request: AuthenticatedRequest[_]):
    Future[Either[ErrorResponse, GenericViewModel]] = {
      TaxYearUtil.extractTaxYear match {
        case Right(taxYear) => genericViewModel(taxYear).map(Right(_))
        case Left(errorResponse) => Future.successful(Left(errorResponse))
    }
  }

  def transformation(implicit request: AuthenticatedRequest[_]): Future[Result] = {
    extractViewModel map {
      case Right(noAts: NoATSViewModel) => Redirect(routes.ErrorController.authorisedNoAts())
      case Right(result: ViewModel) => obtainResult(result)
      case Left(InvalidTaxYear) => BadRequest(views.html.errors.generic_error())
    }
  }
}
