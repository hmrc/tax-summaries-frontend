/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.libs.json._
import utils.TestConstants._

import scala.io.Source

object JsonUtil extends JsonUtil

trait JsonUtil {

  lazy val dummyDataMap = Map("$utr" -> testUtr)

  def load(path: String): String =
    Source.fromURL(getClass.getResource(path)).mkString

  def loadAndParseJsonWithDummyData(path: String): JsValue =
    Json.parse(loadAndReplace(path, dummyDataMap))

  def loadAndReplace(path: String, replaceMap: Map[String, String]): String = {
    var jsonString = Source.fromURL(getClass.getResource(path)).mkString
    for ((key, value) <- replaceMap) {
      jsonString = jsonString.replace(key, value)
    }
    jsonString
  }

}
