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

package controllers.testOnly

import com.google.inject.Inject
import config.ApplicationConfig
import forms.testOnly.EnterSearchFormProvider
import modules.testOnly.TaxYearAndUTR
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.govukfrontend.views.Aliases.SelectItem
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{AccountUtils, AttorneyUtils}
import views.html.testOnly.EnterSearchView

import scala.concurrent.Future

class EnterSearchController @Inject() (
  mcc: MessagesControllerComponents,
  view: EnterSearchView,
  formProvider: EnterSearchFormProvider,
  applicationConfig: ApplicationConfig
) extends FrontendController(mcc)
    with AccountUtils
    with AttorneyUtils
    with I18nSupport
    with Logging {

  private def taxYears: Seq[SelectItem] = {
    val currentTaxYear  = applicationConfig.taxYear
    val earliestTaxYear = currentTaxYear - 3
    (earliestTaxYear to currentTaxYear).map { year =>
      SelectItem(
        value = Some(year.toString),
        text = (year - 1).toString + "-" + year.toString,
        selected = true,
        disabled = false
      )
    }
  }

  private val utrsForTestingAdmin = Seq(
    "0000000011",
    "0000000012",
    "0000000013",
    "0000000014",
    "0000000004",
    "0000000005",
    "0000000006",
    "0000000007",
    "0000000008",
    "0000000009",
    "0000000010"
  )

  private def utrs: Seq[SelectItem] = utrsForTestingAdmin.map { utr =>
    SelectItem(value = Some(utr), text = utr, selected = true, disabled = false)
  }

  def onPageLoad: Action[AnyContent] = Action.async { implicit request =>
    val form: Form[TaxYearAndUTR] = formProvider()
    Future.successful(Ok(view(taxYears, utrs, form)))
  }

  def onSubmit: Action[AnyContent] = Action { implicit request =>
    val form: Form[TaxYearAndUTR] = formProvider()

    form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(view(taxYears, utrs, formWithErrors)),
        value => Ok("VALUES:" + value)
      )

  }

}