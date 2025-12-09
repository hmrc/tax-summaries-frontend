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

package common.view_models

import common.utils.GenericViewModel

case class CapitalGains(
  taxYear: Int,
  utr: String,
  taxableGains: Amount,
  lessTaxFreeAmount: Amount,
  payCgTaxOn: Amount,
  entrepreneursReliefRateBefore: Amount,
  entrepreneursReliefRateAmount: Amount,
  ordinaryRateBefore: Amount,
  ordinaryRateAmount: Amount,
  upperRateBefore: Amount,
  upperRateAmount: Amount,
  rpciLowerTax: Amount,
  rpciLowerTotalAmount: Amount,
  rpciHigherTax: Amount,
  rpciHigherTotalAmount: Amount,
  ciLowerTax: Amount,
  ciLowerTotalAmount: Amount,
  ciHigherTax: Amount,
  ciHigherTotalAmount: Amount,
  rpLowerTax: Amount,
  rpLowerTotalAmount: Amount,
  rpHigherTax: Amount,
  rpHigherTotalAmount: Amount,
  adjustmentsAmount: Amount,
  totalCapitalGainsTaxAmount: Amount,
  cgTaxPerCurrencyUnit: Amount,
  entrepreneursReliefRateRate: Rate,
  ordinaryRateRate: Rate,
  upperRateRate: Rate,
  rpciLowerRate: Rate,
  rpciHigherRate: Rate,
  ciLowerRate: Rate,
  ciHigherRate: Rate,
  rpLowerRate: Rate,
  rpHigherRate: Rate,
  totalCgTaxRate: Rate,
  title: String,
  forename: String,
  surname: String
) extends GenericViewModel
    with TaxYearFormatting {

  def entrepreneursReliefRate: String = entrepreneursReliefRateRate.percent
  def ordinaryRate: String            = ordinaryRateRate.percent
  def upperRate: String               = upperRateRate.percent
}
