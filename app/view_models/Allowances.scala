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

case class Allowances(
  taxYear: Int,
  utr: String,
  taxFreeAllowance: Amount,
  marriageAllowanceTransferred: Amount,
  otherAllowances: Amount,
  youPayTaxOn: Amount,
  totalTaxFree: Amount,
  totalIncomeBeforeTax: Amount,
  title: String,
  forename: String,
  surname: String)
    extends GenericViewModel {
  def isPaye = utr.isEmpty
  def year = taxYear
  def taxYearTo = taxYear.toString
  def taxYearFrom = (taxYear - 1).toString
}
