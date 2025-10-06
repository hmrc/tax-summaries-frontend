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

package utils

import play.api.libs.json.*
import utils.TestConstants.testUtr

import scala.io.Source

object JsonUtil extends JsonUtil

trait JsonUtil {
  lazy val dummyDataMap: Map[String, String] = Map("testUtr" -> testUtr)

  def loadAndParseJsonWithDummyData(path: String): JsValue = Json.parse(loadAndReplace(path, dummyDataMap))

  def loadAndReplace(path: String, replaceMap: Map[String, String] = Map.empty): String = {
    val resource = getClass.getResourceAsStream(path)
    replaceMap.foldLeft(Source.fromInputStream(resource).getLines().mkString) { (acc, c) =>
      acc.replace(c._1, c._2)
    }
  }

}
