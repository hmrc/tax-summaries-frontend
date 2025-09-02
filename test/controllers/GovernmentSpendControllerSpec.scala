/*
 * Copyright 2025 HM Revenue & Customs
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
import models.SpendData
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{reset, when}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, SEE_OTHER}
import play.api.i18n.Messages
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services._
import utils.TestConstants._
import utils.{ControllerBaseSpec, TaxYearUtil}
import view_models._

import scala.concurrent.Future

class GovernmentSpendControllerSpec extends ControllerBaseSpec {
  private val taxYearUtil = app.injector.instanceOf[TaxYearUtil]

  val mockGovernmentSpendService: GovernmentSpendService = mock[GovernmentSpendService]
  val mockAuditService: AuditService                     = mock[AuditService]

  override def beforeEach(): Unit = {
    reset(mockFeatureFlagService)

    val model: GovernmentSpend = GovernmentSpend(
      taxYear = currentTaxYear,
      userUtr = testUtr,
      govSpendAmountData = List(
        ("welfare", SpendData(Amount(5863.22, "GBP"), 24.52)),
        ("health", SpendData(Amount(4512.19, "GBP"), 18.87)),
        ("education", SpendData(Amount(3144.43, "GBP"), 13.15)),
        ("pension", SpendData(Amount(2898.13, "GBP"), 12.12)),
        ("national_debt_interest", SpendData(Amount(1673.84, "GBP"), 7.00)),
        ("defence", SpendData(Amount(1269.73, "GBP"), 5.31)),
        ("criminal_justice", SpendData(Amount(1052.13, "GBP"), 4.40)),
        ("transport", SpendData(Amount(705.4, "GBP"), 2.95)),
        ("business_and_industry", SpendData(Amount(655.19, "GBP"), 2.74)),
        ("government_administration", SpendData(Amount(490.2, "GBP"), 2.05)),
        ("Culture", SpendData(Amount(404.11, "GBP"), 1.69)),
        ("HousingAndUtilities", SpendData(Amount(392.16, "GBP"), 1.64)),
        ("Environment", SpendData(Amount(396.94, "GBP"), 1.66)),
        ("overseas_aid", SpendData(Amount(274.99, "GBP"), 1.15)),
        ("uk_contribution_to_eu_budget", SpendData(Amount(179.34, "GBP"), 0.75))
      ),
      userTitle = "Mr",
      userForename = "userForename",
      userSurname = "userSurname",
      totalAmount = new Amount(23912.00, "GBP"),
      incomeTaxStatus = "0002",
      scottishIncomeTax = new Amount(2000.00, "GBP")
    )

    when(mockGovernmentSpendService.getGovernmentSpendData(meq(taxYear))(any(), meq(request), any()))
      .thenReturn(Future.successful(model))
    ()
  }

  def sut: GovernmentSpendController =
    new GovernmentSpendController(
      mockGovernmentSpendService,
      mockAuditService,
      FakeAuthJourney,
      mcc,
      governmentSpendingView,
      genericErrorView,
      tokenErrorView,
      taxYearUtil
    )

  "Calling government spend" must {

    "return a successful response for a valid request" in {
      val result   = sut.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.title must include(
        Messages("ats.treasury_spending.html.title") + Messages(
          "generic.to_from",
          (taxYear - 1).toString,
          taxYear.toString
        )
      )
    }

    "display an error page for an invalid request" in {
      val result = sut.show(badRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.ErrorController.authorisedNoTaxYear.url)
    }

    "display an error page when AtsUnavailableViewModel is returned" in {

      when(
        mockGovernmentSpendService.getGovernmentSpendData(meq(taxYear))(any(), meq(request), any())
      )
        .thenReturn(Future.successful(new ATSUnavailableViewModel))

      val result = sut.show(request)
      status(result) mustBe INTERNAL_SERVER_ERROR

      val document = Jsoup.parse(contentAsString(result))
      document.title must include(Messages("global.error.InternalServerError500.title"))
    }

    "redirect to the no ATS page when there is no Annual Tax Summary data returned" in {
      when(
        mockGovernmentSpendService.getGovernmentSpendData(meq(taxYear))(any(), meq(request), any())
      )
        .thenReturn(Future.successful(NoATSViewModel(appConfig.taxYearSA)))
      val result = sut.show(request)
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.ErrorController.authorisedNoAts(appConfig.taxYearSA).url
    }

    s"have correct data for $currentTaxYear" in {

      val result   = sut.show(request)
      val document = Jsoup.parse(contentAsString(result))
      document.select("#welfare + td").text() mustBe "£5,863.22"
      document.select("#welfare").text()                      must include("24.52%")
      document.select("#health + td").text() mustBe "£4,512.19"
      document.select("#health").text()                       must include("18.87%")
      document.select("#education + td").text() mustBe "£3,144.43"
      document.select("#education").text()                    must include("13.15%")
      document.select("#pension + td").text() mustBe "£2,898.13"
      document.select("#pension").text()                      must include("12.12%")
      document.select("#national_debt_interest + td").text() mustBe "£1,673.84"
      document.select("#national_debt_interest").text()       must include("7.0%")
      document.select("#defence + td").text() mustBe "£1,269.73"
      document.select("#defence").text()                      must include("5.31%")
      document.select("#criminal_justice + td").text() mustBe "£1,052.13"
      document.select("#criminal_justice").text()             must include("4.4%")
      document.select("#transport + td").text() mustBe "£705.40"
      document.select("#transport").text()                    must include("2.95%")
      document.select("#business_and_industry + td").text() mustBe "£655.19"
      document.select("#business_and_industry").text()        must include("2.74%")
      document.select("#government_administration + td").text() mustBe "£490.20"
      document.select("#government_administration").text()    must include("2.05%")
      document.select("#Culture + td").text() mustBe "£404.11"
      document.select("#Culture").text()                      must include("1.69%")
      document.select("#Environment + td").text() mustBe "£396.94"
      document.select("#Environment").text()                  must include("1.66%")
      document.select("#HousingAndUtilities + td").text() mustBe "£392.16"
      document.select("#HousingAndUtilities").text()          must include("1.64%")
      document.select("#overseas_aid + td").text() mustBe "£274.99"
      document.select("#overseas_aid").text()                 must include("1.15%")
      document.select("#uk_contribution_to_eu_budget + td").text() mustBe "£179.34"
      document.select("#uk_contribution_to_eu_budget").text() must include("0.75%")

      document.getElementById("user-info").text() must include("userForename userSurname")
      document.getElementById("user-info").text() must include("Unique Taxpayer Reference: " + testUtr)
      document.select("#gov-spend-total + td").text() mustBe "£23,912.00"
      document
        .select("header[data-component='ats_page_heading']")
        .text mustBe s"Tax year: April 6 $previousTaxYear to April 5 $currentTaxYear Your taxes and public spending"
    }

    s"have correct data for $previousTaxYear" in {

      val model2 = GovernmentSpend(
        taxYear = previousTaxYear,
        userUtr = testUtr,
        govSpendAmountData = List(
          ("welfare", SpendData(Amount(2530, "GBP"), 25.3)),
          ("health", SpendData(Amount(1990, "GBP"), 19.9)),
          ("pension", SpendData(Amount(1280, "GBP"), 12.8)),
          ("education", SpendData(Amount(1250, "GBP"), 12.5)),
          ("defence", SpendData(Amount(540, "GBP"), 5.4)),
          ("national_debt_interest", SpendData(Amount(500, "GBP"), 5.0)),
          ("public_order_and_safety", SpendData(Amount(440, "GBP"), 4.4)),
          ("transport", SpendData(Amount(300, "GBP"), 3.0)),
          ("business_and_industry", SpendData(Amount(270, "GBP"), 2.7)),
          ("government_administration", SpendData(Amount(200, "GBP"), 2.0)),
          ("Culture", SpendData(Amount(180, "GBP"), 1.8)),
          ("Environment", SpendData(Amount(170, "GBP"), 1.7)),
          ("HousingAndUtilities", SpendData(Amount(160, "GBP"), 1.6)),
          ("overseas_aid", SpendData(Amount(130, "GBP"), 1.3)),
          ("uk_contribution_to_eu_budget", SpendData(Amount(600, "GBP"), 0.6))
        ),
        userTitle = "Mr",
        userForename = "userForename",
        userSurname = "userSurname",
        totalAmount = new Amount(10000.0, "GBP"),
        incomeTaxStatus = "0002",
        scottishIncomeTax = new Amount(2000.00, "GBP")
      )

      when(
        mockGovernmentSpendService.getGovernmentSpendData(meq(taxYear))(any(), meq(request), any())
      )
        .thenReturn(Future.successful(model2))

      val result   = sut.show(request)
      val document = Jsoup.parse(contentAsString(result))

      document.select("#welfare + td").text() mustBe "£2,530.00"
      document.select("#welfare").text()                      must include("25.3%")
      document.select("#health + td").text() mustBe "£1,990.00"
      document.select("#health").text()                       must include("19.9%")
      document.select("#pension + td").text() mustBe "£1,280.00"
      document.select("#pension").text()                      must include("12.8%")
      document.select("#education + td").text() mustBe "£1,250.00"
      document.select("#education").text()                    must include("12.5%")
      document.select("#defence + td").text() mustBe "£540.00"
      document.select("#defence").text()                      must include("5.4%")
      document.select("#national_debt_interest + td").text() mustBe "£500.00"
      document.select("#national_debt_interest").text()       must include("5.0%")
      document.select("#public_order_and_safety + td").text() mustBe "£440.00"
      document.select("#public_order_and_safety").text()      must include("4.4%")
      document.select("#transport + td").text() mustBe "£300.00"
      document.select("#transport").text()                    must include("3.0%")
      document.select("#business_and_industry + td").text() mustBe "£270.00"
      document.select("#business_and_industry").text()        must include("2.7%")
      document.select("#government_administration + td").text() mustBe "£200.00"
      document.select("#government_administration").text()    must include("2.0%")
      document.select("#Culture + td").text() mustBe "£180.00"
      document.select("#Culture").text()                      must include("1.8%")
      document.select("#Environment + td").text() mustBe "£170.00"
      document.select("#Environment").text()                  must include("1.7%")
      document.select("#HousingAndUtilities + td").text() mustBe "£160.00"
      document.select("#HousingAndUtilities").text()          must include("1.6%")
      document.select("#overseas_aid + td").text() mustBe "£130.00"
      document.select("#overseas_aid").text()                 must include("1.3%")
      document.select("#uk_contribution_to_eu_budget + td").text() mustBe "£600.00"
      document.select("#uk_contribution_to_eu_budget").text() must include("0.6%")

      document.getElementById("user-info").text() must include("userForename userSurname")
      document.getElementById("user-info").text() must include("Unique Taxpayer Reference: " + testUtr)
      document.select("#gov-spend-total + td").text() mustBe "£10,000.00"
    }
  }
}
