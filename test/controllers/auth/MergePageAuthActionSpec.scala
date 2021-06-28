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

package controllers.auth

import config.ApplicationConfig
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.SEE_OTHER
import play.api.mvc.{Action, AnyContent, InjectedController}
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, _}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.domain.SaUtrGenerator
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.test.UnitSpec
import utils.RetrievalOps._
import utils.TestConstants._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class MergePageAuthActionSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar {

  implicit lazy val appConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val ec = app.injector.instanceOf[ExecutionContext]

  class Harness(authAction: MergePageAuthAction) extends InjectedController {
    def onPageLoad(): Action[AnyContent] = authAction { request =>
      Ok(
        s"SaUtr: ${request.saUtr.map(_.utr).getOrElse("fail")}," +
          s"AgentRef: ${request.agentRef.map(_.uar).getOrElse("fail")}" +
          s"isSa: ${request.isSa}" +
          s"credentials: ${request.credentials.providerType}")
    }
  }
  val fakeCredentials = Credentials("foo", "bar")
  val mockAuthConnector: DefaultAuthConnector = mock[DefaultAuthConnector]

  val ggSignInUrl =
    "http://localhost:9553/bas-gateway/sign-in?continue_url=http%3A%2F%2Flocalhost%3A9217%2Fannual-tax-summary&origin=tax-summaries-frontend"
  implicit val timeout: FiniteDuration = 5 seconds

  "A user with no active session" should {
    "return 303 and be redirected to GG sign in page" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(new SessionRecordNotFound))
      val authAction = new MergePageAuthActionImpl(mockAuthConnector, new FakeMergePageAuthAction(true).mcc)
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
      val authAction = new MergePageAuthActionImpl(mockAuthConnector, new FakeMergePageAuthAction(true).mcc)
      val controller = new Harness(authAction)
      val result = controller.onPageLoad()(FakeRequest("", ""))

      redirectLocation(result) shouldBe Some("/annual-tax-summary/not-authorised")
    }
  }

  "A user with a confidence level 50 and an SA enrolment" should {
    "create an authenticated request" in {
      val utr = new SaUtrGenerator().nextSaUtr.utr
      val retrievalResult: Future[
        Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[String] ~ ConfidenceLevel] =
        Future.successful(
          Enrolments(Set(Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", utr)), "Activated"))) ~ Some("") ~ Some(
            fakeCredentials) ~ Some(utr) ~ None ~ ConfidenceLevel.L50
        )

      when(
        mockAuthConnector
          .authorise[
            Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[String] ~ ConfidenceLevel](
            any(),
            any())(any(), any()))
        .thenReturn(retrievalResult)

      val authAction = new MergePageAuthActionImpl(mockAuthConnector, new FakeMergePageAuthAction(true).mcc)
      val controller = new Harness(authAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) shouldBe OK
      contentAsString(result) should include(utr)
      contentAsString(result) should include("true")
    }
  }

  "A user with a confidence level 50 and an IR-SA-AGENT enrolment" should {
    "create an authenticated request" in {
      val uar = testUar

      val retrievalResult: Future[
        Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[String] ~ ConfidenceLevel] =
        Future.successful(
          Enrolments(Set(Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", uar)), ""))) ~
            Some("") ~ Some(fakeCredentials) ~ Some(testUtr) ~ None ~ ConfidenceLevel.L50)

      when(
        mockAuthConnector
          .authorise[
            Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[String] ~ ConfidenceLevel](
            any(),
            any())(any(), any()))
        .thenReturn(retrievalResult)

      val authAction = new MergePageAuthActionImpl(mockAuthConnector, new FakeMergePageAuthAction(true).mcc)
      val controller = new Harness(authAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) shouldBe OK
      contentAsString(result) should include(uar)
      contentAsString(result) should include(testUtr)
      contentAsString(result) should include("bar")
    }
  }

  "A user with a confidence level 50 and an IR-SA-AGENT enrolment, no nino and no utr" should {
    "create an authenticated request" in {
      val uar = testUar

      val retrievalResult: Future[
        Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[String] ~ ConfidenceLevel] =
        Future.successful(
          Enrolments(Set(Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", uar)), ""))) ~
            Some("") ~ Some(fakeCredentials) ~ None ~ None ~ ConfidenceLevel.L50)

      when(
        mockAuthConnector
          .authorise[
            Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[String] ~ ConfidenceLevel](
            any(),
            any())(any(), any()))
        .thenReturn(retrievalResult)

      val authAction = new MergePageAuthActionImpl(mockAuthConnector, new FakeMergePageAuthAction(true).mcc)
      val controller = new Harness(authAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) shouldBe OK
      contentAsString(result) should include(uar)
      contentAsString(result) should include("false")
      contentAsString(result) should include("bar")
    }
  }

  "A user with a confidence level 50, no nino and no utr" should {
    "see access denied" in {

      val retrievalResult: Future[
        Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[String] ~ ConfidenceLevel] =
        Future.successful(
          Enrolments(Set(Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", testUtr)), "Activated"))) ~
            Some("") ~ Some(fakeCredentials) ~ None ~ None ~ ConfidenceLevel.L50)

      when(
        mockAuthConnector
          .authorise[
            Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[String] ~ ConfidenceLevel](
            any(),
            any())(any(), any()))
        .thenReturn(retrievalResult)

      val authAction = new MergePageAuthActionImpl(mockAuthConnector, new FakeMergePageAuthAction(true).mcc)
      val controller = new Harness(authAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/annual-tax-summary/not-authorised")
    }
  }

  "A user with no credentials will fail to auth" in {
    val uar = testUar

    val retrievalResult
      : Future[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[String] ~ ConfidenceLevel] =
      Future.successful(
        Enrolments(Set(Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", uar)), ""))) ~
          Some("") ~ None ~ Some(uar) ~ None ~ ConfidenceLevel.L50
      )

    when(mockAuthConnector
      .authorise[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[String] ~ ConfidenceLevel](
        any(),
        any())(any(), any()))
      .thenReturn(retrievalResult)

    val authAction = new MergePageAuthActionImpl(mockAuthConnector, new FakeMergePageAuthAction(true).mcc)
    val controller = new Harness(authAction)

    val ex = intercept[RuntimeException] {
      await(controller.onPageLoad()(FakeRequest("", "")))
    }

    ex.getMessage should include("Can't find credentials for user")

  }

}