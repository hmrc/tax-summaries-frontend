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

package common.utils

import com.google.inject.Inject
import common.view_models.{Amount, Rate}

class ViewUtils @Inject() () {

  def toCurrency(amount: Amount, twoDecimalPlaces: Boolean = false): String =
    s"&pound;${if (twoDecimalPlaces) amount.toTwoDecimalString else amount}"

  def positiveOrZero(currentAmount: Amount): Amount =
    currentAmount.copy(amount = currentAmount.amount.max(BigDecimal(0)))

  def positiveOrZero(currentRate: Rate): Rate =
    if (currentRate.percent.head == '-')
      Rate.empty
    else
      currentRate
}
