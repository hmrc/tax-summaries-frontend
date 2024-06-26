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

package controllers.testOnly

import connectors.MiddleConnector
import org.mockito.ArgumentMatchers.any
import play.api.libs.json.Json
import play.api.test.Helpers._
import utils.ControllerBaseSpec
import views.html.testOnly.DisplayPTAView

import scala.concurrent.Future

class DisplayPTAControllerSpec extends ControllerBaseSpec {
  private val view                = inject[DisplayPTAView]
  private val mockMiddleConnector = mock[MiddleConnector]

  private def controller = new DisplayPTAController(
    mcc,
    view,
    mockMiddleConnector
  )

  private val utr = "00000000010"

  private val jsValue = Json.obj()

  override def beforeEach(): Unit = {
    reset(mockMiddleConnector)
    when(mockMiddleConnector.connectToAtsSaDataPlusCalculus(any(), any())(any())).thenReturn(
      Future.successful(Right(jsValue))
    )
  }

  "onPageLoad" must {
    "render the page" in {
      val result = controller.onPageLoad(taxYear, utr)(request)
      status(result) mustBe OK
    }
  }
}
