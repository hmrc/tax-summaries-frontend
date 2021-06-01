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

import config.ApplicationConfig
import controllers.auth.{AuthenticatedRequest, FakeMergePageAuthAction}
import models.{AtsType, AtsYearChoice}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}
import services.AtsMergePageService
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HttpResponse
import utils.TestConstants.{testNino, testUtr}
import view_models.AtsForms.atsYearFormMapping
import view_models.{AtsList, AtsMergePageViewModel}

import scala.concurrent.Future

class AtsMergePageControllerSpec extends ControllerBaseSpec with ScalaFutures with BeforeAndAfterEach {
  val mockAtsMergePageService = mock[AtsMergePageService]

  override implicit lazy val appConfig = mock[ApplicationConfig]
  val sut = new AtsMergePageController(
    mockAtsMergePageService,
    FakeMergePageAuthAction,
    mcc,
    atsMergePageView,
    genericErrorView)

  lazy implicit val authRequest = AuthenticatedRequest(
    "userId",
    None,
    Some(SaUtr(testUtr)),
    Some(testNino),
    true,
    ConfidenceLevel.L50,
    fakeCredentials,
    FakeRequest("GET", s"?taxYear=$taxYear"))

  val successViewModel = AtsMergePageViewModel(AtsList("", "", "", List(2019)), List(2018), appConfig)(request)

  override def beforeEach() =
    reset(appConfig)

  when(appConfig.saShuttered).thenReturn(false)
  when(appConfig.payeShuttered).thenReturn(false)

  "AtsMergePageController for onPageLoad" should {

    "return a 200 response when called without '?ref=PORTAL' and should not put TAXS_USER_TYPE in session" in {

      when(mockAtsMergePageService.getSaAndPayeYearList(any(), any())).thenReturn(Right(successViewModel))

      val result = Future.successful(sut.onPageLoad(authRequest))

      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.text() contains "Select the tax year"
      document.text() contains "2018"

      session(result).get("TAXS_USER_TYPE") shouldBe None
    }

    "return a 200 response when called with '?ref=PORTAL' and put TAXS_USER_TYPE in session" in {
      val agentRequest = AuthenticatedRequest(
        "userId",
        None,
        Some(SaUtr(testUtr)),
        None,
        true,
        ConfidenceLevel.L50,
        fakeCredentials,
        FakeRequest("GET", controllers.routes.AtsMergePageController.onPageLoad + "?ref=PORTAL")
      )

      when(mockAtsMergePageService.getSaAndPayeYearList(any(), any())).thenReturn(Right(successViewModel))

      val result = sut.onPageLoad(agentRequest)

      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.text() contains "Select the tax year"
      document.text() contains "2018"

      session(result).get("TAXS_USER_TYPE") shouldBe Some("PORTAL")

    }

    "redirect to serviceUnavailable page if sa and paye are shuttered" in {

      when(appConfig.saShuttered).thenReturn(true)
      when(appConfig.payeShuttered).thenReturn(true)

      when(mockAtsMergePageService.getSaAndPayeYearList(any(), any())).thenReturn(Right(successViewModel))

      val result = sut.onPageLoad(authRequest)

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe (controllers.routes.ErrorController.serviceUnavailable().url)
    }

    "redirect to genericErrorView page if service returns an error" in {

      when(mockAtsMergePageService.getSaAndPayeYearList(any(), any())).thenReturn(Left(HttpResponse(BAD_GATEWAY)))

      val result = sut.onPageLoad(authRequest)

      status(result) shouldBe 500
      val document = contentAsString(result)
      document shouldBe contentAsString(genericErrorView())
    }

  }

  "AtsMergePageController for onSubmit" should {

    "return a success response and redirect to sa main page when selected sa tax year" in {

      when(mockAtsMergePageService.getSaAndPayeYearList(any(), any())).thenReturn(Right(successViewModel))

      val form = atsYearFormMapping.bind(Map("year" -> "{\"atsType\":\"SA\",\"year\":2019}"))
      val requestWithQuery = AuthenticatedRequest(
        "userId",
        None,
        Some(SaUtr(testUtr)),
        Some(testNino),
        true,
        ConfidenceLevel.L50,
        fakeCredentials,
        FakeRequest().withFormUrlEncodedBody(form.data.toSeq: _*))

      val result = sut.onSubmit(requestWithQuery)

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe (routes.AtsMainController
        .authorisedAtsMain()
        .url + "?taxYear=2019").toString

    }

    "return a success response and redirect to paye main page when selected paye tax year" in {

      when(mockAtsMergePageService.getSaAndPayeYearList(any(), any())).thenReturn(Right(successViewModel))

      val form = atsYearFormMapping.bind(Map("year" -> "{\"atsType\":\"PAYE\",\"year\":2019}"))
      val requestWithQuery = AuthenticatedRequest(
        "userId",
        None,
        Some(SaUtr(testUtr)),
        Some(testNino),
        true,
        ConfidenceLevel.L50,
        fakeCredentials,
        FakeRequest().withFormUrlEncodedBody(form.data.toSeq: _*))

      val result = sut.onSubmit(requestWithQuery)

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe (paye.routes.PayeAtsMainController.show(2019)).toString

    }

    "return a success response and redirect to no ats page when selected no ats tax year" in {

      when(mockAtsMergePageService.getSaAndPayeYearList(any(), any())).thenReturn(Right(successViewModel))

      val form = atsYearFormMapping.bind(Map("year" -> "{\"atsType\":\"NoATS\",\"year\":2019}"))
      val requestWithQuery = AuthenticatedRequest(
        "userId",
        None,
        Some(SaUtr(testUtr)),
        Some(testNino),
        true,
        ConfidenceLevel.L50,
        fakeCredentials,
        FakeRequest().withFormUrlEncodedBody(form.data.toSeq: _*))

      val result = sut.onSubmit(requestWithQuery)

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe (controllers.routes.ErrorController.authorisedNoAts()).toString

    }

    "return a success response and stay on the same page with form errors and display the error" in {

      when(mockAtsMergePageService.getSaAndPayeYearList(any(), any())).thenReturn(Right(successViewModel))

      val form = atsYearFormMapping.bind(Map("year" -> ""))
      val requestWithQuery = AuthenticatedRequest(
        "userId",
        None,
        Some(SaUtr(testUtr)),
        Some(testNino),
        true,
        ConfidenceLevel.L50,
        fakeCredentials,
        FakeRequest().withFormUrlEncodedBody(form.data.toSeq: _*))

      val result = sut.onSubmit(requestWithQuery)

      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.text() contains "There is a problem with the form"
      document.text() contains "Select an option for the tax year"
    }
  }

}
