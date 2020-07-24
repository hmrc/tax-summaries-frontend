/*
 * Copyright 2020 HM Revenue & Customs
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
import controllers.auth.{AuthenticatedRequest, FakeAuthAction}
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
import view_models.{AtsList, NoATSViewModel, TaxYearEnd}
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

class IndexControllerSpec extends ControllerBaseSpec with ScalaFutures with BeforeAndAfterEach {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))
  val taxYear = 2015

  val request = AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest("Get", s"?taxYear=$taxYear"))
  val agentRequest = AuthenticatedRequest("userId", Some(Uar(testUar)), Some(SaUtr(testUtr)), None, None, None, None, FakeRequest("Get", s"?taxYear=$taxYear"))

  val data = {
    val source = Source.fromURL(getClass.getResource("/test_list_utr.json")).mkString
    val json = Json.parse(source)
    Json.fromJson[AtsListData](json).get
  }

  val mockDataCacheConnector = mock[DataCacheConnector]
  val mockAtsYearListService = mock[AtsYearListService]
  val mockAtsListService = mock[AtsListService]

  def sut = new IndexController(
    mockDataCacheConnector,
    mockAtsYearListService,
    mockAtsListService,
    mock[AuditService],
    FakeAuthAction,
    mcc
  )

  val model: GenericViewModel = AtsList(
    utr = testUtr,
    forename = "forename",
    surname = "surname",
    yearList = List(
      TaxYearEnd(Some("2014")),
      TaxYearEnd(Some("2015"))
    )
  )

  override def beforeEach(): Unit = {
    when(mockAtsYearListService.getAtsListData(any[HeaderCarrier], any[AuthenticatedRequest[_]])).thenReturn(model)
    when(mockDataCacheConnector.storeAgentToken(any[String])(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(None))
  }

  "Calling with request param" should {

    "return a 303 response when called with '?ref=PORTAL'" in {

      val requestWithQuery = AuthenticatedRequest(
        "userId",
        None,
        Some(SaUtr(testUtr)),
        None,
        None,
        None,
        None,
        FakeRequest("GET", controllers.routes.IndexController.authorisedIndex + "?ref=PORTAL")
      )

      val result = Future.successful(sut.agentAwareShow(requestWithQuery))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/annual-tax-summary")
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
        None,
        None,
        None,
        FakeRequest("GET", controllers.routes.IndexController.authorisedIndex + "?ref=PORTAL")
      )

      val result = Future.successful(sut.agentAwareShow(requestWithQuery))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/annual-tax-summary")
      session(result).get("TAXS_USER_TYPE") shouldBe Some("PORTAL")
      verify(mockDataCacheConnector, never()).storeAgentToken(any[String])(any[HeaderCarrier], any[ExecutionContext])
    }

    "put TAXS_USER_TYPE into session and Agent token into dataCache when called with '/?ref=PORTAL&id=bxk2Z3Q84R0W2XSklMb7Kg'" in {

      val requestWithQuery = AuthenticatedRequest(
        "userId",
        None,
        Some(SaUtr(testUtr)),
        None,
        None,
        None,
        None,
        FakeRequest("GET", controllers.routes.IndexController.authorisedIndex + "/?ref=PORTAL&id=bxk2Z3Q84R0W2XSklMb7Kg")
      )

      val result = Future.successful(sut.agentAwareShow(requestWithQuery))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/annual-tax-summary")
      session(result).get("TAXS_USER_TYPE") shouldBe Some("PORTAL")
      verify(mockDataCacheConnector, never()).storeAgentToken(any[String])(any[HeaderCarrier], any[ExecutionContext])
    }

    "not put TAXS_USER_TYPE or TAXS_AGENT_TOKEN into session when called only with '/?id=bxk2Z3Q84R0W2XSklMb7Kg'" in {

      val requestWithQuery = AuthenticatedRequest(
        "userId",
        None,
        Some(SaUtr(testUtr)),
        None,
        None,
        None,
        None,
        FakeRequest("GET", controllers.routes.IndexController.authorisedIndex + "/?id=bxk2Z3Q84R0W2XSklMb7Kg")
      )

      val result = Future.successful(sut.agentAwareShow(requestWithQuery))

      status(result) shouldBe 200
      session(result).get("TAXS_USER_TYPE") shouldBe None
      verify(mockDataCacheConnector, never()).storeAgentToken(any[String])(any[HeaderCarrier], any[ExecutionContext])
    }

    "go straight to summary if user has only one tax year" in {

      val model2: GenericViewModel = AtsList(
        utr = testUtr,
        forename = "forename",
        surname = "surname",
        yearList = List(
          TaxYearEnd(Some("2014"))
        )
      )

      when(mockAtsYearListService.getAtsListData(any[HeaderCarrier], any[AuthenticatedRequest[_]])).thenReturn(model2)
      when(mockAtsListService.getAtsYearList(any[HeaderCarrier], any[AuthenticatedRequest[_]])).thenReturn(data)

      val result = sut.agentAwareShow(request)

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/annual-tax-summary/main?taxYear=2014")
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
        None,
        None,
        None,
        FakeRequest("GET", controllers.routes.IndexController.authorisedIndex + "/?ref=PORTAL")
      )

      val result = Future.successful(sut.agentAwareShow(agentRequestWithQuery))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/annual-tax-summary")
      session(result).get("TAXS_USER_TYPE") shouldBe Some("PORTAL")
      verify(mockDataCacheConnector, never()).storeAgentToken(any[String])(any[HeaderCarrier], any[ExecutionContext])
    }

    "put TAXS_USER_TYPE and TAXS_AGENT_TOKEN into store when called with '/?ref=PORTAL&id=bxk2Z3Q84R0W2XSklMb7Kg'" in {

      val agentRequestWithQuery = AuthenticatedRequest(
        "userId",
        Some(Uar(testUar)),
        Some(SaUtr(testUtr)),
        None,
        None,
        None,
        None,
        FakeRequest("GET", controllers.routes.IndexController.authorisedIndex + "/?ref=PORTAL&id=bxk2Z3Q84R0W2XSklMb7Kg")
      )

      val result = Future.successful(sut.agentAwareShow(agentRequestWithQuery))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/annual-tax-summary")
      session(result).get("TAXS_USER_TYPE") shouldBe Some("PORTAL")
      verify(mockDataCacheConnector, times(1)).storeAgentToken(Matchers.eq("bxk2Z3Q84R0W2XSklMb7Kg"))(any[HeaderCarrier], any[ExecutionContext])
    }

    "not put TAXS_USER_TYPE or TAXS_AGENT_TOKEN into session when called only with '/?id=bxk2Z3Q84R0W2XSklMb7Kg'" in {

      reset(mockDataCacheConnector)

      val agentRequestWithQuery = AuthenticatedRequest(
        "userId",
        Some(Uar(testUar)),
        Some(SaUtr(testUtr)),
        None,
        None,
        None,
        None,
        FakeRequest("GET", controllers.routes.IndexController.authorisedIndex + "/?id=bxk2Z3Q84R0W2XSklMb7Kg")
      )

      val result = Future.successful(sut.agentAwareShow(agentRequestWithQuery))

      session(result).get("TAXS_USER_TYPE") shouldBe None
      verify(mockDataCacheConnector, never()).storeAgentToken(any[String])(any[HeaderCarrier], any[ExecutionContext])
    }
  }

  "Calling connector for ATS Tax Year list" should {

    "return a 200 response" in {

      val result = Future.successful(sut.agentAwareShow(request))
      status(result) shouldBe 200
    }

    "return a Tax Year list" in {

      val result = Future.successful(sut.agentAwareShow(request))
      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe 200
      document.text() contains "2014"
    }
  }

  "Submitting the Index page" should {

    "give a Ok status and stay on the same page if form errors and display the error" in {

      when(mockAtsListService.getAtsYearList(any[HeaderCarrier], any[AuthenticatedRequest[_]])).thenReturn(data)
      val atsYear = Map("atsYear" -> "")
      val form = atsYearFormMapping.bind(atsYear)
      val requestWithQuery = AuthenticatedRequest(
        "userId",
        None,
        Some(SaUtr(testUtr)),
        None,
        None,
        None,
        None,
        FakeRequest().withFormUrlEncodedBody(form.data.toSeq: _*)
      )

      val result = Future.successful(sut.onSubmit(requestWithQuery))
      status(result) shouldBe OK

    }

    "redirect to the no ATS page when there is no annual tax summary data returned" in {

      when(mockAtsYearListService.getAtsListData(any[HeaderCarrier], any[AuthenticatedRequest[_]])).thenReturn(new NoATSViewModel)

      val result = Future.successful(sut.show(request))
      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe routes.ErrorController.authorisedNoAts().url
    }
  }
}
