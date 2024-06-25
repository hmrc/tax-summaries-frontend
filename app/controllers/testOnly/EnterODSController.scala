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
import connectors.MiddleConnector
import forms.testOnly.EnterODSFormProvider
import models.testOnly.CountryAndODSValues
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.govukfrontend.views.Aliases.SelectItem
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{AccountUtils, AttorneyUtils}
import views.html.testOnly.EnterODSView

import scala.concurrent.ExecutionContext

class EnterODSController @Inject() (
  mcc: MessagesControllerComponents,
  view: EnterODSView,
  formProvider: EnterODSFormProvider,
  middleConnector: MiddleConnector
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc)
    with AccountUtils
    with AttorneyUtils
    with I18nSupport
    with Logging {

  private val countries = Seq(
    SelectItem(
      value = Some("0001"),
      text = "England",
      selected = false,
      disabled = false
    ),
    SelectItem(
      value = Some("0002"),
      text = "Wales",
      selected = false,
      disabled = false
    ),
    SelectItem(
      value = Some("0003"),
      text = "Scotland",
      selected = false,
      disabled = false
    )
  )

  def onPageLoad(taxYear: Int, utr: String): Action[AnyContent] = Action.async { implicit request =>
    // TODO: 9032 - connect to stubs and retrieve for utr/ tax year + if present populate with values
    middleConnector.connectToAtsSaFields(taxYear).map {
      case Right(validOdsFieldNames) =>
        // TODO: 9032 - if odsValues is empty then default to validOdsFieldNames as key value pairs
        val form: Form[CountryAndODSValues] = formProvider(validOdsFieldNames)
        val submitCall: Call                = controllers.testOnly.routes.EnterODSController.onSubmit(taxYear, utr)
        Ok(view(submitCall, countries, form))
      case Left(e)                   => throw new RuntimeException(s"Error returned, status=$e")
    }
  }

  def onSubmit(taxYear: Int, utr: String): Action[AnyContent] = Action.async { implicit request =>
    middleConnector.connectToAtsSaFields(taxYear).map {
      case Right(validOdsFieldNames) =>
        val form: Form[CountryAndODSValues] = formProvider(validOdsFieldNames)
        val submitCall: Call                = controllers.testOnly.routes.EnterODSController.onSubmit(taxYear, utr)
        form
          .bindFromRequest()
          .fold(
            formWithErrors => BadRequest(view(submitCall, countries, formWithErrors)),
            value =>
              // TODO: 9032 - save key value pairs to stubs
              Ok("VALUES:" + value)
          )
      case Left(e)                   => throw new RuntimeException(s"Error returned, status=$e")
    }

  }
}
