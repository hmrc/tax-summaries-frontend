/*
 * Copyright 2022 HM Revenue & Customs
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
import com.typesafe.scalalogging.LazyLogging
import config.ApplicationConfig
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.time.CurrentTaxYear
import views.html.errors._

import java.time.LocalDate
import scala.concurrent.ExecutionContext

class PayeErrorController @Inject() (
  mcc: MessagesControllerComponents,
  payeNotAuthorisedView: PayeNotAuthorisedView,
  payeServiceUnavailableView: PayeServiceUnavailableView
)(implicit appConfig: ApplicationConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport
    with CurrentTaxYear
    with LazyLogging {

  val payeYear                      = appConfig.taxYear
  override def now: () => LocalDate = () => LocalDate.now()

  def notAuthorised: Action[AnyContent] = Action { implicit request: Request[_] =>
    Ok(payeNotAuthorisedView())
  }

  def serviceUnavailable: Action[AnyContent] = Action { implicit request: Request[_] =>
    Ok(payeServiceUnavailableView())
  }
}
