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

import config.AppFormPartialRetriever
import controllers.auth.{AuthAction, AuthenticatedRequest, FakeAuthAction}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import services._
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import utils.GenericViewModel
import view_models.{AtsList, TaxYearEnd}
import utils.TestConstants._

import scala.concurrent.Future

class InvalidDataControllerSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar {

  val request = AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest("GET","?taxYear=2015"))
  val dataPath = "/json_containing_errors_test.json"
  val dataPathNoAts = "/no_ats_json_test.json"
  val taxYear = 2014
  implicit val hc = new HeaderCarrier

  val genericViewModel: GenericViewModel =  AtsList(
    utr = "3000024376",
    forename = "forename",
    surname = "surname",
    yearList = List(
      TaxYearEnd(Some("2015"))
    )
  )

  "Calling a service with a JSON containing errors" should {

    "show ats error page for allowances" in new AllowancesController {

      override lazy val allowanceService = mock[AllowanceService]
      override lazy val auditService = mock[AuditService]
      implicit lazy val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever
      override val authAction: AuthAction = FakeAuthAction

      when(allowanceService.getAllowances(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.failed(new Exception("failed")))

      val result = show(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe 200
      document.toString should include("Sorry, there is a problem with the service")
    }

    "show ats error page for capital-gains" in new CapitalGainsTaxController {

      override lazy val capitalGainsService = mock[CapitalGainsService]
      override lazy val auditService = mock[AuditService]
      implicit lazy val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever
      override val authAction: AuthAction = FakeAuthAction

      when(capitalGainsService.getCapitalGains(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.failed(new Exception("failure")))

      val result = show(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe 200
      document.toString should include("Sorry, there is a problem with the service")
    }

    "show ats error page for government-spend" in new GovernmentSpendController {

      override lazy val governmentSpendService = mock[GovernmentSpendService]
      override lazy val auditService = mock[AuditService]
      implicit lazy val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever
      override val authAction: AuthAction = FakeAuthAction

      when(governmentSpendService.getGovernmentSpendData(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.failed(new Exception("failure")))

      val result = show(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe 200
      document.toString should include("Sorry, there is a problem with the service")
    }

    "show ats error page for income" in new IncomeController {

      override lazy val incomeService = mock[IncomeService]
      override lazy val auditService = mock[AuditService]
      implicit lazy val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever
      override val authAction: AuthAction = FakeAuthAction

      when(incomeService.getIncomeData(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.failed(new Exception("failure")))

      val result = show(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe 200
      document.toString should include("Sorry, there is a problem with the service")
    }

    "show ats error page for total-income-tax" in new TotalIncomeTaxController {

      override lazy val totalIncomeTaxService = mock[TotalIncomeTaxService]
      override lazy val auditService = mock[AuditService]
      implicit lazy val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever
      override val authAction: AuthAction = FakeAuthAction

      when(totalIncomeTaxService.getIncomeData(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.failed(new Exception("failure")))

      val result = show(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe 200
      document.toString should include("Sorry, there is a problem with the service")
    }

    "show ats error page for summary page" in new SummaryController {

      override lazy val summaryService = mock[SummaryService]
      override lazy val auditService = mock[AuditService]
      implicit lazy val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever
      override val authAction: AuthAction = FakeAuthAction

      when(summaryService.getSummaryData(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.failed(new Exception("failure")))

      val result = show(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe 200
      document.toString should include("Sorry, there is a problem with the service")
    }

    "show ats error page for nics on summary page" in new NicsController {

      override lazy val summaryService = mock[SummaryService]
      override lazy val auditService = mock[AuditService]
      implicit lazy val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever
      override val authAction: AuthAction = FakeAuthAction

      when(summaryService.getSummaryData(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.failed(new Exception("failure")))

      val result = show(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe 200

      document.toString should include("Sorry, there is a problem with the service")
    }
  }
}
