/*
 * Copyright 2019 HM Revenue & Customs
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

import config.AppFormPartialRetriever
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, _}
import services._
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import utils.AuthorityUtils
import utils.TestConstants._
import view_models.NoATSViewModel
import scala.concurrent.Future

class ZeroTaxLiabilityTest extends UnitSpec with FakeTaxsPlayApplication with MockitoSugar {

  val request = FakeRequest("GET","?taxYear=2015")
  val user = User(AuthorityUtils.saAuthority(testOid, testUtr))
  val dataPath = "/no_ats_json_test.json"
  val model = new NoATSViewModel
  val taxYear = 2015

  "Opening link if user has no income tax or cg tax liability" should {

    "show no ats page for total-income-tax" in new TotalIncomeTaxController {

      override lazy val totalIncomeTaxService = mock[TotalIncomeTaxService]
      override lazy val auditService = mock[AuditService]
      implicit val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever

      when(totalIncomeTaxService.getIncomeData(Matchers.eq(taxYear))(Matchers.eq(user),Matchers.any(),Matchers.eq(request))).thenReturn(Future.successful(model))

      val result = Future.successful(show(user, request))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/annual-tax-summary/no-ats")
    }
  }

  "show have the correct title for the no ATS page" in new IncomeController {

    override lazy val incomeService = mock[IncomeService]
    override lazy val auditService = mock[AuditService]
    implicit val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever

    when(incomeService.getIncomeData(Matchers.eq(taxYear))(Matchers.eq(user),Matchers.any(),Matchers.eq(request))).thenReturn(Future.successful(model))

    val result = Future.successful(show(user, request))

    status(result) shouldBe 303
    redirectLocation(result) shouldBe Some("/annual-tax-summary/no-ats")
  }

  "show no ats page for income-before-tax" in new IncomeController {

    override lazy val incomeService = mock[IncomeService]
    override lazy val auditService = mock[AuditService]
    implicit val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever

    when(incomeService.getIncomeData(Matchers.eq(taxYear))(Matchers.eq(user),Matchers.any(),Matchers.eq(request))).thenReturn(Future.successful(model))

    val result = Future.successful(show(user, request))

    status(result) shouldBe 303
    redirectLocation(result) shouldBe Some("/annual-tax-summary/no-ats")
  }

  "show no ats page for tax-free-amount" in new AllowancesController {

    override lazy val allowanceService = mock[AllowanceService]
    override lazy val auditService = mock[AuditService]
    implicit val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever

    when(allowanceService.getAllowances(Matchers.eq(taxYear))(Matchers.eq(user),Matchers.eq(request),Matchers.any())).thenReturn(Future.successful(model))

    val result = Future.successful(show(user, request))

    status(result) shouldBe 303
    redirectLocation(result) shouldBe Some("/annual-tax-summary/no-ats")
  }

  "show no ats page for capital-gains-tax" in new CapitalGainsTaxController {

    override lazy val capitalGainsService = mock[CapitalGainsService]
    override lazy val auditService = mock[AuditService]
    implicit val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever

    when(capitalGainsService.getCapitalGains(Matchers.eq(taxYear))(Matchers.eq(user),Matchers.any(),Matchers.eq(request))).thenReturn(Future.successful(model))

    val result = Future.successful(show(user, request))

    status(result) shouldBe 303
    redirectLocation(result) shouldBe Some("/annual-tax-summary/no-ats")
  }

  "show no ats page for government spend" in new GovernmentSpendController {

    override lazy val governmentSpendService = mock[GovernmentSpendService]
    override lazy val auditService = mock[AuditService]
    implicit val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever

    when(governmentSpendService.getGovernmentSpendData(Matchers.eq(taxYear))(Matchers.eq(user),Matchers.any(),Matchers.eq(request))).thenReturn(Future.successful(model))

    val result = Future.successful(show(user, request))

    status(result) shouldBe 303
    redirectLocation(result) shouldBe Some("/annual-tax-summary/no-ats")
  }

  "show no ats page for summary page" in new SummaryController {

    override lazy val summaryService = mock[SummaryService]
    override lazy val auditService = mock[AuditService]
    implicit val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever

    when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.eq(user),Matchers.any(),Matchers.eq(request))).thenReturn(Future.successful(model))

    val result = Future.successful(show(user, request))

    status(result) shouldBe 303
    redirectLocation(result) shouldBe Some("/annual-tax-summary/no-ats")
  }

  "show no ats page for nics summary page" in new NicsController {

    override lazy val summaryService = mock[SummaryService]
    override lazy val auditService = mock[AuditService]
    implicit val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever

    when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.eq(user),Matchers.any(),Matchers.eq(request))).thenReturn(Future.successful(model))

    val result = Future.successful(show(user, request))

    status(result) shouldBe 303
    redirectLocation(result) shouldBe Some("/annual-tax-summary/no-ats")
  }
}
