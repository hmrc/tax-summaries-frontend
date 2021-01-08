/*
 * Copyright 2021 HM Revenue & Customs
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
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.MustMatchers._
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services._
import uk.gov.hmrc.domain.SaUtr
import utils.TestConstants._
import view_models.NoATSViewModel

import scala.concurrent.Future

class ATSMainControllerSpec extends ControllerBaseSpec with BeforeAndAfterEach {

  override val taxYear = 2014
  val baseModel = SummaryControllerSpec.baseModel
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

  val mockSummaryService = mock[SummaryService]
  val mockAuditService = mock[AuditService]

  def sut =
    new AtsMainController(
      mockSummaryService,
      mockAuditService,
      FakeAuthAction,
      mcc,
      taxsMainView,
      genericErrorView,
      tokenErrorView)

  override def beforeEach(): Unit =
    when(mockSummaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request))) thenReturn Future
      .successful(baseModel)

  "Calling Index Page" should {

    "return a successful response for a valid request" in {
      val result = Future.successful(sut.show(request))
      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.title should include(
        Messages("ats.index.html.title") + Messages("generic.to_from", (taxYear - 1).toString, taxYear.toString))
    }

    "display an error page for an invalid request" in {
      val result = Future.successful(sut.show(badRequest))
      status(result) shouldBe 400
      val document = Jsoup.parse(contentAsString(result))
      document.title should include(Messages("global.error.InternalServerError500.title"))
    }

    "redirect to the no ATS page when there is no Annual Tax Summary data returned" in {

      when(mockSummaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(new NoATSViewModel))

      val result = Future.successful(sut.show(request))
      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe routes.ErrorController.authorisedNoAts().url

    }

    "have the right user data in the view" in {

      val result = Future.successful(sut.show(request))
      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe 200
      document.getElementById("tax-calc-link").text shouldBe "Your income and taxes"
      document.getElementById("tax-services-link").text shouldBe "Your taxes and public spending"
      document
        .getElementById("index-page-header")
        .text shouldBe "Tax year: April 6 2013 to April 5 2014 Your Annual Tax Summary"
      document
        .getElementById("index-page-description")
        .text shouldBe "This summarises your personal tax and National Insurance, and how they are spent by government."
      document.getElementById("tax-calc-link").tagName shouldBe "a"
      document.getElementById("tax-services-link").tagName shouldBe "a"
      document.getElementById("user-info").text should include("forename surname")
      document.getElementById("user-info").text should include("Unique Taxpayer Reference: " + testUtr)
    }

    "display the right years" in {

      val model = baseModel.copy(
        year = 2015
      )

      when(mockSummaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(model))

      val result = Future.successful(sut.show(request))
      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe 200
      document.getElementById("index-page-header").text should include("2015")
    }

    "show 'Landing page' page with a correct breadcrumb" in {

      val result = Future.successful(sut.show(request))
      val document = Jsoup.parse(contentAsString(result))

      document.select("#global-breadcrumb li:nth-child(1) a").attr("href") should include("/account")
      document.select("#global-breadcrumb li:nth-child(1) a").text should include("Home")

      document.select("#global-breadcrumb li:nth-child(2)").toString should include("Select the tax year")
    }
  }
}
