/*
 * Copyright 2021 HM Revenue & Customs
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

package view_models

import config.ApplicationConfig
import models.SpendData
import utils.{CategoriesUtils, GenericViewModel}
import view_models.paye.TaxYearFormatting

case class GovernmentSpend(
  taxYear: Int,
  userUtr: String,
  govSpendAmountData: List[(String, SpendData)],
  userTitle: String,
  userForename: String,
  userSurname: String,
  totalAmount: Amount,
  incomeTaxStatus: String,
  scottishIncomeTax: Amount)
    extends GenericViewModel with TaxYearFormatting {

  val isScottishTaxPayer: Boolean = (incomeTaxStatus == "0002")

  def sortedSpendData: List[(String, SpendData)] =
    govSpendAmountData.filter(_._1 != "GovSpendTotal").sortWith(_._2.percentage > _._2.percentage)

  def filteredDataWithHigherTransport(appConfig: ApplicationConfig): List[(String, SpendData)] =
    CategoriesUtils.reorderCategories(appConfig, taxYear, sortedSpendData)
}
