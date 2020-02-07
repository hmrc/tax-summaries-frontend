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

package utils

object PayeTaxsBreadcrumbLinks {

  private val links = Map(
    "paye.breadcrumbs.taxs.index"             -> controllers.paye.routes.PayeAtsMainController.authorisedAtsMain,
    "breadcrumbs.taxs.summary"                -> controllers.paye.routes.PayeSummaryController.authorisedSummaries,
    "breadcrumbs.taxs.nics"                   -> controllers.paye.routes.PayeNicsController.authorisedNics,
    "paye.breadcrumbs.taxs.treasury_spending" -> controllers.paye.routes.PayeGovernmentSpendController.authorisedGovernmentSpendData,
    "breadcrumbs.taxs.income_before_tax"      -> controllers.paye.routes.PayeIncomeController.authorisedIncomeBeforeTax,
    "breadcrumbs.taxs.tax_free_amount"        -> controllers.paye.routes.PayeAllowancesController.authorisedAllowance,
    "breadcrumbs.taxs.total_income_tax"       -> controllers.paye.routes.PayeTotalIncomeTaxController.authorisedTotalIncomeTax
  )

  def getLink(key: String) = links.get(key)
}