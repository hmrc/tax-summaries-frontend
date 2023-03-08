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

package controllers.paye

import controllers.auth.PayeAuthenticatedRequest
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import utils.ControllerBaseSpec
import utils.TestConstants.testNino

import scala.io.Source

trait PayeControllerSpecHelpers extends ControllerBaseSpec {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val expectedResponse2020: JsValue = readJson("/paye_ats_2020.json")

  val expectedResponse2021: JsValue = readJson("/paye_ats_2021.json")

  def buildPayeRequest(endpoint: String) =
    PayeAuthenticatedRequest(testNino, false, fakeCredentials, FakeRequest("GET", endpoint), None)

  def readJson(path: String): JsValue = {
    val resource = getClass.getResourceAsStream(path)
    Json.parse(Source.fromInputStream(resource).getLines().mkString)
  }
}
