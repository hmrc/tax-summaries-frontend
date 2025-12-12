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

package testOnly.controllers

import com.google.inject.Inject
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import testOnly.views.html.EnterODSView
import uk.gov.hmrc.govukfrontend.views.Aliases.SelectItem
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import common.utils.{AccountUtils, AttorneyUtils}
import testOnly.connectors.{TaxSummariesConnector, TaxSummariesStubsConnector}
import testOnly.forms.EnterODSFormProvider
import testOnly.models.CountryAndODSValues

import scala.collection.immutable.ListMap
import scala.concurrent.{ExecutionContext, Future}

class EnterODSController @Inject() (
  mcc: MessagesControllerComponents,
  view: EnterODSView,
  formProvider: EnterODSFormProvider,
  taxSummariesConnector: TaxSummariesConnector,
  taxSummariesStubsConnector: TaxSummariesStubsConnector
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
      text = "Scotland",
      selected = false,
      disabled = false
    ),
    SelectItem(
      value = Some("0003"),
      text = "Wales",
      selected = false,
      disabled = false
    )
  )

  private val compareString: (String, String) => Boolean = (s1, s2) => s1.toLowerCase < s2.toLowerCase

  def onPageLoad(taxYear: Int, utr: String): Action[AnyContent] = Action.async { implicit request =>
    taxSummariesStubsConnector.get(taxYear, utr).flatMap { saODSModel =>
      taxSummariesConnector.connectToAtsSaFields(taxYear).map {
        case Right(validOdsFieldNames) =>
          val odsValues: Map[String, String]           = if (saODSModel.odsValues.nonEmpty) {
            val seqTuples = saODSModel.odsValues.map(odsValue => odsValue.fieldName -> odsValue.amount.toString)
            ListMap(seqTuples: _*)
          } else {
            ListMap(validOdsFieldNames.sortWith(compareString).map(_ -> "0.00"): _*)
          }
          val countryAndODSValues: CountryAndODSValues = CountryAndODSValues(saODSModel.country, odsValues)
          val form: Form[CountryAndODSValues]          = formProvider(validOdsFieldNames).fill(countryAndODSValues)
          val submitCall: Call                         = testOnly.controllers.routes.EnterODSController.onSubmit(taxYear, utr)
          Ok(view(submitCall, countries, form))
        case Left(e)                   => throw e
      }
    }
  }

  def onSubmit(taxYear: Int, utr: String): Action[AnyContent] = Action.async { implicit request =>
    taxSummariesConnector.connectToAtsSaFields(taxYear).flatMap {
      case Right(validOdsFieldNames) =>
        val form: Form[CountryAndODSValues] = formProvider(validOdsFieldNames)
        val submitCall: Call                = testOnly.controllers.routes.EnterODSController.onSubmit(taxYear, utr)
        form
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(BadRequest(view(submitCall, countries, formWithErrors))),
            value => {
              val validOdsFieldsWithZeroValues: ListMap[String, String] =
                ListMap(validOdsFieldNames.sortWith(compareString).map(_ -> "0.00"): _*)
              val updatedValue                                          =
                CountryAndODSValues(
                  country = value.country,
                  odsValues = validOdsFieldsWithZeroValues ++ value.odsValues
                )
              taxSummariesStubsConnector.save(taxYear, utr, updatedValue).map { _ =>
                Redirect(testOnly.controllers.routes.DisplayPTAController.onPageLoad(taxYear, utr))
              }
            }
          )
      case Left(e)                   => throw e
    }

  }
}
