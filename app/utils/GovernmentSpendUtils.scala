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

import view_models.paye.SpendRow

object GovernmentSpendUtils {

  def reorderDataBasedOnCategories[A](coll: List[(String, A)], key1: String, key2: String): List[(String, A)] =
    coll.map {
      case (category, spendData) if category == key1 => (key2, spendData)
      case (category, spendData) if category == key2 => (key1, spendData)
      case default @ _                               => default
    }

  def reorderPayeDataBasedOnCategories[A](coll: List[SpendRow], key1: String, key2: String): List[SpendRow] =
    coll.map {
      case (SpendRow(category, spendData)) if category == key1 => SpendRow(key2, spendData)
      case (SpendRow(category, spendData)) if category == key2 => SpendRow(key1, spendData)
      case default @ _                                         => default
    }
}
