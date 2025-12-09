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

package sa.controllers

import com.google.inject.Inject
import common.config.ApplicationConfig
import common.models.requests.AuthenticatedRequest
import common.models.{ErrorResponse, InvalidTaxYear}
import common.utils.{GenericViewModel, TaxYearUtil}
import common.views.html.errors.{GenericErrorView, TokenErrorView}
import play.api.mvc.MessagesControllerComponents

import scala.concurrent.{ExecutionContext, Future}

abstract class TaxYearRequest @Inject() (
  mcc: MessagesControllerComponents,
  genericErrorView: GenericErrorView,
  tokenErrorView: TokenErrorView,
  taxYearUtil: TaxYearUtil
)(implicit appConfig: ApplicationConfig, ec: ExecutionContext)
    extends TaxsController(mcc, genericErrorView, tokenErrorView) {

  def extractViewModelWithTaxYear(
    genericViewModel: Int => Future[GenericViewModel]
  )(implicit request: AuthenticatedRequest[_]): Future[Either[ErrorResponse, GenericViewModel]] =
    taxYearUtil.extractTaxYear match {
      case Right(taxYear) if taxYearUtil.isValidTaxYear(taxYear) => genericViewModel(taxYear).map(Right(_))
      case Right(taxYear)                                        =>
        Future.successful(Left(InvalidTaxYear(taxYear)))
      case Left(errorResponse)                                   => Future.successful(Left(errorResponse))
    }
}
