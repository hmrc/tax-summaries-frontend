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

import connectors.DataCacheConnector
import controllers.auth.{AuthenticatedRequest, FakeAuthAction, FakeMergePageAuthAction}
import models.AtsListData
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito.{when, _}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.MustMatchers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services._
import uk.gov.hmrc.domain.{SaUtr, Uar}
import uk.gov.hmrc.http.HeaderCarrier
import utils.GenericViewModel
import utils.TestConstants._
import view_models.AtsForms._
import view_models.{ATSUnavailableViewModel, AtsList, AtsMergePageViewModel, NoATSViewModel, TaxYearEnd}
import views.html.AtsMergePageView
import uk.gov.hmrc.auth.core.ConfidenceLevel
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

class IndexControllerSpec extends ControllerBaseSpec with ScalaFutures with BeforeAndAfterEach {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))
  override val taxYear = 2015

  implicit val agentRequest = AuthenticatedRequest(
    "userId",
    Some(Uar(testUar)),
    Some(SaUtr(testUtr)),
    None,
    true,
    ConfidenceLevel.L50,
    fakeCredentials,
    FakeRequest("Get", s"?taxYear=$taxYear"))

  val data = {
    val source = Source.fromURL(getClass.getResource("/test_list_utr.json")).mkString
    val json = Json.parse(source)
    Json.fromJson[AtsListData](json).get
  }

  val mockAtsMergePageService = mock[AtsMergePageService]
  val mockAtsMergePageView = inject[AtsMergePageView]
  val successViewModel = AtsMergePageViewModel(AtsList("", "", "", List(2019)), List(2018), appConfig)

  def sut = new AtsMergePageController(
    mockAtsMergePageService,
    FakeMergePageAuthAction,
    mcc,
    mockAtsMergePageView,
    genericErrorView
  )

  val model = AtsList(
    utr = testUtr,
    forename = "forename",
    surname = "surname",
    yearList = List(2014, 2015)
  )

  "Calling with request param" should {

    "return a 200 response when called with '?ref=PORTAL'" in {

      when(mockAtsMergePageService.getSaAndPayeYearList(any(), any())).thenReturn(Right(successViewModel))
      val requestWithQuery = AuthenticatedRequest(
        "userId",
        None,
        Some(SaUtr(testUtr)),
        None,
        true,
        ConfidenceLevel.L50,
        fakeCredentials,
        FakeRequest("GET", controllers.routes.AtsMergePageController.onPageLoad + "?ref=PORTAL")
      )

      val result = Future.successful(sut.onPageLoad(requestWithQuery))

      status(result) shouldBe 200
      session(result).get("TAXS_USER_TYPE") shouldBe Some("PORTAL")
    }

  }

  "Calling with request param and trailing slash (non-AGENT)" should {

    "put TAXS_USER_TYPE 'PORTAL' into session when called with '/?ref=PORTAL'" in {

      val requestWithQuery = AuthenticatedRequest(
        "userId",
        None,
        Some(SaUtr(testUtr)),
        None,
        true,
        ConfidenceLevel.L50,
        fakeCredentials,
        FakeRequest("GET", controllers.routes.AtsMergePageController.onPageLoad + "?ref=PORTAL")
      )

      val result = Future.successful(sut.onPageLoad(requestWithQuery))

      status(result) shouldBe 200
      session(result).get("TAXS_USER_TYPE") shouldBe Some("PORTAL")

    }

    "put TAXS_USER_TYPE into session and Agent token into dataCache when called with '/?ref=PORTAL&id=bxk2Z3Q84R0W2XSklMb7Kg'" in {

      val requestWithQuery = AuthenticatedRequest(
        "userId",
        None,
        Some(SaUtr(testUtr)),
        None,
        true,
        ConfidenceLevel.L50,
        fakeCredentials,
        FakeRequest(
          "GET",
          controllers.routes.AtsMergePageController.onPageLoad + "/?ref=PORTAL&id=bxk2Z3Q84R0W2XSklMb7Kg")
      )

      val result = Future.successful(sut.onPageLoad(requestWithQuery))

      status(result) shouldBe 200
      session(result).get("TAXS_USER_TYPE") shouldBe Some("PORTAL")
    }

    "not put TAXS_USER_TYPE or TAXS_AGENT_TOKEN into session when called only with '/?id=bxk2Z3Q84R0W2XSklMb7Kg'" in {

      val requestWithQuery = AuthenticatedRequest(
        "userId",
        None,
        Some(SaUtr(testUtr)),
        None,
        true,
        ConfidenceLevel.L50,
        fakeCredentials,
        FakeRequest("GET", controllers.routes.AtsMergePageController.onPageLoad + "/?id=bxk2Z3Q84R0W2XSklMb7Kg")
      )

      val result = Future.successful(sut.onPageLoad(requestWithQuery))

      status(result) shouldBe 200
      session(result).get("TAXS_USER_TYPE") shouldBe None

    }

  }

  //FIXME - should throw an error, if Agent does not provide ref/id
  "Calling with request param and trailing slash (AGENT)" should {

    "put TAXS_USER_TYPE 'PORTAL' into session when called with '/?ref=PORTAL'" in {

      val agentRequestWithQuery = AuthenticatedRequest(
        "userId",
        Some(Uar(testUar)),
        Some(SaUtr(testUtr)),
        None,
        true,
        ConfidenceLevel.L50,
        fakeCredentials,
        FakeRequest("GET", controllers.routes.AtsMergePageController.onPageLoad + "/?ref=PORTAL")
      )

      val result = Future.successful(sut.onPageLoad(agentRequestWithQuery))

      status(result) shouldBe 200
      session(result).get("TAXS_USER_TYPE") shouldBe Some("PORTAL")
    }

    "put TAXS_USER_TYPE and TAXS_AGENT_TOKEN into store when called with '/?ref=PORTAL&id=bxk2Z3Q84R0W2XSklMb7Kg'" in {

      val agentRequestWithQuery = AuthenticatedRequest(
        "userId",
        Some(Uar(testUar)),
        Some(SaUtr(testUtr)),
        None,
        true,
        ConfidenceLevel.L50,
        fakeCredentials,
        FakeRequest(
          "GET",
          controllers.routes.AtsMergePageController.onPageLoad + "/?ref=PORTAL&id=bxk2Z3Q84R0W2XSklMb7Kg")
      )

      val result = Future.successful(sut.onPageLoad(agentRequestWithQuery))

      status(result) shouldBe 200
      session(result).get("TAXS_USER_TYPE") shouldBe Some("PORTAL")
    }

    "not put TAXS_USER_TYPE or TAXS_AGENT_TOKEN into session when called only with '/?id=bxk2Z3Q84R0W2XSklMb7Kg'" in {

      val agentRequestWithQuery = AuthenticatedRequest(
        "userId",
        Some(Uar(testUar)),
        Some(SaUtr(testUtr)),
        None,
        true,
        ConfidenceLevel.L50,
        fakeCredentials,
        FakeRequest("GET", controllers.routes.AtsMergePageController.onPageLoad + "/?id=bxk2Z3Q84R0W2XSklMb7Kg")
      )

      val result = Future.successful(sut.onPageLoad(agentRequestWithQuery))

      session(result).get("TAXS_USER_TYPE") shouldBe None
    }
  }

  "Calling connector for ATS Tax Year list" should {

    "return a 200 response" in {

      val result = Future.successful(sut.onPageLoad(request))
      status(result) shouldBe 200
    }

    "return a Tax Year list" in {

      val result = Future.successful(sut.onPageLoad(request))
      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe 200
      document.text() contains "2014"
    }
  }

  "Submitting the Index page" should {

    "give a Ok status and stay on the same page if form errors and display the error" in {

      val atsYear = Map("atsYear" -> "")
      val form = atsYearFormMapping.bind(atsYear)
      val requestWithQuery = AuthenticatedRequest(
        "userId",
        None,
        Some(SaUtr(testUtr)),
        None,
        true,
        ConfidenceLevel.L50,
        fakeCredentials,
        FakeRequest().withFormUrlEncodedBody(form.data.toSeq: _*)
      )

      val result = Future.successful(sut.authorisedOnSubmit(requestWithQuery))
      status(result) shouldBe OK

    }

    "return 200 if service returns 404" in {

      val atsYear = Map("atsYear" -> "")
      val form = atsYearFormMapping.bind(atsYear)
      val requestWithQuery = AuthenticatedRequest(
        "userId",
        None,
        Some(SaUtr(testUtr)),
        None,
        true,
        ConfidenceLevel.L50,
        fakeCredentials,
        FakeRequest().withFormUrlEncodedBody(form.data.toSeq: _*)
      )

      val result = sut.authorisedOnSubmit(requestWithQuery)

      status(result) shouldBe 200
    }

    "redirect to the generic error page when service returns 500" in {

      when(mockAtsMergePageService.getSaAndPayeYearList(any(), any())) thenReturn (Left(INTERNAL_SERVER_ERROR))

      val result = Future.successful(sut.onPageLoad(agentRequest))

      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe INTERNAL_SERVER_ERROR

      document.getElementById("generic-error-page-heading").text() shouldBe "Sorry, the service is unavailable"
    }

    "return 500 if service returns 500" in {

      val atsYear = Map("atsYear" -> "")
      val form = atsYearFormMapping.bind(atsYear)
      val requestWithQuery = AuthenticatedRequest(
        "userId",
        None,
        Some(SaUtr(testUtr)),
        None,
        true,
        ConfidenceLevel.L50,
        fakeCredentials,
        FakeRequest().withFormUrlEncodedBody(form.data.toSeq: _*)
      )
      when(mockAtsMergePageService.getSaAndPayeYearList(any(), any())) thenReturn (Left(INTERNAL_SERVER_ERROR))

      val result = sut.authorisedOnSubmit(requestWithQuery)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }

}
