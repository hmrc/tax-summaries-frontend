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
import play.api.test.Helpers._
import play.api.test.FakeRequest
import services._
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import utils.AuthorityUtils
import scala.concurrent.Future
import utils.TestConstants._

class ATSMainControllerTest extends UnitSpec with FakeTaxsPlayApplication with MockitoSugar {

  val user = User(AuthorityUtils.saAuthority(testOid, testUtr))
  val request = FakeRequest()

  val baseModel = SummaryControllerTest.baseModel

  trait TestController extends AtsMainController {

    override lazy val summaryService = mock[SummaryService]
    override lazy val auditService = mock[AuditService]

    val model = baseModel

    when(summaryService.getSummaryData(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)
  }

  "Calling Index Page with no session" should {

    "return a 303 response" in new TestController {

      val result = Future.successful(authorisedAtsMain(request))
      status(result) shouldBe 303
    }
  }


  "Calling Index Page with session" should {

    "have the right user data in the view" in new TestController {

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe 200
      document.getElementById("tax-calc-link").text shouldBe "Your income and taxes"
      document.getElementById("tax-services-link").text shouldBe "Your taxes and public spending"
      document.getElementById("index-page-header").text shouldBe "Tax year: April 6 2013 to April 5 2014 Your annual tax summary"
      document.getElementById("index-page-description").text shouldBe "This summarises your personal tax and National Insurance, and how they are spent by government. This information comes from you, your employer(s) or your pension provider(s)."
      document.getElementById("tax-calc-link").tagName shouldBe "a"
      document.getElementById("tax-services-link").tagName shouldBe "a"
      document.getElementById("user-info").text should include("forename surname")
      document.getElementById("user-info").text should include("Unique Taxpayer Reference: "+testUtr)
    }

    "display the right years" in new TestController {

      override val model = baseModel.copy(
        year = 2015
      )

      when(summaryService.getSummaryData(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe 200
      document.getElementById("index-page-header").text should include("2015")
    }

    "show 'Landing page' page with a correct breadcrumb" in new TestController {

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.select("#global-breadcrumb li:nth-child(1) a").toString should include("/account\">")
      document.select("#global-breadcrumb li:nth-child(1) a").text should include("Home")

      document.select("#global-breadcrumb li:nth-child(2)").toString should include("Select the tax year")
    }
  }
}
