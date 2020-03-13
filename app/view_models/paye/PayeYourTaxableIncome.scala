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

    Some(PayeYourTaxableIncome(
      payeAtsData.taxYear,
      taxRows.dropRight(1),
      taxRows.last.value
      ))
  }

  def getIncomeTaxRows(incomeData: Option[DataHolder]) : List[IncomeTaxRow] ={
    val selfEmploymentIncome = incomeData.flatMap(_.payload).flatMap(_.get("self_employment_income")).getOrElse(Amount.empty)
    val incomeFromEmployment = incomeData.flatMap(_.payload).flatMap(_.get("income_from_employment")).getOrElse(Amount.empty)
    val statePension = incomeData.flatMap(_.payload).flatMap(_.get("state_pension")).getOrElse(Amount.empty)
    val otherPensionIncome = incomeData.flatMap(_.payload).flatMap(_.get("other_pension_income")).getOrElse(Amount.empty)
    val taxableStateBenefits = incomeData.flatMap(_.payload).flatMap(_.get("taxable_state_benefits")).getOrElse(Amount.empty)
    val otherIncome = incomeData.flatMap(_.payload).flatMap(_.get("other_income")).getOrElse(Amount.empty)
    val benefitsFromEmployment = incomeData.flatMap(_.payload).flatMap(_.get("benefits_from_employment")).getOrElse(Amount.empty)
    val totalIncomeBeforeTax = incomeData.flatMap(_.payload).flatMap(_.get("total_income_before_tax")).getOrElse(Amount.empty)

    List(
      IncomeTaxRow("self_employment_income", selfEmploymentIncome),
      IncomeTaxRow("income_from_employment", incomeFromEmployment),
      IncomeTaxRow("state_pension", statePension),
      IncomeTaxRow(if(statePension == Amount.empty) "personal_pension_income" else "other_pension_income", otherPensionIncome),
      IncomeTaxRow("taxable_state_benefits", taxableStateBenefits),
      IncomeTaxRow("other_income", otherIncome),
      IncomeTaxRow("benefits_from_employment", benefitsFromEmployment),
      IncomeTaxRow("total_income_before_tax", totalIncomeBeforeTax)
    )
  }
}

case class IncomeTaxRow(messageKey: String, value: Amount)


/*
def apply(payeAtsData: PayeAtsData): PayeTaxFreeAmount = {
    val totalTaxFreeAmount = payeAtsData.summary_data.flatMap(_.payload).flatMap(_.get("total_tax_free_amount")).getOrElse(Amount.empty)
    val liableTaxAmount = payeAtsData.summary_data.flatMap(_.payload).flatMap(_.get("liable_tax_amount")).getOrElse(Amount.empty)
    val totalIncomeBeforeTax = payeAtsData.summary_data.flatMap(_.payload).flatMap(_.get("total_income_before_tax")).getOrElse(Amount.empty)
    val personalTaxFreeAmount = payeAtsData.allowance_data.flatMap(_.payload).flatMap(_.get("personal_tax_free_amount")).getOrElse(Amount.empty)

    val adjustmentRows = (for {
      allowanceData <- payeAtsData.allowance_data
      payload <- allowanceData.payload
    } yield {
      adjustments.map { adjustment =>
        AmountRow(adjustment, payload.getOrElse(adjustment, Amount.empty))
      }.filter(_.amount != Amount.empty)
    }).getOrElse(List.empty)

    val summaryRows = List(
      AmountRow("income_before_tax", totalIncomeBeforeTax),
      AmountRow("tax_free_amount", if (totalTaxFreeAmount == Amount.empty) personalTaxFreeAmount else totalTaxFreeAmount)
    )

    PayeTaxFreeAmount(payeAtsData.taxYear, adjustmentRows, totalTaxFreeAmount, summaryRows, liableTaxAmount)
  }
 */