/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers

import controllers.auth.FakeAuthJourney
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import play.api.i18n.Messages
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import services._
import uk.gov.hmrc.http.HeaderCarrier
import utils.ControllerBaseSpec

import scala.concurrent.Future

class InvalidDataControllerSpec extends ControllerBaseSpec {

  val dataPath                          = "/json_containing_errors_test.json"
  val dataPathNoAts                     = "/no_ats_json_test.json"
  override val taxYear                  = 2023
  private val mockTotalIncomeTaxService = mock[IncomeTaxAndNIService]
  implicit val hc: HeaderCarrier        = new HeaderCarrier

  "Calling a service with a JSON containing errors" must {

    "show ats error page for allowances" in {
      val mockAllowanceService: AllowanceService = mock[AllowanceService]
      val mockAuditService: AuditService         = mock[AuditService]

      def sut =
        new AllowancesController(
          mockAllowanceService,
          mockAuditService,
          FakeAuthJourney,
          mcc,
          taxFreeAmountView,
          genericErrorView,
          tokenErrorView
        )

      when(mockAllowanceService.getAllowances(any())(any(), any()))
        .thenReturn(Future.failed(new Exception("failed")))

      val result   = sut.show(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe 500
      document.toString must include(Messages("global.error.InternalServerError500.title"))
    }

    "show ats error page for capital-gains" in {

      val mockCapitalGainsService: CapitalGainsService = mock[CapitalGainsService]
      val mockAuditService: AuditService               = mock[AuditService]

      def sut =
        new CapitalGainsTaxController(
          mockCapitalGainsService,
          mockAuditService,
          FakeAuthJourney,
          mcc,
          capitalGainsView,
          genericErrorView,
          tokenErrorView
        )

      when(mockCapitalGainsService.getCapitalGains(any())(any(), any()))
        .thenReturn(Future.failed(new Exception("failure")))

      val result   = sut.show(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe 500
      document.toString must include(Messages("global.error.InternalServerError500.title"))
    }

    "show ats error page for government-spend" in {

      val mockGovernmentSpendService: GovernmentSpendService = mock[GovernmentSpendService]
      val mockAuditService: AuditService                     = mock[AuditService]

      def sut =
        new GovernmentSpendController(
          mockGovernmentSpendService,
          mockAuditService,
          FakeAuthJourney,
          mcc,
          governmentSpendingView,
          genericErrorView,
          tokenErrorView
        )

      when(mockGovernmentSpendService.getGovernmentSpendData(any())(any(), any(), any()))
        .thenReturn(Future.failed(new Exception("failure")))

      val result   = sut.show(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe 500
      document.toString must include(Messages("global.error.InternalServerError500.title"))
    }

    "show ats error page for income" in {

      val mockIncomeService = mock[IncomeService]
      val mockAuditService  = mock[AuditService]

      def sut =
        new IncomeController(
          mockIncomeService,
          mockAuditService,
          FakeAuthJourney,
          mcc,
          incomeBeforeTaxView,
          genericErrorView,
          tokenErrorView
        )

      when(mockIncomeService.getIncomeData(any())(any(), any()))
        .thenReturn(Future.failed(new Exception("failure")))

      val result   = sut.show(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe 500
      document.toString must include(Messages("global.error.InternalServerError500.title"))
    }

    "show ats error page for summary page" in {

      val mockSummaryService = mock[SummaryService]
      val mockAuditService   = mock[AuditService]

      val sut = new SummaryController(
        mockSummaryService,
        mockAuditService,
        FakeAuthJourney,
        mcc,
        summaryView,
        genericErrorView,
        tokenErrorView
      )

      when(mockSummaryService.getSummaryData(any())(any(), any()))
        .thenReturn(Future.failed(new Exception("failure")))

      val result   = sut.show(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe 500
      document.toString must include(Messages("global.error.InternalServerError500.title"))
    }

    "show ats error page for nics on summary page" in {
      when(mockTotalIncomeTaxService.getIncomeAndNIData(any())(any(), any()))
        .thenReturn(Future.successful(totalIncomeTaxModel))
      val mockAuditService = mock[AuditService]

      def sut =
        new NicsController(
          mockAuditService,
          FakeAuthJourney,
          mcc,
          nicsView,
          genericErrorView,
          tokenErrorView,
          mockTotalIncomeTaxService
        )

      when(mockTotalIncomeTaxService.getIncomeAndNIData(any())(any(), any()))
        .thenReturn(Future.failed(new Exception("failure")))

      val result   = sut.show(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe 500

      document.toString must include(Messages("global.error.InternalServerError500.title"))
    }
  }
}
