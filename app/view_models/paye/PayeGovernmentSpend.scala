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

import models.PayeAtsData
import view_models.Amount

case class PayeGovernmentSpend(
  taxYear: Int,
  orderedSpendRows: List[SpendRow],
  totalAmount: Amount)

object PayeGovernmentSpend {

  val orderedSpendCategories: List[String] = List(
    "welfare",
    "health",
    "pension",
    "education",
    "defence",
    "national_debt_interest",
    "transport",
    "criminal_justice",
    "business_and_industry",
    "government_administration",
    "housing_and_utilities",
    "environment",
    "culture",
    "overseas_aid",
    "uk_contribution_to_eu_budget"
  )

  def buildViewModel(payeAtsData: PayeAtsData): PayeGovernmentSpend = {

    val spendRows: List[SpendRow] = orderedSpendCategories.flatMap(
      category => {
        payeAtsData.gov_spending.flatMap {
          govSpending =>
            govSpending.govSpendAmountData.map {
              spendDataMap => {
                val spending = spendDataMap(category)
                SpendRow(category, spending.percentage, spending.amount)
              }
            }
        }
      }
    )

    val totalSpendingAmount = payeAtsData.gov_spending.map {
      spending =>
        spending.totalAmount
    }

    PayeGovernmentSpend(payeAtsData.taxYear, spendRows, totalSpendingAmount.getOrElse(Amount(BigDecimal(0.0), "GBP")))
  }
}

case class SpendRow(category: String, percentage: BigDecimal, amount: Amount)

