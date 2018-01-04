/*
 * Copyright 2018 HM Revenue & Customs
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
import play.api.test.Helpers._
import play.api.test.FakeRequest
import services._
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import uk.gov.hmrc.play.test.UnitSpec
import utils.{GenericViewModel, AuthorityUtils}
import view_models.{NoATSViewModel, Rate, Summary, Amount}
import utils.TestConstants._
import scala.concurrent.Future
import scala.math.BigDecimal.double2bigDecimal
import uk.gov.hmrc.http.HeaderCarrier


object SummaryControllerTest {

  val baseModel = Summary(
    year = 2014,
    utr = testUtr,
    employeeNicAmount = Amount(1200, "GBP"),
    totalIncomeTaxAndNics = Amount(1400, "GBP"),
    yourTotalTax = Amount(1800, "GBP"),
    totalTaxFree = Amount(9440, "GBP"),
    totalTaxFreeAllowance = Amount(9740, "GBP"),
    yourIncomeBeforeTax = Amount(11600, "GBP"),
    totalIncomeTaxAmount = Amount(372, "GBP"),
    totalCapitalGainsTax = Amount(5500, "GBP"),
    taxableGains = Amount(20000, "GBP"),
    cgTaxPerCurrencyUnit = Amount(0.1234, "GBP"),
    nicsAndTaxPerCurrencyUnit = Amount(0.5678, "GBP"),
    totalCgTaxRate = Rate("12.34%"),
    nicsAndTaxRate = Rate("56.78%"),
    title = "Mr",
    forename = "forename",
    surname = "surname"
  )
}

class SummaryControllerTest extends UnitSpec with FakeTaxsPlayApplication with MockitoSugar {

  val request = FakeRequest()
  val user = User(AuthorityUtils.saAuthority(testOid, testUtr))
  val baseModel = SummaryControllerTest.baseModel

  trait TestController extends SummaryController {

    override lazy val summaryService = mock[SummaryService]
    override lazy val auditService = mock[AuditService]

    val model: GenericViewModel = baseModel

    when(summaryService.getSummaryData(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)
  }

  "Calling Summary with no session" should {

    "return a 303 response" in new TestController {

      val result = Future.successful(authorisedSummaries(request))
      status(result) shouldBe 303
    }
  }

  "Calling Summary with session" should {

    "have the right user data in the view" in new TestController {

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString contains "Tax calculation"
      document.getElementById("income-before-tax-amount").text() shouldBe "£11,600"
      document.getElementById("user-info").text should include("forename surname")
      document.getElementById("user-info").text should include("Unique Taxpayer Reference: "+testUtr)
    }

    "have the correct tax free amount" in new TestController {

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-tax-free-amount").text() shouldBe "£9,740"
    }

    "still show a 0 tax free amount" in new TestController {

      override val model = baseModel.copy(
        totalTaxFreeAllowance = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-tax-free-amount").text() shouldBe "£0"
    }

    "show total income tax and NICs value on the summary page" in new TestController {

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-income-tax-and-nics").text() shouldBe "£1,400"
    }

    "show capital gains (and description) on the summary if capital gains is not 0" in new TestController {

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("capital-gains") should not be null
    }

    "not show capital gains on the summary if capital gains is 0" in new TestController {

      override val model = baseModel.copy(
        taxableGains = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString should not include("Technical Difficulties")
      document.getElementById("capital-gains") should be(null)
    }

    "show Total Capital Gains Tax value" in new TestController {

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-capital-gains-tax").text() should equal("£5,500")
    }

    "show capital gains description on the summary if total capital gains tax is not 0" in new TestController {

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-cg-description") should not be null
    }

    "hide capital gains description on the summary if total capital gains tax is  0" in new TestController {

      override val model = baseModel.copy(
        totalCapitalGainsTax = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString should not include("Technical Difficulties")
      document.getElementById("total-cg-description") should be (null)
    }

    "show zero in Total Income Tax value" in new TestController {

      override val model = baseModel.copy(
        totalIncomeTaxAndNics = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-income-tax-and-nics").text() should equal("£0")
    }

    "show Tax and Nics description having (income tax and employee nics)" in new TestController {

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("tax-and-nics-title").text() should equal("Income Tax and National Insurance")
    }

    "show Tax and Nics description having only (total income tax)" in new TestController {

      override val model = baseModel.copy(
        employeeNicAmount = Amount(0, "GBP"),
        totalCapitalGainsTax = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("tax-and-nics-title").text() should equal("Your tax was calculated as")
    }

    "show Tax and Nics description having only (capital gains)" in new TestController {

      override val model = baseModel.copy(
        totalIncomeTaxAmount = Amount(0, "GBP"),
        employeeNicAmount = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("tax-and-nics-title").text() should equal("Your tax was calculated as")
    }

    "show Tax and Nics description having only (employee nics)" in new TestController {

      override val model = baseModel.copy(
        totalIncomeTaxAmount = Amount(0, "GBP"),
        totalCapitalGainsTax = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("tax-and-nics-title").text() should equal("Your NICs were calculated as")
    }

    "show Tax and Nics description having only (total income tax, employee nics)" in new TestController {

      override val model = baseModel.copy(
        totalCapitalGainsTax = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("tax-and-nics-title").text() should equal("Income Tax and National Insurance")
    }

    "show Tax and Nics description having only (capital gains, employee nics)" in new TestController {

      override val model = baseModel.copy(
        totalIncomeTaxAmount = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("tax-and-nics-title").text() should equal("Your NICs were calculated as")
    }

    "show Your Total Tax as sum of Income Tax, capital gains and employee nics)" in new TestController {

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-tax-amount").text() shouldBe "£1,800"
    }

    "show Your Total Tax description having (total income tax, capital gains, employee nics)" in new TestController {

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-tax-description").text() should equal("Your total income tax, National Insurance and Capital Gains Tax.")
    }

    "show Your Total Tax description having only (total income tax)" in new TestController {

      override val model = baseModel.copy(
        employeeNicAmount = Amount(0, "GBP"),
        totalCapitalGainsTax = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-tax-description").text() should equal("Your total income tax.")
    }

    "show Your Total Tax description having only (capital gains)" in new TestController {

      override val model = baseModel.copy(
        totalIncomeTaxAmount = Amount(0, "GBP"),
        employeeNicAmount = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-tax-description").text() should equal("Your Capital Gains Tax.")
    }

    "show Your Total Tax description having only (employee nics)" in new TestController {

      override val model = baseModel.copy(
        totalIncomeTaxAmount = Amount(0, "GBP"),
        totalCapitalGainsTax = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-tax-description").text() should equal("Your National Insurance.")
    }

    "show Your Total Tax description having only (total income tax, capital gains)" in new TestController {

      override val model = baseModel.copy(
        employeeNicAmount = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-tax-description").text() should equal("Your total income tax and Capital Gains Tax.")
    }

    "show Your Total Tax description having only (total income tax, employee nics)" in new TestController {

      override val model = baseModel.copy(
        totalCapitalGainsTax = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-tax-description").text() should equal("Your total income tax and National Insurance.")
    }

    "show Your Total Tax description having only (capital gains, employee nics)" in new TestController {

      override val model = baseModel.copy(
        totalIncomeTaxAmount = Amount(0, "GBP")
      )

      when(summaryService.getSummaryData(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("total-tax-description").text() should equal("Your National Insurance and Capital Gains Tax.")
    }

    "show 'Summary' page with a correct breadcrumb" in new TestController {

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.select("#global-breadcrumb li:nth-child(1) a").toString should include("/account\">")
      document.select("#global-breadcrumb li:nth-child(1) a").text should include("Home")

      document.select("#global-breadcrumb li:nth-child(2) a").toString should include("<a href=\"/annual-tax-summary\">")
      document.select("#global-breadcrumb li:nth-child(2) a").text shouldBe "Select the tax year"

      document.select("#global-breadcrumb li:nth-child(3) a").toString should include("<a href=\"/annual-tax-summary/main?taxYear=2014\">")
      document.select("#global-breadcrumb li:nth-child(3) a").text shouldBe "Your annual tax summary"

      document.select("#global-breadcrumb li:nth-child(4)").toString should include("<strong>Your income and taxes</strong>")
    }

    "Redirect to 'No ATS' page" in new TestController {

      override val model = new NoATSViewModel

      when(summaryService.getSummaryData(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)

      val result = Future.successful(show(user, request))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/annual-tax-summary/no-ats")
    }

    "show 'ATS error' page with a correct breadcrumb" in new TestController {

      when(summaryService.getSummaryData(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(Future.failed(new Exception("failed")))

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe 200
      document.select("#global-breadcrumb li:nth-child(1) a").toString should include("/account\">")
      document.select("#global-breadcrumb li:nth-child(1) a").text should include("Home")

      document.select("#global-breadcrumb li:nth-child(2) a").toString should include("<a href=\"/annual-tax-summary\">")
      document.select("#global-breadcrumb li:nth-child(2) a").text shouldBe "Select the tax year"

      document.select("#global-breadcrumb li:nth-child(3)").toString should include("<strong>Technical Difficulties</strong>")
    }
  }
}
