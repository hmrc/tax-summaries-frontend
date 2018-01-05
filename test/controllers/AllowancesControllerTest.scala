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
import utils.AuthorityUtils
import view_models.{Allowances, Amount}
import scala.concurrent.Future
import utils.TestConstants._
import uk.gov.hmrc.http.HeaderCarrier

class AllowancesControllerTest extends UnitSpec with FakeTaxsPlayApplication with MockitoSugar {

  val request = FakeRequest()
  val user = User(AuthorityUtils.saAuthority(testOid, testUtr))

  val baseModel = Allowances(
    taxYear = 2014,
    utr = testUtr,
    taxFreeAllowance = Amount(9440, "GBP"),
    marriageAllowanceTransferred = Amount(0, "GBP"),
    otherAllowances = Amount(300, "GBP"),
    totalTaxFree = Amount(9740, "GBP"),
    title = "Mr",
    forename = "forename",
    surname = "surname"
  )

  trait TestController extends AllowancesController {
    override lazy val allowanceService = mock[AllowanceService]
    override lazy val auditService = mock[AuditService]

    val model = baseModel

    when(allowanceService.getAllowances(any[User], any[Request[AnyRef]], any[HeaderCarrier])).thenReturn(model)
  }

  "Calling allowances with no session" should {

    "return a 303 response" in new TestController {

      val result = Future.successful(authorisedAllowance(request))
      status(result) shouldBe 303
    }
  }

  "Calling allowances with session" should {

    "have the right user data in the view" in new TestController {

      when(allowanceService.getAllowances(any[User], any[Request[AnyRef]], any[HeaderCarrier])).thenReturn(model)

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("tax-free-total").text() shouldBe "£9,740"
      document.getElementById("tax-free-allowance-amount").text() shouldBe "£9,440"
      document.getElementById("other-allowances").text() shouldBe "£300"
      document.toString should include("tax-free-allowance")
      document.getElementById("user-info").text() should include("forename surname")
      document.getElementById("user-info").text() should include("Unique Taxpayer Reference: "+testUtr)
      document.select(".page-header h1").text shouldBe "Tax year: April 6 2013 to April 5 2014 Your tax-free amount"
    }

    "have zero-value fields hidden in the view" in new TestController {

      override val model = baseModel.copy(
        taxFreeAllowance = Amount(0, "GBP"),
        otherAllowances = Amount(0, "GBP")
      )

      when(allowanceService.getAllowances(any[User], any[Request[AnyRef]], any[HeaderCarrier])).thenReturn(model)

      val result = Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))

      document.toString should not include "tax-free-allowance-amount"
      document.toString should not include "other-allowances"
    }

    "show 'Allowances' page with a correct breadcrumb" in new TestController {

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.select("#global-breadcrumb li:nth-child(1) a").toString should include("/account\">")
      document.select("#global-breadcrumb li:nth-child(1) a").text should include("Home")

      document.select("#global-breadcrumb li:nth-child(2) a").toString should include("<a href=\"/annual-tax-summary\">")
      document.select("#global-breadcrumb li:nth-child(2) a").text shouldBe "Select the tax year"

      document.select("#global-breadcrumb li:nth-child(3) a").toString should include("<a href=\"/annual-tax-summary/main?taxYear=2014\">")
      document.select("#global-breadcrumb li:nth-child(3) a").text shouldBe "Your annual tax summary"

      document.select("#global-breadcrumb li:nth-child(4) a").toString should include("<a href=\"/annual-tax-summary/summary?taxYear=2014\">")
      document.select("#global-breadcrumb li:nth-child(4) a").text shouldBe "Your income and taxes"

      document.select("#global-breadcrumb li:nth-child(5)").toString should include("<strong>Your tax-free amount</strong>")
    }
  }
}
