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
import connectors.DataCacheConnector
import models.{AtsListData, ErrorResponse}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.{AuthContext => User}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestConstants._
import utils.{AuthorityUtils, GenericViewModel}
import view_models.AtsForms._
import view_models.{AtsList, TaxYearEnd}
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

class IndexControllerTest extends UnitSpec with FakeTaxsPlayApplication with MockitoSugar with ScalaFutures {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))
  val taxYear = 2015
  val request = FakeRequest("Get", s"?taxYear=$taxYear")
  val user = User(AuthorityUtils.saAuthority(testOid, testUtr))
  val agentUser = User(AuthorityUtils.taxsAgentAuthority(testOid, testUar))

  val data = {
    val source = Source.fromURL(getClass.getResource("/test_list_utr.json")).mkString
    val json = Json.parse(source)
    Json.fromJson[AtsListData](json).get
  }

  trait TestController extends IndexController {

    override lazy val dataCache = mock[DataCacheConnector]
    override lazy val atsYearListService = mock[AtsYearListService]
    override lazy val auditService = mock[AuditService]
    override lazy val atsListService = mock[AtsListService]
    implicit lazy val formPartialRetriever: FormPartialRetriever = AppFormPartialRetriever

    val model: GenericViewModel = AtsList(
      utr = testUtr,
      forename = "forename",
      surname = "surname",
      yearList = List(
        TaxYearEnd(Some("2014")),
        TaxYearEnd(Some("2015"))
      )
    )

    when(atsYearListService.getAtsListData(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model)
    when(dataCache.storeAgentToken(any[String])(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(None))
  }

  "Calling Index Page with no session" should {

    "return a 303 response" in new TestController {

      val result = Future.successful(authorisedIndex(request))
      status(result) shouldBe 303
    }
  }

  "Calling Index Page submit" should {

    "return a 303 response" in new TestController {

      val result = Future.successful(authorisedOnSubmit(request))
      status(result) shouldBe 303
    }
  }


  "Calling with request param" should {

    "return a 303 response when called with '?ref=PORTAL'" in new TestController {

      val requestWithQuery = FakeRequest("GET", controllers.routes.IndexController.authorisedIndex + "?ref=PORTAL")
      val result = Future.successful(agentAwareShow(user, requestWithQuery))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/annual-tax-summary")
      session(result).get("TAXS_USER_TYPE") shouldBe Some("PORTAL")
    }
  }

  "Calling with request param and trailing slash (non-AGENT)" should {

    "put TAXS_USER_TYPE 'PORTAL' into session when called with '/?ref=PORTAL'" in new TestController {

      val requestWithQuery = FakeRequest("GET", controllers.routes.IndexController.authorisedIndex + "/?ref=PORTAL")
      val result = Future.successful(agentAwareShow(user, requestWithQuery))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/annual-tax-summary")
      session(result).get("TAXS_USER_TYPE") shouldBe Some("PORTAL")
      verify(dataCache, never()).storeAgentToken(any[String])(any[HeaderCarrier], any[ExecutionContext])
    }

    "put TAXS_USER_TYPE into session and Agent token into dataCache when called with '/?ref=PORTAL&id=bxk2Z3Q84R0W2XSklMb7Kg'" in new TestController {

      val requestWithQuery = FakeRequest("GET", controllers.routes.IndexController.authorisedIndex + "/?ref=PORTAL&id=bxk2Z3Q84R0W2XSklMb7Kg")
      val result = Future.successful(agentAwareShow(user, requestWithQuery))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/annual-tax-summary")
      session(result).get("TAXS_USER_TYPE") shouldBe Some("PORTAL")
      verify(dataCache, never()).storeAgentToken(any[String])(any[HeaderCarrier], any[ExecutionContext])
    }

    "not put TAXS_USER_TYPE or TAXS_AGENT_TOKEN into session when called only with '/?id=bxk2Z3Q84R0W2XSklMb7Kg'" in new TestController {

      val requestWithQuery = FakeRequest("GET", controllers.routes.IndexController.authorisedIndex + "/?id=bxk2Z3Q84R0W2XSklMb7Kg")
      val result = Future.successful(agentAwareShow(user, requestWithQuery))

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

      when(atsYearListService.getAtsListData(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(model2)
      when(atsListService.getAtsYearList(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(data)


      val result = agentAwareShow(user, request)

      whenReady(result) { result =>
        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some("/annual-tax-summary/main?taxYear=2014")
      }
    }
  }

  //FIXME - should throw an error, if Agent does not provide ref/id
  "Calling with request param and trailing slash (AGENT)" should {

    "put TAXS_USER_TYPE 'PORTAL' into session when called with '/?ref=PORTAL'" in new TestController {

      val requestWithQuery = FakeRequest("GET", controllers.routes.IndexController.authorisedIndex + "/?ref=PORTAL")
      val result = Future.successful(agentAwareShow(agentUser, requestWithQuery))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/annual-tax-summary")
      session(result).get("TAXS_USER_TYPE") shouldBe Some("PORTAL")
      verify(dataCache, never()).storeAgentToken(any[String])(any[HeaderCarrier], any[ExecutionContext])
    }

    "put TAXS_USER_TYPE and TAXS_AGENT_TOKEN into store when called with '/?ref=PORTAL&id=bxk2Z3Q84R0W2XSklMb7Kg'" in new TestController {

      val requestWithQuery = FakeRequest("GET", controllers.routes.IndexController.authorisedIndex + "/?ref=PORTAL&id=bxk2Z3Q84R0W2XSklMb7Kg")
      val result = Future.successful(agentAwareShow(agentUser, requestWithQuery))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/annual-tax-summary")
      session(result).get("TAXS_USER_TYPE") shouldBe Some("PORTAL")
      verify(dataCache, times(1)).storeAgentToken(Matchers.eq("bxk2Z3Q84R0W2XSklMb7Kg"))(any[HeaderCarrier], any[ExecutionContext])
    }

    "not put TAXS_USER_TYPE or TAXS_AGENT_TOKEN into session when called only with '/?id=bxk2Z3Q84R0W2XSklMb7Kg'" in new TestController {

      val requestWithQuery = FakeRequest("GET", controllers.routes.IndexController.authorisedIndex + "/?id=bxk2Z3Q84R0W2XSklMb7Kg")
      val result = Future.successful(agentAwareShow(agentUser, requestWithQuery))

      session(result).get("TAXS_USER_TYPE") shouldBe None
      verify(dataCache, never()).storeAgentToken(any[String])(any[HeaderCarrier], any[ExecutionContext])
    }
  }

  "Calling connector for ATS Tax Year list" should {

    "return a 200 response" in new TestController {

      val result = Future.successful(agentAwareShow(user, request))
      status(result) shouldBe 200
    }

    "return a Tax Year list" in new TestController {

      val result = Future.successful(agentAwareShow(user, request))
      val document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe 200
      document.text() contains "2014"
    }
  }

  "Submitting the Index page" should {

    "give a Ok status and stay on the same page if form errors and display the error" in new TestController {

      when(atsListService.getAtsYearList(any[User], any[HeaderCarrier], any[Request[AnyRef]])).thenReturn(data)
      val atsYear = Map("atsYear" -> "")
      val form = atsYearFormMapping.bind(atsYear)
      val requestWithQuery = FakeRequest().withFormUrlEncodedBody(form.data.toSeq: _*)
      val result = Future.successful(onSubmit(user, requestWithQuery))
      status(result) shouldBe OK

    }

  }


}
