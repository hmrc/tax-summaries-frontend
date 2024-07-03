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

package testOnly.controllers

import org.mockito.ArgumentMatchers.any
import play.api.libs.json.Json
import play.api.test.Helpers._
import testOnly.connectors.TaxSummariesConnector
import testOnly.views.html.DisplayPTAView
import utils.ControllerBaseSpec

import scala.concurrent.Future

class DisplayPTAControllerSpec extends ControllerBaseSpec {
  private val view                      = inject[DisplayPTAView]
  private val mockTaxSummariesConnector = mock[TaxSummariesConnector]

  private def controller = new DisplayPTAController(
    mcc,
    view,
    mockTaxSummariesConnector
  )

  private val utr = "00000000010"

  private val jsValue = Json.parse("""{
                          |   "taxYear":2023,
                          |   "utr":"0000000010",
                          |   "odsValues":{
                          |      "income_tax":[
                          |         {
                          |            "fieldName":"Field1",
                          |            "amount":1.00,
                          |            "calculus":"calculusField1"
                          |         }
                          |      ],
                          |      "summary_data":[
                          |         {
                          |            "fieldName":"Field2",
                          |            "amount":2.00,
                          |            "calculus":"calculusField2"
                          |         }
                          |      ],
                          |      "income_data":[
                          |         {
                          |            "fieldName":"Field3",
                          |            "amount":3.00,
                          |            "calculus":"calculusField3"
                          |         }
                          |      ],
                          |      "allowance_data":[
                          |         {
                          |            "fieldName":"Field4",
                          |            "amount":4.00,
                          |            "calculus":"calculusField4"
                          |         }
                          |      ],
                          |      "capital_gains_data":[
                          |         {
                          |            "fieldName":"Field5",
                          |            "amount":5.00,
                          |            "calculus":"calculusField5"
                          |         }
                          |      ],
                          |      "tax_liability": {
                          |         "amount":6.00,
                          |         "calculus": "calculusAll"
                          |      }
                          |   }
                          |}""".stripMargin)

  override def beforeEach(): Unit = {
    reset(mockTaxSummariesConnector)
    when(mockTaxSummariesConnector.connectToAtsSaDataPlusCalculus(any(), any())(any())).thenReturn(
      Future.successful(Right(jsValue))
    )
  }

  "onPageLoad" must {
    "render the page" in {
      val result                                                        = controller.onPageLoad(taxYear, utr)(request)
      status(result) mustBe OK
      val document                                                      = contentAsString(result)
      val expSections: Seq[(String, Seq[(String, BigDecimal, String)])] = Seq(
        ("Income tax data", Seq(("Field1", BigDecimal(1.00).setScale(2), "calculusField1"))),
        ("Summary data", Seq(("Field2", BigDecimal(2.00).setScale(2), "calculusField2"))),
        ("Income data", Seq(("Field3", BigDecimal(3.00).setScale(2), "calculusField3"))),
        ("Allowance data", Seq(("Field4", BigDecimal(4.00).setScale(2), "calculusField4"))),
        ("Capital gains data", Seq(("Field5", BigDecimal(5.00).setScale(2), "calculusField5")))
      )
      val expTaxLiability: Option[(String, BigDecimal, String)]         =
        Some(Tuple3("", BigDecimal(6.00).setScale(2), "calculusAll"))

      document mustBe contentAsString(view(expSections, expTaxLiability)(request, implicitly))
    }
  }
}
