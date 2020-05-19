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

import config.AppFormPartialRetriever
import connectors.DataCacheConnector
import controllers.auth.{AuthAction, AuthenticatedRequest, FakeAuthAction}
import models.AtsListData
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito.{when, _}
import org.scalatest.MustMatchers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services._
import uk.gov.hmrc.domain.{SaUtr, Uar}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import utils.GenericViewModel
import utils.TestConstants._
import view_models.AtsForms._
import view_models.{AtsList, NoATSViewModel, TaxYearEnd}

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

class IndexControllerSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar with ScalaFutures with I18nSupport {

  override def messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))
  val taxYear = 2015

  val request = AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest("Get", s"?taxYear=$taxYear"))
  val agentRequest = AuthenticatedRequest("userId", Some(Uar(testUar)), Some(SaUtr(testUtr)), None, None, None, None, FakeRequest("Get", s"?taxYear=$taxYear"))

  val data = {
    val source = Source.fromURL(getClass.getResource("/test_list_utr.json")).mkString
    val json = Json.parse(source)
    Json.fromJson[AtsListData](json).get
  }

  trait TestController extends IndexController {

    override lazy val dataCache = mock[DataCacheConnector]
    override lazy val atsYearListService = mock[AtsYearListService]
    override val auditService = mock[AuditService]
    override lazy val atsListService = mock[AtsListService]
    implicit lazy val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever
    override val authAction: AuthAction = FakeAuthAction

    val model: GenericViewModel = AtsList(
      utr = testUtr,
      forename = "forename",
      surname = "surname",
      yearList = List(
        TaxYearEnd(Some("2014")),
        TaxYearEnd(Some("2015"))
      )
    )

    when(atsYearListService.getAtsListData(any[HeaderCarrier], any[AuthenticatedRequest[_]])).thenReturn(model)
    when(dataCache.storeAgentToken(any[String])(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(None))
  }

  "Calling with request param" should {

    "return a 303 response when called with '?ref=PORTAL'" in new TestController {

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

      val result = Future.successful(agentAwareShow(requestWithQuery))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/annual-tax-summary")
      session(result).get("TAXS_USER_TYPE") shouldBe Some("PORTAL")
    }

  }

  "Calling with request param and trailing slash (non-AGENT)" should {

    "put TAXS_USER_TYPE 'PORTAL' into session when called with '/?ref=PORTAL'" in new TestController {

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

      val result = Future.successful(agentAwareShow(requestWithQuery))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/annual-tax-summary")
      session(result).get("TAXS_USER_TYPE") shouldBe Some("PORTAL")
      verify(dataCache, never()).storeAgentToken(any[String])(any[HeaderCarrier], any[ExecutionContext])
    }

    "put TAXS_USER_TYPE into session and Agent token into dataCache when called with '/?ref=PORTAL&id=bxk2Z3Q84R0W2XSklMb7Kg'" in new TestController {

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

      val result = Future.successful(agentAwareShow(requestWithQuery))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/annual-tax-summary")
      session(result).get("TAXS_USER_TYPE") shouldBe Some("PORTAL")
      verify(dataCache, never()).storeAgentToken(any[String])(any[HeaderCarrier], any[ExecutionContext])
    }

    "not put TAXS_USER_TYPE or TAXS_AGENT_TOKEN into session when called only with '/?id=bxk2Z3Q84R0W2XSklMb7Kg'" in new TestController {

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

      val result = Future.successful(agentAwareShow(requestWithQuery))

      status(result) shouldBe 200
      session(result).get("TAXS_USER_TYPE") shouldBe None
      verify(dataCache, never()).storeAgentToken(any[String])(any[HeaderCarrier], any[ExecutionContext])
    }

    "go straight to summary if user has only one tax year" in new TestController {

      val model2: GenericViewModel = AtsList(
        utr = testUtr,
        forename = "forename",
        surname = "surname",
        yearList = List(
          TaxYearEnd(Some("2014"))
        )
      )

      when(atsYearListService.getAtsListData(any[HeaderCarrier], any[AuthenticatedRequest[_]])).thenReturn(model2)
      when(atsListService.getAtsYearList(any[HeaderCarrier], any[AuthenticatedRequest[_]])).thenReturn(data)

      val result = agentAwareShow(request)

      whenReady(result) { result =>
        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("/annual-tax-summary/main?taxYear=2014")
      }
    }
  }

  //FIXME - should throw an error, if Agent does not provide ref/id
  "Calling with request param and trailing slash (AGENT)" should {

    "put TAXS_USER_TYPE 'PORTAL' into session when called with '/?ref=PORTAL'" in new TestController {

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

      val result = Future.successful(agentAwareShow(agentRequestWithQuery))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/annual-tax-summary")
      session(result).get("TAXS_USER_TYPE") shouldBe Some("PORTAL")
      verify(dataCache, never()).storeAgentToken(any[String])(any[HeaderCarrier], any[ExecutionContext])
    }

    "put TAXS_USER_TYPE and TAXS_AGENT_TOKEN into store when called with '/?ref=PORTAL&id=bxk2Z3Q84R0W2XSklMb7Kg'" in new TestController {

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

      val result = Future.successful(agentAwareShow(agentRequestWithQuery))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/annual-tax-summary")
      session(result).get("TAXS_USER_TYPE") shouldBe Some("PORTAL")
      verify(dataCache, times(1)).storeAgentToken(Matchers.eq("bxk2Z3Q84R0W2XSklMb7Kg"))(any[HeaderCarrier], any[ExecutionContext])
    }

    "not put TAXS_USER_TYPE or TAXS_AGENT_TOKEN into session when called only with '/?id=bxk2Z3Q84R0W2XSklMb7Kg'" in new TestController {

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

      val result = Future.successful(agentAwareShow(agentRequestWithQuery))

      session(result).get("TAXS_USER_TYPE") shouldBe None
      verify(dataCache, never()).storeAgentToken(any[String])(any[HeaderCarrier], any[ExecutionContext])
    }
  }

  "Calling connector for ATS Tax Year list" should {

    "return a 200 response" in new TestController {

      val result = Future.successful(agentAwareShow(request))
      status(result) shouldBe 200
    }

    "return a Tax Year list" in new TestController {

      val result = Future.successful(agentAwareShow(request))
      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe 200
      document.text() contains "2014"
    }
  }

  "Submitting the Index page" should {

    "give a Ok status and stay on the same page if form errors and display the error" in new TestController {

      when(atsListService.getAtsYearList(any[HeaderCarrier], any[AuthenticatedRequest[_]])).thenReturn(data)
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

      val result = Future.successful(onSubmit(requestWithQuery))
      status(result) shouldBe OK

    }

    "redirect to the no ATS page when there is no annual tax summary data returned" in new TestController {

      when(atsYearListService.getAtsListData(any[HeaderCarrier], any[AuthenticatedRequest[_]])).thenReturn(new NoATSViewModel)

      val result = Future.successful(show(request))
      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe routes.ErrorController.authorisedNoAts().url
    }
  }
}
