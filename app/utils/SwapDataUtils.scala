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

object SwapDataUtils {

  def swapDataForSa[A](coll: List[(String, A)], key1: String, key2: String): List[(String, A)] =
    coll.map {
      case (key, value) if key == key1 => (key2, value)
      case (key, value) if key == key2 => (key1, value)
      case default @ _                 => default
    }

  def swapDataForPaye[A](coll: List[SpendRow], key1: String, key2: String): List[SpendRow] =
    coll.map {
      case (SpendRow(key, value)) if key == key1 => SpendRow(key2, value)
      case (SpendRow(key, value)) if key == key2 => SpendRow(key1, value)
      case default @ _                           => default
    }
}
