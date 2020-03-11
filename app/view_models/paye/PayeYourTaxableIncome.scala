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

package view_models.paye

import models.{DataHolder, PayeAtsData}
import view_models.Amount

case class PayeYourTaxableIncome(
                                  taxYear: Int,
                                  incomeTaxRows: List[IncomeTaxRow],
                                  incomeBeforeTaxTaxTotal: Amount
                                ) extends TaxYearFormatting {
}

object PayeYourTaxableIncome {
  def buildViewModel(payeAtsData: PayeAtsData): Option[PayeYourTaxableIncome] = {
    val taxRows = getIncomeTaxRows(payeAtsData.income_data).filter(row => row.value.nonZero)
    val updatedTaxRows = modifyOtherIncomeMessageKey(taxRows)

    Some(PayeYourTaxableIncome(
      payeAtsData.taxYear,
      updatedTaxRows.dropRight(1),
      updatedTaxRows.last.value
      ))
  }

  def modifyOtherIncomeMessageKey(taxRows: List[IncomeTaxRow]): List[IncomeTaxRow] = {
    val noStatePension = !taxRows.exists(row => row.messageKey == "ats.income_before_tax.table.line3")
    val otherPensionIndex = taxRows.indexOf(
        taxRows.find(_.messageKey == "ats.income_before_tax.table.line4")
               .getOrElse(IncomeTaxRow("", Amount.empty)))
    if(noStatePension && (otherPensionIndex >= 0))
      taxRows
        .updated(otherPensionIndex, taxRows(otherPensionIndex)
        .copy(messageKey = "paye.ats.income_before_tax.table.line4"))
    else
      taxRows
  }

  val lookupMessageKey =  List (
    ("self_employment_income", "ats.income_before_tax.table.line1"),
    ("income_from_employment", "ats.income_before_tax.table.line2"),
    ("state_pension", "ats.income_before_tax.table.line3"),
    ("other_pension_income", "ats.income_before_tax.table.line4"),
    ("taxable_state_benefits", "ats.income_before_tax.table.line5"),
    ("other_income", "ats.income_before_tax.table.line6"),
    ("benefits_from_employment", "ats.income_before_tax.table.line7"),
    ("total_income_before_tax", "paye.ats.income_before_tax.html.title")
  )
  
  def createRows(data: Map[String, Amount]): List[IncomeTaxRow] = {
    lookupMessageKey.map{ case(key, messageKey) => {
            IncomeTaxRow(messageKey, data.getOrElse(key, Amount.empty))
      }
    }
  }

  def getIncomeTaxRows(incomeData: Option[DataHolder]) : List[IncomeTaxRow] ={
    incomeData match {
      case Some(data) => createRows(data.payload.getOrElse(Map()))
      case None => List()
    }
  }
}

case class IncomeTaxRow(messageKey: String, value: Amount)
