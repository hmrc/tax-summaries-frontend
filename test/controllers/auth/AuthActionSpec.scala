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

package controllers.auth

import config.WSHttp
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import org.scalatestplus.play.OneAppPerSuite
import play.api.mvc.{Action, AnyContent, Controller}
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolment, EnrolmentIdentifier, Enrolments, InsufficientConfidenceLevel, InsufficientEnrolments, SessionRecordNotFound}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Credentials, LoginTimes, Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import org.mockito.Matchers._
import play.api.test.FakeRequest
import org.scalatest.concurrent.ScalaFutures._
import utils.RetrievalOps._

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.http.Status.SEE_OTHER
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{redirectLocation, status, _}
import uk.gov.hmrc.auth.core.retrieve.v2.TrustedHelper
import uk.gov.hmrc.domain.SaUtrGenerator

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
import utils.TestConstants._

class AuthActionSpec extends UnitSpec with OneAppPerSuite with MockitoSugar {

  class BrokenAuthConnector (exception: Throwable) extends AuthConnector(app.injector.instanceOf[WSHttp]) {
    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
      Future.failed(exception)
  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  class Harness(authAction: AuthAction) extends Controller {
    def onPageLoad(): Action[AnyContent] = authAction { request => Ok(s"SaUtr: ${request.saUtr.map(_.utr).getOrElse("fail").toString}," +
      s"AgentRef: ${request.agentRef.map(_.uar).getOrElse("fail").toString}") }
  }

  val ggSignInUrl = "http://localhost:9025/gg/sign-in?continue=http://localhost:9217/annual-tax-summary&continue=http%3A%2F%2Flocalhost%3A9217%2Fannual-tax-summary&origin=tax-summaries-frontend"
  implicit val timeout: FiniteDuration = 5 seconds

  "A user with no active session" should {
    "return 303 and be redirected to GG sign in page" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(new SessionRecordNotFound))
      val authAction = new AuthActionImpl(mockAuthConnector, app.configuration)
      val controller = new Harness(authAction)
      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get should endWith(ggSignInUrl)
    }
  }

  "A user with insufficient enrolments" should {
    "be redirected to the Insufficient Enrolments Page" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(InsufficientEnrolments()))
      val authAction = new AuthActionImpl(mockAuthConnector, app.configuration)
      val controller = new Harness(authAction)
      val result = controller.onPageLoad()(FakeRequest("", ""))

      redirectLocation(result) shouldBe Some("/annual-tax-summary/not-authorised")
    }
  }

  "A user with a confidence level 50 and an SA enrolment" should {
    "create an authenticated request" in {
      val utr = new SaUtrGenerator().nextSaUtr.utr
      val retrievalResult: Future[
        Enrolments ~ Option[String]] =
        Future.successful(
           Enrolments(Set(Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", utr)), "Activated"))) ~ Some("")
          )

      when(mockAuthConnector
        .authorise[Enrolments ~ Option[String]](any(), any())(any(), any()))
        .thenReturn(retrievalResult)

      val authAction = new AuthActionImpl(mockAuthConnector, app.configuration)
      val controller = new Harness(authAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) shouldBe OK
      contentAsString(result) should include(utr)
    }
  }

  "A user with a confidence level 50 and an IR-SA-AGENT enrolment" should {
    "create an authenticated request" in {
      val uar = testUar
      val retrievalResult: Future[
        Enrolments ~ Option[String]] =
        Future.successful(
          Enrolments(Set(Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", uar)), ""))) ~
            Some("")
          )

      when(mockAuthConnector
        .authorise[Enrolments ~ Option[String]](any(), any())(any(), any()))
        .thenReturn(retrievalResult)

      val authAction = new AuthActionImpl(mockAuthConnector, app.configuration)
      val controller = new Harness(authAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) shouldBe OK
      contentAsString(result) should include(uar)
    }
  }

  "A user visiting the service when it is shuttered" should {
    "be directed to the service unavailable page without calling auth" in {
      reset(mockAuthConnector)
      val shutteredApplication = new GuiceApplicationBuilder()
        .configure(
          "shuttering.sa" -> "true"
        )
        .build()

      val authAction = new AuthActionImpl(mockAuthConnector, shutteredApplication.configuration)
      val controller = new Harness(authAction)
      val result = controller.onPageLoad()(FakeRequest())
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe (controllers.routes.ErrorController.serviceUnavailable().url)
      verifyZeroInteractions(mockAuthConnector)
    }
  }
}
