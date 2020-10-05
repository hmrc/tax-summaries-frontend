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

package controllers

import controllers.auth.{AuthenticatedRequest, FakeAuthAction}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, _}
import services._
import uk.gov.hmrc.domain.SaUtr
import utils.TestConstants._
import view_models.NoATSViewModel
import scala.concurrent.Future

class ZeroTaxLiabilitySpec extends ControllerBaseSpec with BeforeAndAfterEach {

  val taxYear = 2015
  val request = AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest("GET", s"?taxYear=$taxYear"))
  val dataPath = "/no_ats_json_test.json"
  val model = new NoATSViewModel

  val mockIncomeService = mock[IncomeService]
  val mockAuditService = mock[AuditService]
  val mockSummaryService = mock[SummaryService]

  def incomeController = new IncomeController(mockIncomeService, mockAuditService, FakeAuthAction, mcc, incomeBeforeTaxView, genericErrorView, tokenErrorView)

  override def beforeEach() = {
    when(mockIncomeService.getIncomeData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model))
  }

  "Opening link if user has no income tax or cg tax liability" should {

    "show no ats page for total-income-tax" in {

      val mockTotalIncomeTaxService = mock[TotalIncomeTaxService]

      def sut = new TotalIncomeTaxController(mockTotalIncomeTaxService, mockAuditService, FakeAuthAction, mcc, totalIncomeTaxView, genericErrorView, tokenErrorView)
      when(mockTotalIncomeTaxService.getIncomeData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model))

      val result = Future.successful(sut.show(request))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/annual-tax-summary/no-ats")
    }
  }

  "show have the correct title for the no ATS page" in {

    val result = Future.successful(incomeController.show(request))

    status(result) shouldBe 303
    redirectLocation(result) shouldBe Some("/annual-tax-summary/no-ats")
  }

  "show no ats page for income-before-tax" in {

    val result = Future.successful(incomeController.show(request))

    status(result) shouldBe 303
    redirectLocation(result) shouldBe Some("/annual-tax-summary/no-ats")
  }

  "show no ats page for tax-free-amount" in {

    val allowanceService = mock[AllowanceService]

    def sut = new AllowancesController(allowanceService, mockAuditService, FakeAuthAction, mcc,taxFreeAmountView, genericErrorView, tokenErrorView)

    when(allowanceService.getAllowances(Matchers.eq(taxYear))(Matchers.eq(request), Matchers.any())).thenReturn(Future.successful(model))

    val result = Future.successful(sut.show(request))

    status(result) shouldBe 303
    redirectLocation(result) shouldBe Some("/annual-tax-summary/no-ats")
  }

  "show no ats page for capital-gains-tax" in {

    val mockCapitalGainsService = mock[CapitalGainsService]

    def sut = new CapitalGainsTaxController(mockCapitalGainsService, mockAuditService, FakeAuthAction, mcc, capitalGainsView, genericErrorView, tokenErrorView)

    when(mockCapitalGainsService.getCapitalGains(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model))

    val result = Future.successful(sut.show(request))

    status(result) shouldBe 303
    redirectLocation(result) shouldBe Some("/annual-tax-summary/no-ats")
  }

  "show no ats page for government spend" in {

    val mockGovernmentSpendService = mock[GovernmentSpendService]

    def sut = new GovernmentSpendController(mockGovernmentSpendService, mockAuditService, FakeAuthAction, mcc, governmentSpendingView, genericErrorView, tokenErrorView)

    when(mockGovernmentSpendService.getGovernmentSpendData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model))

    val result = Future.successful(sut.show(request))

    status(result) shouldBe 303
    redirectLocation(result) shouldBe Some("/annual-tax-summary/no-ats")
  }

  "show no ats page for summary page" in {

    def sut = new SummaryController(mockSummaryService, mockAuditService, FakeAuthAction, mcc, summaryView, genericErrorView, tokenErrorView)

    when(mockSummaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model))

    val result = Future.successful(sut.show(request))

    status(result) shouldBe 303
    redirectLocation(result) shouldBe Some("/annual-tax-summary/no-ats")
  }

  "show no ats page for nics summary page" in {

    def sut = new NicsController(mockSummaryService, mockAuditService, FakeAuthAction, mcc, nicsView, genericErrorView, tokenErrorView)

    when(mockSummaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request))).thenReturn(Future.successful(model))

    val result = Future.successful(sut.show(request))

    status(result) shouldBe 303
    redirectLocation(result) shouldBe Some("/annual-tax-summary/no-ats")
  }
}
