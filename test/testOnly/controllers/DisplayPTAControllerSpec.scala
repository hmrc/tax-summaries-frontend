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

import connectors.MiddleConnector
import models.{AtsData, AtsSuccessResponseWithPayload, DataHolder}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers._
import testOnly.views.html.DisplayPTAView
import uk.gov.hmrc.http.HeaderCarrier
import utils.ControllerBaseSpec
import view_models.Amount

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

  private val connectorResponse: AtsData = {
    def fieldInSection(fieldName: String, amount: BigDecimal, calculus: String): Option[DataHolder] =
      Some(
        DataHolder(
          payload = Some(
            Map(
              fieldName -> Amount(amount, "GBP", Some(calculus))
            )
          ),
          rates = None,
          incomeTaxStatus = None
        )
      )

    AtsData(
      taxYear = taxYear,
      utr = Some(utr),
      income_tax = fieldInSection("Field1", BigDecimal(1).setScale(2), "calculusField1"),
      summary_data = fieldInSection("Field2", BigDecimal(2).setScale(2), "calculusField2"),
      income_data = fieldInSection("Field3", BigDecimal(3).setScale(2), "calculusField3"),
      allowance_data = fieldInSection("Field4", BigDecimal(4).setScale(2), "calculusField4"),
      capital_gains_data = fieldInSection("Field5", BigDecimal(5).setScale(2), "calculusField5"),
      gov_spending = None,
      taxPayerData = None,
      errors = None,
      taxLiability = Some(Amount(BigDecimal(6).setScale(2), "GBP", None))
    )
  }

  override def beforeEach(): Unit = {
    reset(mockMiddleConnector)
    when(mockMiddleConnector.connectToAts(any(), any())(any())).thenReturn(
      Future.successful(AtsSuccessResponseWithPayload(connectorResponse))
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
      val expTaxLiability: Option[BigDecimal]                           =
        Some(BigDecimal(6.00).setScale(2))

      document mustBe contentAsString(view(expSections, expTaxLiability)(request, implicitly))
      val captor: ArgumentCaptor[HeaderCarrier] = ArgumentCaptor.forClass(classOf[HeaderCarrier])
      verify(mockMiddleConnector, times(1)).connectToAts(any(), any())(captor.capture())
      val actHC                                 = captor.getValue
      actHC.extraHeaders.contains("ignoreSAODSCache" -> "true") mustBe true
    }
  }
}
