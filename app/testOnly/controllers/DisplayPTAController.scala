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
import common.models._
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import testOnly.views.html.DisplayPTAView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import common.utils.{AccountUtils, AttorneyUtils}
import testOnly.connectors.TaxSummariesConnector
import testOnly.models.FieldInfo

import scala.concurrent.ExecutionContext

class DisplayPTAController @Inject() (
  mcc: MessagesControllerComponents,
  view: DisplayPTAView,
  taxSummariesConnector: TaxSummariesConnector
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc)
    with AccountUtils
    with AttorneyUtils
    with I18nSupport
    with Logging {

  private def getSection(fields: Seq[FieldInfo]): Seq[(String, BigDecimal, String)] =
    fields.map { fieldInfo =>
      val fieldName = fieldInfo.fieldNameCamelCase
      val amount    = fieldInfo.amount
      val calculus  = fieldInfo.calculus
      Tuple3(fieldName, amount, calculus)
    }

  def onPageLoad(taxYear: Int, utr: String): Action[AnyContent] = Action.async { implicit request =>
    taxSummariesConnector.connectToAtsSaDataWithoutAuth(taxYear, utr).map {
      case Right(json) =>
        val atsData                                                    = json.as[AtsData]
        val incomeTaxDataSection: Seq[(String, BigDecimal, String)]    =
          getSection(atsData.income_tax.map(createSeqFieldInfo).getOrElse(Nil))
        val summaryDataSection: Seq[(String, BigDecimal, String)]      =
          getSection(atsData.summary_data.map(createSeqFieldInfo).getOrElse(Nil))
        val incomeDataSection: Seq[(String, BigDecimal, String)]       =
          getSection(atsData.income_data.map(createSeqFieldInfo).getOrElse(Nil))
        val allowanceDataSection: Seq[(String, BigDecimal, String)]    =
          getSection(atsData.allowance_data.map(createSeqFieldInfo).getOrElse(Nil))
        val capitalGainsDataSection: Seq[(String, BigDecimal, String)] =
          getSection(atsData.capital_gains_data.map(createSeqFieldInfo).getOrElse(Nil))
        val taxLiability: Option[BigDecimal]                           = atsData.taxLiability.map(_.amount)
        Ok(
          view(
            Seq(
              Tuple2("Income tax data", incomeTaxDataSection),
              Tuple2("Summary data", summaryDataSection),
              Tuple2("Income data", incomeDataSection),
              Tuple2("Allowance data", allowanceDataSection),
              Tuple2("Capital gains data", capitalGainsDataSection)
            ),
            taxLiability
          )
        )
      case Left(e)     => throw e
    }

  }

  private def createSeqFieldInfo(dataHolder: DataHolder): Seq[FieldInfo] = {
    val dataHolderWithCalculusList =
      dataHolder.payload match {
        case Some(payload) =>
          payload.map(liabilityAmountMap =>
            FieldInfo(
              liabilityAmountMap._1,
              liabilityAmountMap._2.amount,
              liabilityAmountMap._2.calculus.getOrElse("")
            )
          )

        case _ => List.empty
      }
    dataHolderWithCalculusList.toList
  }

}
