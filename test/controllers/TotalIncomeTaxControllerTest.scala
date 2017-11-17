/*
 * Copyright 2017 HM Revenue & Customs
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

import org.jsoup.Jsoup
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest.mock.MockitoSugar
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services._
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import uk.gov.hmrc.play.test.UnitSpec
import utils.AuthorityUtils
import view_models.{TotalIncomeTax, Rate, Amount}
import utils.TestConstants._
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class TotalIncomeTaxControllerTest extends UnitSpec with FakeTaxsPlayApplication with MockitoSugar {

  val request = FakeRequest()
  val user = User(AuthorityUtils.saAuthority(testOid, testUtr))

  val baseModel = TotalIncomeTax(
    year = 2014,
    utr = testUtr,
    startingRateForSavings = Amount(110, "GBP"),
    startingRateForSavingsAmount = Amount(140, "GBP"),
    basicRateIncomeTax = Amount(1860, "GBP"),
    basicRateIncomeTaxAmount = Amount(372, "GBP"),
    higherRateIncomeTax = Amount(130, "GBP"),
    higherRateIncomeTaxAmount = Amount(70, "GBP"),
    additionalRateIncomeTax = Amount(80, "GBP"),
    additionalRateIncomeTaxAmount = Amount(60, "GBP"),
    ordinaryRate = Amount(100, "GBP"),
    ordinaryRateAmount = Amount(50, "GBP"),
    upperRate = Amount(30, "GBP"),
    upperRateAmount = Amount(120, "GBP"),
    additionalRate = Amount(10, "GBP"),
    additionalRateAmount = Amount(40, "GBP"),
    otherAdjustmentsIncreasing = Amount(90, "GBP"),
    otherAdjustmentsReducing = Amount(20, "GBP"),
    totalIncomeTax = Amount(372, "GBP"),
    startingRateForSavingsRateRate = Rate("10%"),
    basicRateIncomeTaxRateRate = Rate("20%"),
    higherRateIncomeTaxRateRate = Rate("40%"),
    additionalRateIncomeTaxRateRate = Rate("45%"),
    ordinaryRateTaxRateRate = Rate("10%"),
    upperRateRateRate = Rate("32.5%"),
    additionalRateRateRate = Rate("37.5%"),
    "Mr",
    "forename",
    "surname"
  )

  trait TestController extends TotalIncomeTaxController {

    override lazy val totalIncomeTaxService = mock[TotalIncomeTaxService]
    override lazy val auditService: AuditService = mock[AuditService]
    val model = baseModel

    when(totalIncomeTaxService.getIncomeData(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)
  }

  "Calling Total Income Tax with no session" should {

    "return a 303 response" in new TestController {

      val result = Future.successful(authorisedTotalIncomeTax(request))
      status(result) shouldBe 303
    }
  }

  "Calling Total Income Tax with session" should {

    "have the right user data in the view" in new TestController {

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString should not include "Technical Difficulties"
      document.getElementById("starting-rate-for-savings-amount").text() shouldBe "£140"
      document.getElementById("start-rate-for-savings-before").text() shouldBe "£110"
      document.getElementById("start-rate-for-savings-rate").text() shouldBe "10%"

      document.getElementById("basic-rate-income-tax-amount").text() shouldBe "£372"
      document.getElementById("basic-rate-income-tax-before").text() shouldBe "£1,860"
      document.getElementById("basic-rate-income-tax-rate").text() shouldBe "20%"

      document.getElementById("higher-rate-income-tax-amount").text() shouldBe "£70"
      document.getElementById("higher-rate-income-tax-before").text() shouldBe "£130"
      document.getElementById("higher-rate-income-tax-rate").text() shouldBe "40%"

      document.getElementById("additional-rate-income-tax-amount").text() shouldBe "£60"
      document.getElementById("additional-rate-income-tax-before").text() shouldBe "£80"
      document.getElementById("additional-rate-income-tax-rate").text() shouldBe "45%"

      document.getElementById("ordinary-rate-amount").text() shouldBe "£50"
      document.getElementById("total-income-tax-amount").text() shouldBe "£372"

      document.toString should include("Total Income Tax")
      document.getElementById("user-info").text should include("forename surname")
      document.getElementById("user-info").text should include("Unique Taxpayer Reference: "+testUtr)
    }

    "hide rows if there is a zero value in the left cell amount field of the view" in new TestController {

      override val model = baseModel.copy(
        startingRateForSavings = Amount(0, "GBP"),
        basicRateIncomeTax = Amount(0, "GBP")
      )

      when(totalIncomeTaxService.getIncomeData(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString should not include "Technical Difficulties"
      document.toString should not include "starting-rate-for-savings-row"
      document.toString should not include "basic-rate-income-tax-row"
      document.toString should not include "higher-rate-income-tax-row"
      document.toString should not include "additional-rate-income-tax-row"
    }

    "hide Higher and Additional Rate fields if the amounts are 0.00" in new TestController {

      override val model = baseModel.copy(
        higherRateIncomeTax = Amount(0, "GBP"),
        additionalRateIncomeTax = Amount(0, "GBP")
      )

      when(totalIncomeTaxService.getIncomeData(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString should not include "Technical Difficulties"
      document.toString should include("basic-rate-income-tax-row")
      document.toString should not include "higher-rate-income-tax-row"
      document.toString should not include "additional-rate-income-tax-row"
    }

    "show 'Total Income Tax page with a correct breadcrumb" in new TestController {

      val result = Future.successful(show(user, request))

      val document = Jsoup.parse(contentAsString(result))

      document.select("#global-breadcrumb li:nth-child(1) a").toString should include("/account\">")
      document.select("#global-breadcrumb li:nth-child(1) a").text should include("Home")

      document.select("#global-breadcrumb li:nth-child(2) a").toString should include("<a href=\"/annual-tax-summary\">")
      document.select("#global-breadcrumb li:nth-child(2) a").text shouldBe "Select the tax year"

      document.select("#global-breadcrumb li:nth-child(3) a").toString should include("<a href=\"/annual-tax-summary/main?taxYear=2014\">")
      document.select("#global-breadcrumb li:nth-child(3) a").text shouldBe "Your annual tax summary"

      document.select("#global-breadcrumb li:nth-child(4) a").toString should include("<a href=\"/annual-tax-summary/summary?taxYear=2014\">")
      document.select("#global-breadcrumb li:nth-child(4) a").text should include("Your income and taxes")

      document.select("#global-breadcrumb li:nth-child(5) a").toString should include("<a href=\"/annual-tax-summary/nics?taxYear=2014\">")
      document.select("#global-breadcrumb li:nth-child(5) a").text should include("Your Income Tax and National Insurance")

      document.select("#global-breadcrumb li:nth-child(6)").toString should include("<strong>Income Tax</strong>")
    }
  }

    "Dividends section" should {
      "have the right user data for Ordinary, Additional and Higher Rates fields in the view" in new TestController {

        val result = Future.successful(show(user, request))
        status(result) shouldBe 200
        val document = Jsoup.parse(contentAsString(result))

        document.toString should not include "Technical Difficulties"
        document.getElementById("ordinary-rate-amount").text() should equal("£50")
        document.getElementById("ordinary-rate-before").text() should equal("£100")
        document.getElementById("ordinary-rate-rate").text() should equal("10%")

        document.getElementById("upper-rate-amount").text() should equal("£120")
        document.getElementById("upper-rate-before").text() should equal("£30")
        document.getElementById("upper-rate-rate").text() should equal("32.5%")

        document.getElementById("additional-rate-amount").text() should equal("£40")
        document.getElementById("additional-rate-before").text() should equal("£10")
        document.getElementById("additional-rate-rate").text() should equal("37.5%")
      }

      "hide Dividends section if the amount before in each row is 0.00" in new TestController {

        override val model = baseModel.copy(
          ordinaryRate = Amount(0, "GBP"),
          upperRate = Amount(0, "GBP"),
          additionalRate = Amount(0, "GBP")
        )

        when(totalIncomeTaxService.getIncomeData(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)

        val result = Future.successful(show(user, request))
        status(result) shouldBe 200
        val document = Jsoup.parse(contentAsString(result))

        document.toString should not include "Technical Difficulties"
        document.toString should not include "dividends-section-row"
        document.toString should not include "ordinary-rate-row"
        document.toString should not include "upper-rate-row"
        document.toString should not include "additional-rate-row"
      }

      "not hide Dividends section if only Ordinary rate amount is greater than 0.00" in new TestController {

        override val model = baseModel.copy(
          upperRate = Amount(0, "GBP"),
          additionalRate = Amount(0, "GBP")
        )

        when(totalIncomeTaxService.getIncomeData(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)

        val result = Future.successful(show(user, request))
        status(result) shouldBe 200
        val document = Jsoup.parse(contentAsString(result))

        document.toString should not include "Technical Difficulties"
        document.toString should include("dividends-section-row")
        document.toString should include("ordinary-rate-row")
        document.toString should not include "upper-rate-row"
        document.toString should not include "additional-rate-row"
      }

    "Adjustments section" should {

      "have the right user data for adjustments increasing and reducing income tax" in new TestController {

        val result = Future.successful(show(user, request))
        status(result) shouldBe 200
        val document = Jsoup.parse(contentAsString(result))

        document.getElementById("other-adjustments-increasing-amount").text() should equal("£90")
        document.getElementById("other-adjustments-reducing-amount").text() should equal("minus £20 -£20")
      }

      "hide other adjustments increasing your tax section if the amount is 0.00" in new TestController {

        override val model = baseModel.copy(
          otherAdjustmentsIncreasing = Amount(0, "GBP")
        )

        when(totalIncomeTaxService.getIncomeData(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)

        val result = Future.successful(show(user, request))
        status(result) shouldBe 200
        val document = Jsoup.parse(contentAsString(result))

        document.toString should not include "Technical Difficulties"
        document.toString should not include "other-adjustments-increasing-amount"
      }

      "hide other adjustments reducing your tax section if the amount is 0.00" in new TestController {

        override val model = baseModel.copy(
          otherAdjustmentsReducing = Amount(0, "GBP")
        )

        when(totalIncomeTaxService.getIncomeData(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)

        val result = Future.successful(show(user, request))
        status(result) shouldBe 200
        val document = Jsoup.parse(contentAsString(result))

        document.toString should not include "Technical Difficulties"
        document.toString should not include "other-adjustments-reducing-amount"
      }

      "hide Adjustments section if all the amounts in this section are 0.00" in new TestController {

        override val model = baseModel.copy(
          otherAdjustmentsIncreasing = Amount(0, "GBP"),
          otherAdjustmentsReducing = Amount(0, "GBP")
        )

        when(totalIncomeTaxService.getIncomeData(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)

        val result = Future.successful(show(user, request))
        status(result) shouldBe 200
        val document = Jsoup.parse(contentAsString(result))

        document.toString should not include "Technical Difficulties"
        document.toString should not include "adjustments-section"
      }
    }

    "Total Income Tax" should {

      "show zero value" in new TestController {

        override val model = baseModel.copy(
          totalIncomeTax = Amount(0, "GBP")
        )

        when(totalIncomeTaxService.getIncomeData(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)

        val result = Future.successful(show(user, request))
        val document = Jsoup.parse(contentAsString(result))

        document.getElementById("total-income-tax-amount").text() should equal("£0")
      }
    }
  }
}
