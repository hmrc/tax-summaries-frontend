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

import config.{AppFormPartialRetriever, ApplicationConfig}
import controllers.auth.{PayeAuthAction, PayeAuthenticatedRequest}
import play.api.Play
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import view_models.Amount
import view_models.paye.PayeYourIncomeAndTaxes

import scala.concurrent.Future

object PayeYourIncomeAndTaxesController extends PayeYourIncomeAndTaxesController{

  override val payeAuthAction = Play.current.injector.instanceOf[PayeAuthAction]
  override val payeYear: Int = ApplicationConfig.payeYear
}

trait PayeYourIncomeAndTaxesController extends FrontendController {

  implicit val formPartialRetriever = AppFormPartialRetriever

  val payeAuthAction: PayeAuthAction
  val payeYear: Int

  def show: Action[AnyContent] = payeAuthAction.async {
    implicit request: PayeAuthenticatedRequest[_] => {

      val blankViewModel = PayeYourIncomeAndTaxes(
        taxYear = 2019,
        employeeContributions = false,
        incomeBeforeTax = Amount(200, "GBP"),
        taxableIncome = Some(Amount(200, "GBP")),
        totalIncomeTax = Amount(200, "GBP"),
        totalIncomeTaxNics = Amount(200, "GBP"),
        incomeAfterTaxNics = Amount(200, "GBP"),
        averageTaxRate = Amount(200, "GBP"),
        taxFreeAmount =  Amount(200, "GBP"))

      Future.successful(Ok(views.html.paye.paye_your_income_and_taxes(blankViewModel)))


    }

  }
}
