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

package controllers.paye

import java.time.LocalDate

import controllers.ControllerBaseSpec
import controllers.auth.FakePayeAuthAction
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api.test.Helpers._
import play.api.test.Injecting
import services.GovernmentSpendService
import services.atsData.PayeAtsTestData.govSpendingData
import uk.gov.hmrc.time.CurrentTaxYear
import views.html.errors.{PayeGenericErrorView, PayeNotAuthorisedView, PayeServiceUnavailableView}

import scala.concurrent.Future

class PayeErrorControllerSpec
    extends PayeControllerSpecHelpers with ControllerBaseSpec with Injecting with CurrentTaxYear {

  override def now: () => LocalDate = () => LocalDate.now()

  implicit val fakeAuthenticatedRequest = buildPayeRequest("/annual-tax-summary/paye/treasury-spending")

  lazy val payeGenericErrorView: PayeGenericErrorView = inject[PayeGenericErrorView]

  val mockGovSpendService = mock[GovernmentSpendService]

  def sut =
    new PayeErrorController(
      mockGovSpendService,
      FakePayeAuthAction,
      mcc,
      payeGenericErrorView,
      howTaxIsSpentView,
      mock[PayeNotAuthorisedView],
      mock[PayeServiceUnavailableView])

  "PayeErrorController" should {

    "Show generic_error page with status INTERNAL_SERVER_ERROR when INTERNAL_SERVER_ERROR is received" in {

      val result = sut.genericError(INTERNAL_SERVER_ERROR)(fakeAuthenticatedRequest)
      val document = contentAsString(result)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      document shouldBe contentAsString(payeGenericErrorView())
    }

    "Show generic_error page with status BAD_GATEWAY when GATEWAY_TIMEOUT is received" in {

      val result = sut.genericError(GATEWAY_TIMEOUT)(fakeAuthenticatedRequest)
      val document = contentAsString(result)

      status(result) shouldBe BAD_GATEWAY
      document shouldBe contentAsString(payeGenericErrorView())
    }

    "Show generic_error page with status BAD_GATEWAY when BAD_GATEWAY is received" in {

      val result = sut.genericError(BAD_GATEWAY)(fakeAuthenticatedRequest)
      val document = contentAsString(result)

      status(result) shouldBe BAD_GATEWAY
      document shouldBe contentAsString(payeGenericErrorView())
    }

    "Show generic How Tax is Spent page and return OK" when {

      "the service returns Government Spend data" in {

        val spendCategory: String = "Environment"
        val spendPercentage: Double = 5.5
        val response: Seq[(String, Double)] = Seq((spendCategory, spendPercentage))

        when(mockGovSpendService.getGovernmentSpendDataV2(any(), any())(any(), any())) thenReturn Future
          .successful(response)

        val result = sut.authorisedNoAts(fakeAuthenticatedRequest)
        val document = contentAsString(result)

        status(result) shouldBe OK
        document shouldBe contentAsString(howTaxIsSpentView(response, govSpendingData.taxYear))
      }
    }
  }
}
