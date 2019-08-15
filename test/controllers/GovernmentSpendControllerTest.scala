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
import models.SpendData
import org.jsoup.Jsoup
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.Request
import play.api.test.Helpers._
import play.api.test.FakeRequest
import services._
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import uk.gov.hmrc.play.test.UnitSpec
import utils.AuthorityUtils
import view_models.{Amount, GovernmentSpend}

import scala.concurrent.Future
import utils.TestConstants._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.FormPartialRetriever


class GovernmentSpendControllerTest extends UnitSpec with FakeTaxsPlayApplication with MockitoSugar {

  val request = FakeRequest()
  val user = User(AuthorityUtils.saAuthority(testOid, testUtr))
  val taxYear = 2014

  trait TestController extends GovernmentSpendController {

    override lazy val governmentSpendService: GovernmentSpendService = mock[GovernmentSpendService]
    override lazy val auditService: AuditService = mock[AuditService]
    implicit lazy val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever


    val model = new GovernmentSpend(
      taxYear = 2014,
      userUtr = testUtr,
      govSpendAmountData = List (
        ("welfare" , SpendData(Amount(5863.22, "GBP"), 24.52)),
        ("health", SpendData(Amount(4512.19, "GBP"), 18.87)),
        ("education" , SpendData(Amount(3144.43, "GBP"), 13.15)),
        ("pension" , SpendData(Amount(2898.13, "GBP"), 12.12)),
        ("national_debt_interest" , SpendData(Amount(1673.84, "GBP"), 7.00)),
        ("defence" , SpendData(Amount(1269.73, "GBP"), 5.31)),
        ("criminal_justice" , SpendData(Amount(1052.13, "GBP"), 4.40)),
        ("transport" , SpendData(Amount(705.4, "GBP"), 2.95)),
        ("business_and_industry" , SpendData(Amount(655.19, "GBP"), 2.74)),
        ("government_administration" , SpendData(Amount(490.2, "GBP"), 2.05)),
        ("Culture" , SpendData(Amount(404.11, "GBP"), 1.69)),
        ("HousingAndUtilities" , SpendData(Amount(392.16, "GBP"), 1.64)),
        ("Environment" , SpendData(Amount(396.94, "GBP"), 1.66)),
        ("overseas_aid" , SpendData(Amount(274.99, "GBP"), 1.15)),
        ("uk_contribution_to_eu_budget" , SpendData(Amount(179.34, "GBP"), 0.75))
      ),
      userTitle = "Mr",
      userForename = "userForename",
      userSurname = "userSurname",
      totalAmount = new Amount(23912.00, "GBP"),
      incomeTaxStatus = "0002",
      scottishIncomeTax = new Amount(2000.00, "GBP")
    )

    when(governmentSpendService.getGovernmentSpendData(taxYear)(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)
  }

  "Calling government spend with no session" should {

    "return a 303 response" in new TestController {

      val result = Future.successful(authorisedGovernmentSpendData(request))

      status(result) shouldBe 303
    }
  }

  "Calling government spend with session" should {

    "return a 200 response" in new TestController {

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
    }

    "have correct data for 2014" in new TestController {

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.select("#welfare + td").text() shouldBe "£5,863.22"
      document.select("#welfare").text() should include ("24.52%")
      document.select("#health + td").text() shouldBe "£4,512.19"
      document.select("#health").text() should include ("18.87%")
      document.select("#education + td").text() shouldBe "£3,144.43"
      document.select("#education").text() should include ("13.15%")
      document.select("#pension + td").text() shouldBe "£2,898.13"
      document.select("#pension").text() should include ("12.12%")
      document.select("#national_debt_interest + td").text() shouldBe "£1,673.84"
      document.select("#national_debt_interest").text() should include ("7.0%")
      document.select("#defence + td").text() shouldBe "£1,269.73"
      document.select("#defence").text() should include ("5.31%")
      document.select("#criminal_justice + td").text() shouldBe "£1,052.13"
      document.select("#criminal_justice").text() should include ("4.4%")
      document.select("#transport + td").text() shouldBe "£705.40"
      document.select("#transport").text() should include ("2.95%")
      document.select("#business_and_industry + td").text() shouldBe "£655.19"
      document.select("#business_and_industry").text() should include ("2.74%")
      document.select("#government_administration + td").text() shouldBe "£490.20"
      document.select("#government_administration").text() should include ("2.05%")
      document.select("#Culture + td").text() shouldBe "£404.11"
      document.select("#Culture").text() should include ("1.69%")
      document.select("#Environment + td").text() shouldBe "£396.94"
      document.select("#Environment").text() should include ("1.66%")
      document.select("#HousingAndUtilities + td").text() shouldBe "£392.16"
      document.select("#HousingAndUtilities").text() should include ("1.64%")
      document.select("#overseas_aid + td").text() shouldBe "£274.99"
      document.select("#overseas_aid").text() should include ("1.15%")
      document.select("#uk_contribution_to_eu_budget + td").text() shouldBe "£179.34"
      document.select("#uk_contribution_to_eu_budget").text() should include ("0.75%")

      document.getElementById("user-info").text() should include("userForename userSurname")
      document.getElementById("user-info").text() should include("Unique Taxpayer Reference: "+testUtr)
      document.select("#gov-spend-total + td").text() shouldBe "£23,912.00"
      document.select(".page-header h1").text shouldBe "Tax year: April 6 2013 to April 5 2014 Your taxes and public spending"
    }

    "have correct data for 2015" in new TestController {

      override val model = new GovernmentSpend(
        taxYear = 2015,
        userUtr = testUtr,
        govSpendAmountData = List (
          ("welfare" , SpendData(Amount(2530 ,"GBP"), 25.3)),
          ("health", SpendData(Amount(1990 ,"GBP"), 19.9)),
          ("pension" , SpendData(Amount(1280 ,"GBP"), 12.8)),
          ("education" , SpendData(Amount(1250 ,"GBP"), 12.5)),
          ("defence" , SpendData(Amount(540 , "GBP"), 5.4)),
          ("national_debt_interest" , SpendData(Amount(500 ,"GBP"), 5.0)),
          ("public_order_and_safety" , SpendData(Amount(440 ,"GBP"), 4.4)),
          ("transport" , SpendData(Amount(300 ,"GBP"), 3.0)),
          ("business_and_industry" , SpendData(Amount(270 ,"GBP"), 2.7)),
          ("government_administration" , SpendData(Amount(200 ,"GBP"), 2.0)),
          ("Culture" , SpendData(Amount(180 ,"GBP"), 1.8)),
          ("Environment" , SpendData(Amount(170 ,"GBP"), 1.7)),
          ("HousingAndUtilities" , SpendData(Amount(160 ,"GBP"), 1.6)),
          ("overseas_aid" , SpendData(Amount(130 ,"GBP"), 1.3 )),
          ("uk_contribution_to_eu_budget" , SpendData(Amount(600 ,"GBP"), 0.6))
        ),
        userTitle = "Mr",
        userForename = "userForename",
        userSurname = "userSurname",
        totalAmount = new Amount(10000.0,"GBP"),
        incomeTaxStatus = "0002",
        scottishIncomeTax = new Amount(2000.00, "GBP")
      )

      when(governmentSpendService.getGovernmentSpendData(taxYear)(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.select("#welfare + td").text() shouldBe "£2,530.00"
      document.select("#welfare").text() should include ("25.3%")
      document.select("#health + td").text() shouldBe "£1,990.00"
      document.select("#health").text() should include ("19.9%")
      document.select("#pension + td").text() shouldBe "£1,280.00"
      document.select("#pension").text() should include ("12.8%")
      document.select("#education + td").text() shouldBe "£1,250.00"
      document.select("#education").text() should include ("12.5%")
      document.select("#defence + td").text() shouldBe "£540.00"
      document.select("#defence").text() should include ("5.4%")
      document.select("#national_debt_interest + td").text() shouldBe "£500.00"
      document.select("#national_debt_interest").text() should include ("5.0%")
      document.select("#public_order_and_safety + td").text() shouldBe "£440.00"
      document.select("#public_order_and_safety").text() should include ("4.4%")
      document.select("#transport + td").text() shouldBe "£300.00"
      document.select("#transport").text() should include ("3.0%")
      document.select("#business_and_industry + td").text() shouldBe "£270.00"
      document.select("#business_and_industry").text() should include ("2.7%")
      document.select("#government_administration + td").text() shouldBe "£200.00"
      document.select("#government_administration").text() should include ("2.0%")
      document.select("#Culture + td").text() shouldBe "£180.00"
      document.select("#Culture").text() should include ("1.8%")
      document.select("#Environment + td").text() shouldBe "£170.00"
      document.select("#Environment").text() should include ("1.7%")
      document.select("#HousingAndUtilities + td").text() shouldBe "£160.00"
      document.select("#HousingAndUtilities").text() should include ("1.6%")
      document.select("#overseas_aid + td").text() shouldBe "£130.00"
      document.select("#overseas_aid").text() should include ("1.3%")
      document.select("#uk_contribution_to_eu_budget + td").text() shouldBe "£600.00"
      document.select("#uk_contribution_to_eu_budget").text() should include ("0.6%")

      document.getElementById("user-info").text() should include("userForename userSurname")
      document.getElementById("user-info").text() should include("Unique Taxpayer Reference: " + testUtr)
      document.select("#gov-spend-total + td").text() shouldBe "£10,000.00"
    }


  }

  "show 'Government spend' page with a correct breadcrumb" in new TestController {

    val result = Future.successful(show(user, request))
    val document = Jsoup.parse(contentAsString(result))

    document.select("#global-breadcrumb li:nth-child(1) a").attr("href") should include("/account")
    document.select("#global-breadcrumb li:nth-child(1) a").text should include("Home")

    document.select("#global-breadcrumb li:nth-child(2) a").attr("href") should include("/annual-tax-summary")
    document.select("#global-breadcrumb li:nth-child(2) a").text shouldBe "Select the tax year"

    document.select("#global-breadcrumb li:nth-child(3) a").attr("href") should include("/annual-tax-summary/main?taxYear=2014")
    document.select("#global-breadcrumb li:nth-child(3) a").text shouldBe "Your annual tax summary"

    document.select("#global-breadcrumb li:nth-child(4)").toString should include("<strong>Your taxes and public spending</strong>")
  }

  "return zero percentage for Housing, Cultural and Environment when they are not same" in new TestController {

    val govSpendAmountData = List (
      ("welfare" , SpendData(Amount(2530 ,"GBP"), 25.3)),
      ("health", SpendData(Amount(1990 ,"GBP"), 19.9)),
      ("pension" , SpendData(Amount(1280 ,"GBP"), 12.8)),
      ("education" , SpendData(Amount(1250 ,"GBP"), 12.5)),
      ("defence" , SpendData(Amount(540 , "GBP"), 5.4)),
      ("national_debt_interest" , SpendData(Amount(500 ,"GBP"), 5.0)),
      ("public_order_and_safety" , SpendData(Amount(440 ,"GBP"), 4.4)),
      ("transport" , SpendData(Amount(300 ,"GBP"), 3.0)),
      ("business_and_industry" , SpendData(Amount(270 ,"GBP"), 2.7)),
      ("government_administration" , SpendData(Amount(200 ,"GBP"), 2.0)),
      ("Culture" , SpendData(Amount(180 ,"GBP"), 1.8)),
      ("Environment" , SpendData(Amount(170 ,"GBP"), 1.7)),
      ("HousingAndUtilities" , SpendData(Amount(160 ,"GBP"), 1.6)),
      ("overseas_aid" , SpendData(Amount(130 ,"GBP"), 1.3 )),
      ("uk_contribution_to_eu_budget" , SpendData(Amount(600 ,"GBP"), 0.6))
    )

    val result = assignPercentage(govSpendAmountData)

    result shouldBe(1.7, 1.8, 1.6)

  }

  "return equal percentage for Housing, Cultural and Environment when they are same" in new TestController {

    val govSpendAmountData = List (
      ("welfare" , SpendData(Amount(2530 ,"GBP"), 25.3)),
      ("health", SpendData(Amount(1990 ,"GBP"), 19.9)),
      ("pension" , SpendData(Amount(1280 ,"GBP"), 12.8)),
      ("education" , SpendData(Amount(1250 ,"GBP"), 12.5)),
      ("defence" , SpendData(Amount(540 , "GBP"), 5.4)),
      ("national_debt_interest" , SpendData(Amount(500 ,"GBP"), 5.0)),
      ("public_order_and_safety" , SpendData(Amount(440 ,"GBP"), 4.4)),
      ("transport" , SpendData(Amount(300 ,"GBP"), 3.0)),
      ("business_and_industry" , SpendData(Amount(270 ,"GBP"), 2.7)),
      ("government_administration" , SpendData(Amount(200 ,"GBP"), 2.0)),
      ("Culture" , SpendData(Amount(180 ,"GBP"), 1.8)),
      ("Environment" , SpendData(Amount(180 ,"GBP"), 1.8)),
      ("HousingAndUtilities" , SpendData(Amount(180 ,"GBP"), 1.8)),
      ("overseas_aid" , SpendData(Amount(130 ,"GBP"), 1.3 )),
      ("uk_contribution_to_eu_budget" , SpendData(Amount(600 ,"GBP"), 0.6))
    )

    val result = assignPercentage(govSpendAmountData)

    result shouldBe(1.8, 1.8, 1.8)

  }

}
