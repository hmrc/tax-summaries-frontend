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

package utils

object TaxsBreadcrumbLinks {

  private val links = Map(
    "breadcrumbs.taxs.index"             -> controllers.routes.AtsMainController.authorisedAtsMain,
    "breadcrumbs.taxs.summary"           -> controllers.routes.SummaryController.authorisedSummaries,
    "breadcrumbs.taxs.nics"              -> controllers.routes.NicsController.authorisedNics,
    "breadcrumbs.taxs.treasury_spending" -> controllers.routes.GovernmentSpendController.authorisedGovernmentSpendData,
    "breadcrumbs.taxs.income_before_tax" -> controllers.routes.IncomeController.authorisedIncomeBeforeTax,
    "breadcrumbs.taxs.tax_free_amount"   -> controllers.routes.AllowancesController.authorisedAllowance,
    "breadcrumbs.taxs.total_income_tax"  -> controllers.routes.TotalIncomeTaxController.authorisedTotalIncomeTax,
    "breadcrumbs.taxs.capital_gains_tax" -> controllers.routes.CapitalGainsTaxController.authorisedCapitalGains,
    "breadcrumbs.taxs.select_tax_year"   -> controllers.routes.IndexController.authorisedIndex
  )

  def getLink(key: String) = links.get(key)
}
