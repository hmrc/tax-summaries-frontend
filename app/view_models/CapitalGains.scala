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

package view_models

import utils.GenericViewModel

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
  adjustmentsAmount: Amount,
  totalCapitalGainsTaxAmount: Amount,
  cgTaxPerCurrencyUnit: Amount,
  entrepreneursReliefRateRate: Rate,
  ordinaryRateRate: Rate,
  upperRateRate: Rate,
  totalCgTaxRate: Rate,
  title: String,
  forename: String,
  surname: String)
    extends GenericViewModel {
  def taxYearFrom = (taxYear - 1).toString
  def taxYearTo = taxYear.toString

  def entrepreneursReliefRate = entrepreneursReliefRateRate.percent
  def ordinaryRate = ordinaryRateRate.percent
  def upperRate = upperRateRate.percent
}
