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
import org.mockito.ArgumentMatchers.{any, eq => meq}
import play.api.test.Helpers._
import services._
import utils.ControllerBaseSpec
import view_models.NoATSViewModel

import scala.concurrent.Future

class ZeroTaxLiabilitySpec extends ControllerBaseSpec {

  val dataPath              = "/no_ats_json_test.json"
  val model: NoATSViewModel = NoATSViewModel(taxYear)

  val mockIncomeService: IncomeService   = mock[IncomeService]
  val mockAuditService: AuditService     = mock[AuditService]
  val mockSummaryService: SummaryService = mock[SummaryService]

  def incomeController: IncomeController =
    new IncomeController(
      mockIncomeService,
      mockAuditService,
      FakeAuthJourney,
      mcc,
      incomeBeforeTaxView,
      genericErrorView,
      tokenErrorView
    )

  override def beforeEach(): Unit =
    when(mockIncomeService.getIncomeData(meq(taxYear))(any(), meq(request)))
      .thenReturn(Future.successful(model))

  "Opening link if user has no income tax or cg tax liability" must {

    "show no ats page for total-income-tax" in {

      val mockTotalIncomeTaxService = mock[TotalIncomeTaxService]

      def sut: TotalIncomeTaxController =
        new TotalIncomeTaxController(
          mockTotalIncomeTaxService,
          mockAuditService,
          FakeAuthJourney,
          mcc,
          totalIncomeTaxView,
          genericErrorView,
          tokenErrorView
        )
      when(mockTotalIncomeTaxService.getIncomeData(any())(any(), any()))
        .thenReturn(Future.successful(model))

      val result = sut.show(request)

      status(result) mustBe 303
      redirectLocation(result) mustBe Some(s"/annual-tax-summary/no-ats?taxYear=$taxYear")
    }
  }

  "show have the correct title for the no ATS page" in {

    val result = incomeController.show(request)

    status(result) mustBe 303
    redirectLocation(result) mustBe Some(s"/annual-tax-summary/no-ats?taxYear=$taxYear")
  }

  "show no ats page for income-before-tax" in {

    val result = incomeController.show(request)

    status(result) mustBe 303
    redirectLocation(result) mustBe Some(s"/annual-tax-summary/no-ats?taxYear=$taxYear")
  }

  "show no ats page for tax-free-amount" in {

    val allowanceService = mock[AllowanceService]

    def sut: AllowancesController =
      new AllowancesController(
        allowanceService,
        mockAuditService,
        FakeAuthJourney,
        mcc,
        taxFreeAmountView,
        genericErrorView,
        tokenErrorView
      )

    when(allowanceService.getAllowances(any())(any(), any()))
      .thenReturn(Future.successful(model))

    val result = sut.show(request)

    status(result) mustBe 303
    redirectLocation(result) mustBe Some(s"/annual-tax-summary/no-ats?taxYear=$taxYear")
  }

  "show no ats page for capital-gains-tax" in {

    val mockCapitalGainsService = mock[CapitalGainsService]

    def sut: CapitalGainsTaxController =
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
      .thenReturn(Future.successful(model))

    val result = sut.show(request)

    status(result) mustBe 303
    redirectLocation(result) mustBe Some(s"/annual-tax-summary/no-ats?taxYear=$taxYear")
  }

  "show no ats page for government spend" in {

    val mockGovernmentSpendService = mock[GovernmentSpendService]

    def sut: GovernmentSpendController =
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
      .thenReturn(Future.successful(model))

    val result = sut.show(request)

    status(result) mustBe 303
    redirectLocation(result) mustBe Some(s"/annual-tax-summary/no-ats?taxYear=$taxYear")
  }

  "show no ats page for summary page" in {

    def sut: SummaryController =
      new SummaryController(
        mockSummaryService,
        mockAuditService,
        FakeAuthJourney,
        mcc,
        summaryView,
        genericErrorView,
        tokenErrorView
      )

    when(mockSummaryService.getSummaryData(any())(any(), any()))
      .thenReturn(Future.successful(model))

    val result = sut.show(request)

    status(result) mustBe 303
    redirectLocation(result) mustBe Some(s"/annual-tax-summary/no-ats?taxYear=$taxYear")
  }

  "show no ats page for nics summary page" in {

    def sut: NicsController =
      new NicsController(
        mockSummaryService,
        mockAuditService,
        FakeAuthJourney,
        mcc,
        nicsView,
        genericErrorView,
        tokenErrorView
      )

    when(mockSummaryService.getSummaryData(any())(any(), any()))
      .thenReturn(Future.successful(model))

    val result = sut.show(request)

    status(result) mustBe 303
    redirectLocation(result) mustBe Some(s"/annual-tax-summary/no-ats?taxYear=$taxYear")
  }
}
