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

import controllers.auth.requests
import controllers.auth.requests.PayeAuthenticatedRequest
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestConstants.testNino
import utils.{ControllerBaseSpec, JsonUtil}

trait PayeControllerSpecHelpers extends ControllerBaseSpec with JsonUtil {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  protected val apiResponseGovSpendPreviousTaxYear: JsValue =
    Json.parse(
      payAtsData(currentTaxYearPAYE - 1)
    )

  protected val apiResponseGovSpendCurrentTaxYear: JsValue =
    Json.parse(
      payAtsData(currentTaxYearPAYE)
    )

  protected def buildPayeRequest(endpoint: String): PayeAuthenticatedRequest[AnyContentAsEmpty.type] =
    requests.PayeAuthenticatedRequest(testNino, fakeCredentials, FakeRequest("GET", endpoint))
}
