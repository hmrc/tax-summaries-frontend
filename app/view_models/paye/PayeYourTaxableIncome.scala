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

package view_models.paye

import models.{DataHolder, PayeAtsData}
import view_models.Amount

case class PayeYourTaxableIncome(
  taxYear: Int,
  incomeTaxRows: List[IncomeTaxRow],
  totalIncomeBeforeTax: Amount
) extends TaxYearFormatting {}

object PayeYourTaxableIncome {
  def buildViewModel(payeAtsData: PayeAtsData): PayeYourTaxableIncome = {
    val taxRows = getIncomeTaxRows(payeAtsData.income_data).filter(row => row.value.nonZero)

    PayeYourTaxableIncome(payeAtsData.taxYear, taxRows, getTotalIncomeBeforeTax(payeAtsData.income_data))
  }

  private def getTotalIncomeBeforeTax(incomeData: Option[DataHolder]): Amount =
    incomeData.flatMap(_.payload).flatMap(_.get("total_income_before_tax")).getOrElse(Amount.empty)

  private def getIncomeTaxRows(incomeData: Option[DataHolder]): List[IncomeTaxRow] = {
    val selfEmploymentIncome   = Amount(incomeData.flatMap(_.payload).flatMap(_.get("self_employment_income")))
    val incomeFromEmployment   = Amount(incomeData.flatMap(_.payload).flatMap(_.get("income_from_employment")))
    val statePension           = Amount(incomeData.flatMap(_.payload).flatMap(_.get("state_pension")))
    val otherPensionIncome     = Amount(incomeData.flatMap(_.payload).flatMap(_.get("other_pension_income")))
    val taxableStateBenefits   = Amount(incomeData.flatMap(_.payload).flatMap(_.get("taxable_state_benefits")))
    val otherIncome            = Amount(incomeData.flatMap(_.payload).flatMap(_.get("other_income")))
    val benefitsFromEmployment = Amount(incomeData.flatMap(_.payload).flatMap(_.get("benefits_from_employment")))

    List(
      IncomeTaxRow("self_employment_income", selfEmploymentIncome),
      IncomeTaxRow("income_from_employment", incomeFromEmployment),
      IncomeTaxRow("state_pension", statePension),
      IncomeTaxRow(
        if (statePension.isValueEqual(Amount.empty)) "personal_pension_income" else "other_pension_income",
        otherPensionIncome
      ),
      IncomeTaxRow("taxable_state_benefits", taxableStateBenefits),
      IncomeTaxRow("other_income", otherIncome),
      IncomeTaxRow("benefits_from_employment", benefitsFromEmployment)
    )
  }
}

case class IncomeTaxRow(messageKey: String, value: Amount)
