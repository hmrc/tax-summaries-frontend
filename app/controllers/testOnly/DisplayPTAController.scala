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
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.libs.json.{JsArray, JsObject, JsValue}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{AccountUtils, AttorneyUtils}
import views.html.testOnly.DisplayPTAView

import scala.concurrent.ExecutionContext

class DisplayPTAController @Inject() (
  mcc: MessagesControllerComponents,
  view: DisplayPTAView,
  middleConnector: MiddleConnector
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc)
    with AccountUtils
    with AttorneyUtils
    with I18nSupport
    with Logging {

  private def getSection(data: JsValue, section: String): Seq[(String, BigDecimal, String)] = {
    val jsArray = (data \ "odsValues" \ section).as[JsArray]
    jsArray.value.toSeq.map { json =>
      val jsNode    = json.as[JsObject]
      val fieldName = (jsNode \ "fieldName").as[String]
      val amount    = (jsNode \ "amount").as[BigDecimal]
      val calculus  = (jsNode \ "calculus").as[String]
      Tuple3(fieldName, amount, calculus)
    }
  }

  def onPageLoad(taxYear: Int, utr: String): Action[AnyContent] = Action.async { implicit request =>
    middleConnector.connectToAtsSaDataPlusCalculus(taxYear, utr).map {
      case Right(data) =>
        println("\nDATA:" + data)

        val incomeTaxDataSection: Seq[(String, BigDecimal, String)]    = getSection(data = data, section = "income_tax")
        val summaryDataSection: Seq[(String, BigDecimal, String)]      = getSection(data = data, section = "summary_data")
        val incomeDataSection: Seq[(String, BigDecimal, String)]       = getSection(data = data, section = "income_data")
        val allowanceDataSection: Seq[(String, BigDecimal, String)]    =
          getSection(data = data, section = "allowance_data")
        val capitalGainsDataSection: Seq[(String, BigDecimal, String)] =
          getSection(data = data, section = "capital_gains_data")

        Ok(
          view(
            "https://www.staging.tax.service.gov.uk/auth-login-stub/gg-sign-in",
            incomeTaxDataSection,
            summaryDataSection,
            incomeDataSection,
            allowanceDataSection,
            capitalGainsDataSection
          )
        )
      case Left(e)     => throw new RuntimeException(s"Error returned, status=$e")
    }

  }
}
