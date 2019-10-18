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
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest.MustMatchers._
import org.scalatest.mock.MockitoSugar
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services._
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import utils.AuthorityUtils
import utils.TestConstants._
import view_models.NoATSViewModel

import scala.concurrent.Future

class ATSMainControllerTest extends UnitSpec with FakeTaxsPlayApplication with MockitoSugar {

  val user = User(AuthorityUtils.saAuthority(testOid, testUtr))
  val taxYear  =2014
  val baseModel = SummaryControllerTest.baseModel
  val request = FakeRequest("GET",s"?taxYear=$taxYear")
  val badRequest = FakeRequest("GET","?taxYear=20145")

  trait TestController extends AtsMainController {
    override lazy val summaryService = mock[SummaryService]
    override lazy val auditService = mock[AuditService]
    implicit lazy val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever
    when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.eq(user),Matchers.any(),Matchers.eq(request))).thenReturn(Future.successful(baseModel))

  }

  "Calling Index Page with no session" should {

    "return a 303 response" in new TestController {
      val result = Future.successful(authorisedAtsMain(request))
      status(result) shouldBe 303
    }
  }

  "Calling Index Page with session" should {

    "return a successful response for a valid request" in new TestController {
      val result =  Future.successful(show(user, request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.title should include(Messages("ats.index.html.title")+ Messages("generic.to_from", (taxYear-1).toString, taxYear.toString))
    }

    "display an error page for an invalid request" in new TestController {
      val result = Future.successful(show(user, badRequest))
      status(result) shouldBe 400
      val document = Jsoup.parse(contentAsString(result))
      document.title should include(Messages("generic.error.html.title"))
    }

    "redirect to the no ATS page when there is no annual tax summary data returned" in new TestController {

      when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.eq(user),Matchers.any(),Matchers.eq(request))).thenReturn(Future.successful(new NoATSViewModel))

      val result = Future.successful(show(user, request))
      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe routes.ErrorController.authorisedNoAts().url

    }

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

      val model = baseModel.copy(
        year = 2015
      )

      when(summaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.eq(user),Matchers.any(),Matchers.eq(request))).thenReturn(Future.successful(model))

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe 200
      document.getElementById("index-page-header").text should include("2015")
    }

    "show 'Landing page' page with a correct breadcrumb" in new TestController {

      val result = Future.successful(show(user, request))
      val document = Jsoup.parse(contentAsString(result))

      document.select("#global-breadcrumb li:nth-child(1) a").attr("href") should include("/account")
      document.select("#global-breadcrumb li:nth-child(1) a").text should include("Home")

      document.select("#global-breadcrumb li:nth-child(2)").toString should include("Select the tax year")
    }
  }
}
