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

import config.ApplicationConfig
import models.{GovernmentSpendingOutputWrapper, PayeAtsData, SpendData}
import utils.CategoriesUtils
import view_models.Amount

case class PayeGovernmentSpend(taxYear: Int, orderedSpendRows: List[SpendRow], totalAmount: Amount, isScottish: Boolean)
    extends TaxYearFormatting

object PayeGovernmentSpend {

  def apply(payeAtsData: PayeAtsData, appConfig: ApplicationConfig): PayeGovernmentSpend = {

    val spendRows: List[SpendRow] =
      payeAtsData.gov_spending
        .flatMap { govSpending: GovernmentSpendingOutputWrapper =>
          govSpending.govSpendAmountData
            .map { govSpendAmountDataMap =>
              val sortedSpendData  = govSpendAmountDataMap.toList.sortWith(_._2.percentage > _._2.percentage)
              val orderedSpendData =
                CategoriesUtils.reorderCategories(appConfig, payeAtsData.taxYear, sortedSpendData)

              for ((category, spendData) <- orderedSpendData)
                yield SpendRow(category, spendData)
            }
        }
        .getOrElse(List(SpendRow("", SpendData(Amount.empty, 0.0))))

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
