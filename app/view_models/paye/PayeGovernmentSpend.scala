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

import models.{GovernmentSpendingOutputWrapper, PayeAtsData, SpendData}
import view_models.Amount

case class PayeGovernmentSpend(taxYear: Int, orderedSpendRows: List[SpendRow], totalAmount: Amount, isScottish: Boolean)
    extends TaxYearFormatting

object PayeGovernmentSpend {

  def apply(payeAtsData: PayeAtsData): PayeGovernmentSpend = {

    val spendRows: List[SpendRow] = {
      val govSpendAmountDataList = payeAtsData.gov_spending
        .flatMap { govSpending: GovernmentSpendingOutputWrapper =>
          {
            govSpending.govSpendAmountData
              .map { govSpendAmountDataMap =>
                for { (category, spendData) <- govSpendAmountDataMap } yield SpendRow(category, spendData)
              }
              .map(spendRow => spendRow.toList.sortWith(_.spendData.percentage > _.spendData.percentage))
          }
        }
        .getOrElse(List(SpendRow("", SpendData(Amount.empty, 0.0))))

      val transport = "Transport"
      val publicOrder = "PublicOrderAndSafety"

      govSpendAmountDataList.map {
        case (SpendRow(category, spendData)) if category == transport   => SpendRow(publicOrder, spendData)
        case (SpendRow(category, spendData)) if category == publicOrder => SpendRow(transport, spendData)
        case default @ _                                                => default
      }
    }

    val totalSpendingAmount = payeAtsData.gov_spending
      .map { spending =>
        spending.totalAmount
      }
      .getOrElse(Amount.empty)

    val isScottish = payeAtsData.income_tax
      .flatMap(incomeTax => incomeTax.payload.flatMap(_.get("scottish_total_tax")))
      .exists(_.nonZero)

    PayeGovernmentSpend(payeAtsData.taxYear, spendRows, totalSpendingAmount, isScottish)
  }
}

case class SpendRow(category: String, spendData: SpendData)
