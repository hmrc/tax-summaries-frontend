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
import models.SpendData
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.MustMatchers._
import play.api.http.Status.SEE_OTHER
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation}
import services.{AuditService, _}
import uk.gov.hmrc.domain.SaUtr
import utils.GenericViewModel
import utils.TestConstants._
import view_models._
import scala.concurrent.Future

class GovernmentSpendControllerSpec extends ControllerBaseSpec with BeforeAndAfterEach {

  override val taxYear = 2014
  val request = AuthenticatedRequest(
    "userId",
    None,
    Some(SaUtr(testUtr)),
    None,
    None,
    None,
    None,
    FakeRequest("GET", s"?taxYear=$taxYear"))
  val badRequest = AuthenticatedRequest(
    "userId",
    None,
    Some(SaUtr(testUtr)),
    None,
    None,
    None,
    None,
    FakeRequest("GET", "?taxYear=20145"))

  val mockGovernmentSpendService = mock[GovernmentSpendService]
  val mockAuditService = mock[AuditService]

  def sut =
    new GovernmentSpendController(
      mockGovernmentSpendService,
      mockAuditService,
      FakeAuthAction,
      mcc,
      governmentSpendingView,
      genericErrorView,
      tokenErrorView)

  val genericViewModel: GenericViewModel = AtsList(
    utr = "3000024376",
    forename = "forename",
    surname = "surname",
    yearList = List(
      TaxYearEnd(Some("2015"))
    )
  )

  val model = new GovernmentSpend(
    taxYear = 2014,
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

  override def beforeEach() =
    when(mockGovernmentSpendService.getGovernmentSpendData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
      .thenReturn(Future.successful(model))

  "Calling government spend" should {

    "return a successful response for a valid request" in {
      val result = Future.successful(sut.show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.title should include(
        Messages("ats.treasury_spending.html.title") + Messages(
          "generic.to_from",
          (taxYear - 1).toString,
          taxYear.toString))
    }

    "display an error page for an invalid request" in {
      val result = Future.successful(sut.show(badRequest))
      status(result) shouldBe 400
      val document = Jsoup.parse(contentAsString(result))
      document.title should include(Messages("generic.error.html.title"))
    }

    "redirect to the no ATS page when there is no annual tax summary data returned" in {
      when(
        mockGovernmentSpendService.getGovernmentSpendData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(new NoATSViewModel))
      val result = Future.successful(sut.show(request))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.ErrorController.authorisedNoAts().url
    }

    "have correct data for 2014" in {

      val result = Future.successful(sut.show(request))
      val document = Jsoup.parse(contentAsString(result))
      document.select("#welfare + td").text() shouldBe "£5,863.22"
      document.select("#welfare").text() should include("24.52%")
      document.select("#health + td").text() shouldBe "£4,512.19"
      document.select("#health").text() should include("18.87%")
      document.select("#education + td").text() shouldBe "£3,144.43"
      document.select("#education").text() should include("13.15%")
      document.select("#pension + td").text() shouldBe "£2,898.13"
      document.select("#pension").text() should include("12.12%")
      document.select("#national_debt_interest + td").text() shouldBe "£1,673.84"
      document.select("#national_debt_interest").text() should include("7.0%")
      document.select("#defence + td").text() shouldBe "£1,269.73"
      document.select("#defence").text() should include("5.31%")
      document.select("#criminal_justice + td").text() shouldBe "£1,052.13"
      document.select("#criminal_justice").text() should include("4.4%")
      document.select("#transport + td").text() shouldBe "£705.40"
      document.select("#transport").text() should include("2.95%")
      document.select("#business_and_industry + td").text() shouldBe "£655.19"
      document.select("#business_and_industry").text() should include("2.74%")
      document.select("#government_administration + td").text() shouldBe "£490.20"
      document.select("#government_administration").text() should include("2.05%")
      document.select("#Culture + td").text() shouldBe "£404.11"
      document.select("#Culture").text() should include("1.69%")
      document.select("#Environment + td").text() shouldBe "£396.94"
      document.select("#Environment").text() should include("1.66%")
      document.select("#HousingAndUtilities + td").text() shouldBe "£392.16"
      document.select("#HousingAndUtilities").text() should include("1.64%")
      document.select("#overseas_aid + td").text() shouldBe "£274.99"
      document.select("#overseas_aid").text() should include("1.15%")
      document.select("#uk_contribution_to_eu_budget + td").text() shouldBe "£179.34"
      document.select("#uk_contribution_to_eu_budget").text() should include("0.75%")

      document.getElementById("user-info").text() should include("userForename userSurname")
      document.getElementById("user-info").text() should include("Unique Taxpayer Reference: " + testUtr)
      document.select("#gov-spend-total + td").text() shouldBe "£23,912.00"
      document
        .select("h1")
        .text shouldBe "Tax year: April 6 2013 to April 5 2014 Your taxes and public spending"
    }

    "have correct data for 2015" in {

      val model2 = new GovernmentSpend(
        taxYear = 2015,
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
        mockGovernmentSpendService.getGovernmentSpendData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(model2))

      val result = Future.successful(sut.show(request))
      val document = Jsoup.parse(contentAsString(result))

      document.select("#welfare + td").text() shouldBe "£2,530.00"
      document.select("#welfare").text() should include("25.3%")
      document.select("#health + td").text() shouldBe "£1,990.00"
      document.select("#health").text() should include("19.9%")
      document.select("#pension + td").text() shouldBe "£1,280.00"
      document.select("#pension").text() should include("12.8%")
      document.select("#education + td").text() shouldBe "£1,250.00"
      document.select("#education").text() should include("12.5%")
      document.select("#defence + td").text() shouldBe "£540.00"
      document.select("#defence").text() should include("5.4%")
      document.select("#national_debt_interest + td").text() shouldBe "£500.00"
      document.select("#national_debt_interest").text() should include("5.0%")
      document.select("#public_order_and_safety + td").text() shouldBe "£440.00"
      document.select("#public_order_and_safety").text() should include("4.4%")
      document.select("#transport + td").text() shouldBe "£300.00"
      document.select("#transport").text() should include("3.0%")
      document.select("#business_and_industry + td").text() shouldBe "£270.00"
      document.select("#business_and_industry").text() should include("2.7%")
      document.select("#government_administration + td").text() shouldBe "£200.00"
      document.select("#government_administration").text() should include("2.0%")
      document.select("#Culture + td").text() shouldBe "£180.00"
      document.select("#Culture").text() should include("1.8%")
      document.select("#Environment + td").text() shouldBe "£170.00"
      document.select("#Environment").text() should include("1.7%")
      document.select("#HousingAndUtilities + td").text() shouldBe "£160.00"
      document.select("#HousingAndUtilities").text() should include("1.6%")
      document.select("#overseas_aid + td").text() shouldBe "£130.00"
      document.select("#overseas_aid").text() should include("1.3%")
      document.select("#uk_contribution_to_eu_budget + td").text() shouldBe "£600.00"
      document.select("#uk_contribution_to_eu_budget").text() should include("0.6%")

      document.getElementById("user-info").text() should include("userForename userSurname")
      document.getElementById("user-info").text() should include("Unique Taxpayer Reference: " + testUtr)
      document.select("#gov-spend-total + td").text() shouldBe "£10,000.00"
    }

    "show 'Government spend' page with a correct breadcrumb" in {

      val result = Future.successful(sut.show(request))
      val document = Jsoup.parse(contentAsString(result))

      document.select("#global-breadcrumb li:nth-child(1) a").attr("href") should include("/account")
      document.select("#global-breadcrumb li:nth-child(1) a").text should include("Home")

      document.select("#global-breadcrumb li:nth-child(2) a").attr("href") should include("/annual-tax-summary")
      document.select("#global-breadcrumb li:nth-child(2) a").text shouldBe "Select the tax year"

      document.select("#global-breadcrumb li:nth-child(3) a").attr("href") should include(
        "/annual-tax-summary/main?taxYear=2014")
      document.select("#global-breadcrumb li:nth-child(3) a").text shouldBe "Your annual tax summary"

      document.select("#global-breadcrumb li:nth-child(4)").toString should include(
        "<strong>Your taxes and public spending</strong>")
    }

    "return zero percentage for Housing, Cultural and Environment when they are not same" in {

      val govSpendAmountData = List(
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
      )

      val result = sut.assignPercentage(govSpendAmountData)

      result._1 shouldBe 1.7
      result._2 shouldBe 1.8
      result._3 shouldBe 1.6

    }

    "return equal percentage for Housing, Cultural and Environment when they are same" in {

      val expected = 1.8

      val govSpendAmountData = List(
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
        ("Culture", SpendData(Amount(180, "GBP"), expected)),
        ("Environment", SpendData(Amount(180, "GBP"), expected)),
        ("HousingAndUtilities", SpendData(Amount(180, "GBP"), expected)),
        ("overseas_aid", SpendData(Amount(130, "GBP"), 1.3)),
        ("uk_contribution_to_eu_budget", SpendData(Amount(600, "GBP"), 0.6))
      )

      val result = sut.assignPercentage(govSpendAmountData)

      result._1 shouldBe expected
      result._2 shouldBe expected
      result._3 shouldBe expected

    }

  }
}
