/*
 * Copyright 2022 HM Revenue & Customs
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
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito.when
import play.api.i18n.Messages
import play.api.test.Helpers._
import services._
import utils.ControllerBaseSpec
import utils.TestConstants._
import view_models.{ATSUnavailableViewModel, NoATSViewModel}

import scala.concurrent.Future

class ATSMainControllerSpec extends ControllerBaseSpec {

  val baseModel = SummaryControllerSpec.baseModel

  val mockSummaryService = mock[SummaryService]
  val mockAuditService = mock[AuditService]

  def sut =
    new AtsMainController(
      mockSummaryService,
      mockAuditService,
      FakeAuthJourney,
      mcc,
      taxsMainView,
      genericErrorView,
      tokenErrorView)

  override def beforeEach(): Unit =
    when(mockSummaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request))) thenReturn Future
      .successful(baseModel)

  "Calling Index Page" must {

    "return a successful response for a valid request" in {
      val result = sut.show(request)
      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.title must include(
        Messages("ats.index.html.title") + Messages("generic.to_from", (taxYear - 1).toString, taxYear.toString))
      contentAsString(result) must include("contact/beta-feedback-unauthenticated")
    }

    "display an error page for an invalid request" in {
      val result = sut.show(badRequest)
      status(result) mustBe 400
      val document = Jsoup.parse(contentAsString(result))
      document.title must include(Messages("global.error.InternalServerError500.title"))
    }

    "display an error page when AtsUnavailableViewModel is returned" in {

      when(mockSummaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(new ATSUnavailableViewModel))

      val result = sut.show(request)
      status(result) mustBe INTERNAL_SERVER_ERROR

      val document = Jsoup.parse(contentAsString(result))
      document.title must include(Messages("global.error.InternalServerError500.title"))
    }

    "redirect to the no ATS page when there is no Annual Tax Summary data returned" in {

      when(mockSummaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(new NoATSViewModel))

      val result = sut.show(request)
      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe routes.ErrorController.authorisedNoAts(appConfig.taxYear).url

    }

    "have the right user data in the view" in {

      val result = sut.show(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe 200
      document.getElementById("tax-calc-link").text mustBe "Your income and taxes"
      document.getElementById("tax-services-link").text mustBe "Your taxes and public spending"
      document
        .select("header[data-component='ats_page_heading']")
        .text mustBe s"Tax year: April 6 ${taxYear - 1} to April 5 $taxYear Self Assessment Annual Tax Summary"
      document
        .getElementById("index-page-description")
        .text mustBe "This summarises your personal tax and National Insurance, and how they are spent by government."
      document.getElementById("tax-calc-link").tagName mustBe "a"
      document.getElementById("tax-services-link").tagName mustBe "a"
      document.getElementById("user-info").text must include("forename surname")
      document.getElementById("user-info").text must include("Unique Taxpayer Reference: " + testUtr)
    }

    "display the right years" in {

      val model = baseModel.copy(
        year = taxYear
      )

      when(mockSummaryService.getSummaryData(Matchers.eq(taxYear))(Matchers.any(), Matchers.eq(request)))
        .thenReturn(Future.successful(model))

      val result = sut.show(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe 200
      document.select("header[data-component='ats_page_heading']").text must include(taxYear.toString)
    }

  }
}
