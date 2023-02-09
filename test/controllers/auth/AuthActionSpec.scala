/*
 * Copyright 2023 HM Revenue & Customs
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

import controllers.routes
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.http.Status.SEE_OTHER
import play.api.mvc.{Action, AnyContent, InjectedController}
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, _}
import services.MessageFrontendService
import uk.gov.hmrc.auth.core.ConfidenceLevel.L50
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.domain.SaUtrGenerator
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import utils.BaseSpec
import utils.RetrievalOps._
import utils.TestConstants._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class AuthActionSpec extends BaseSpec {

  class Harness(authAction: AuthAction) extends InjectedController {
    def onPageLoad(): Action[AnyContent] = authAction { request =>
      Ok(
        s"SaUtr: ${request.saUtr.map(_.utr).getOrElse("fail")}," +
          s"AgentRef: ${request.agentRef.map(_.uar).getOrElse("fail")}" +
          s"isSa: ${request.isSa}" +
          s"credentials: ${request.credentials.providerType}"
      )
    }
  }
  val fakeCredentials                                    = Credentials("foo", "bar")
  val mockAuthConnector: DefaultAuthConnector            = mock[DefaultAuthConnector]
  val mockMessageFrontendService: MessageFrontendService = mock[MessageFrontendService]

  val ggSignInUrl                      =
    "http://localhost:9553/bas-gateway/sign-in?continue_url=http%3A%2F%2Flocalhost%3A9217%2Fannual-tax-summary&origin=tax-summaries-frontend"
  implicit val timeout: FiniteDuration = 5 seconds

  "A user with no active session" must {
    "return 303 and be redirected to GG sign in page" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(new SessionRecordNotFound))
      when(mockMessageFrontendService.getUnreadMessageCount(any())).thenReturn(Future.successful(None))

      val authAction = new AuthActionImpl(mockAuthConnector, FakeAuthAction.mcc, mockMessageFrontendService)
      val controller = new Harness(authAction)
      val result     = controller.onPageLoad()(FakeRequest("", ""))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get must endWith(ggSignInUrl)
    }
  }

  "A user with insufficient enrolments" must {
    "be redirected to the Insufficient Enrolments Page" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(InsufficientEnrolments()))
      when(mockMessageFrontendService.getUnreadMessageCount(any())).thenReturn(Future.successful(None))

      val authAction = new AuthActionImpl(mockAuthConnector, FakeAuthAction.mcc, mockMessageFrontendService)
      val controller = new Harness(authAction)
      val result     = controller.onPageLoad()(FakeRequest("", ""))

      redirectLocation(result) mustBe Some("/annual-tax-summary/not-authorised")
    }
  }

  "A user with a confidence level 50 and an SA enrolment and an IR-SA-AGENT enrolment" must {
    "create an authenticated request" in {
      val utr = new SaUtrGenerator().nextSaUtr.utr
      val uar = testUar

      val retrievalResult
        : Future[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ ConfidenceLevel] =
        Future.successful(
          Enrolments(
            Set(
              Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", uar)), ""),
              Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", utr)), "")
            )
          ) ~ Some("") ~ Some(fakeCredentials) ~ Some(utr) ~ L50
        )

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ ConfidenceLevel](
            any(),
            any()
          )(any(), any())
      ).thenReturn(retrievalResult)
      when(mockMessageFrontendService.getUnreadMessageCount(any())).thenReturn(Future.successful(None))

      val authAction = new AuthActionImpl(mockAuthConnector, FakeAuthAction.mcc, mockMessageFrontendService)
      val controller = new Harness(authAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) mustBe OK
      contentAsString(result) must include(utr)
      contentAsString(result) must include(uar)
      contentAsString(result) must include("true")
      contentAsString(result) must include("bar")
    }
  }

  "A user with a confidence level 50 and an SA enrolment" must {
    "create an authenticated request" in {
      val utr                                                                                          = new SaUtrGenerator().nextSaUtr.utr
      val retrievalResult
        : Future[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ ConfidenceLevel] =
        Future.successful(
          Enrolments(Set(Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", utr)), "Activated"))) ~ Some("") ~ Some(
            fakeCredentials
          ) ~ Some(utr) ~ ConfidenceLevel.L50
        )

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ ConfidenceLevel](
            any(),
            any()
          )(any(), any())
      )
        .thenReturn(retrievalResult)
      when(mockMessageFrontendService.getUnreadMessageCount(any())).thenReturn(Future.successful(None))

      val authAction = new AuthActionImpl(mockAuthConnector, FakeAuthAction.mcc, mockMessageFrontendService)
      val controller = new Harness(authAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) mustBe OK
      contentAsString(result) must include(utr)
      contentAsString(result) must include("true")
    }
  }

  "A user with a confidence level 50 and an active IR-SA-AGENT enrolment" must {
    "create an authenticated request" in {
      val uar = testUar

      val retrievalResult
        : Future[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ ConfidenceLevel] =
        Future.successful(
          Enrolments(Set(Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", uar)), "Activated"))) ~
            Some("") ~ Some(fakeCredentials) ~ None ~ ConfidenceLevel.L50
        )

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ ConfidenceLevel](
            any(),
            any()
          )(any(), any())
      )
        .thenReturn(retrievalResult)
      when(mockMessageFrontendService.getUnreadMessageCount(any())).thenReturn(Future.successful(None))

      val authAction = new AuthActionImpl(mockAuthConnector, FakeAuthAction.mcc, mockMessageFrontendService)
      val controller = new Harness(authAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) mustBe OK
      contentAsString(result) must include(uar)
      contentAsString(result) must include("false")
      contentAsString(result) must include("bar")
    }
  }

  "A user with a confidence level 50 and an inactive IR-SA-AGENT enrolment" must {
    "create an authenticated request" in {
      val uar = testUar

      val retrievalResult
        : Future[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ ConfidenceLevel] =
        Future.successful(
          Enrolments(Set(Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", uar)), ""))) ~
            Some("") ~ Some(fakeCredentials) ~ None ~ L50
        )

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ ConfidenceLevel](
            any(),
            any()
          )(any(), any())
      )
        .thenReturn(retrievalResult)
      when(mockMessageFrontendService.getUnreadMessageCount(any())).thenReturn(Future.successful(None))

      val authAction = new AuthActionImpl(mockAuthConnector, FakeAuthAction.mcc, mockMessageFrontendService)
      val controller = new Harness(authAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) mustBe OK
      contentAsString(result) must include(uar)
      contentAsString(result) must include("false")
      contentAsString(result) must include("bar")
    }
  }

  "A user with a confidence level 50 and neither SA enrolment" must {
    "create an authenticated request" in {
      val retrievalResult
        : Future[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ ConfidenceLevel] =
        Future.successful(
          Enrolments(Set.empty) ~
            Some("") ~ Some(fakeCredentials) ~ None ~ L50
        )

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ ConfidenceLevel](
            any(),
            any()
          )(any(), any())
      )
        .thenReturn(retrievalResult)
      when(mockMessageFrontendService.getUnreadMessageCount(any())).thenReturn(Future.successful(None))

      val authAction = new AuthActionImpl(mockAuthConnector, FakeAuthAction.mcc, mockMessageFrontendService)
      val controller = new Harness(authAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) mustBe OK
      contentAsString(result) must include("false")
      contentAsString(result) must include("bar")
    }
  }

  "A user with no credentials will fail to auth" in {
    val uar = testUar

    val retrievalResult: Future[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ ConfidenceLevel] =
      Future.successful(
        Enrolments(Set(Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", uar)), ""))) ~
          Some("") ~ None ~ Some(uar) ~ ConfidenceLevel.L50
      )

    when(
      mockAuthConnector
        .authorise[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ ConfidenceLevel](any(), any())(
          any(),
          any()
        )
    )
      .thenReturn(retrievalResult)
    when(mockMessageFrontendService.getUnreadMessageCount(any())).thenReturn(Future.successful(None))

    val authAction = new AuthActionImpl(mockAuthConnector, FakeAuthAction.mcc, mockMessageFrontendService)
    val controller = new Harness(authAction)

    val ex = intercept[RuntimeException] {
      await(controller.onPageLoad()(FakeRequest("", "")))
    }

    ex.getMessage must include("Can't find credentials for user")

  }

  "A user visiting the service when it is shuttered" must {
    "be directed to the service unavailable page without calling auth" in {
      reset(mockAuthConnector)

      val authAction = new AuthActionImpl(mockAuthConnector, FakeAuthAction.mcc, mockMessageFrontendService) {
        override val saShuttered: Boolean = true
      }
      val controller = new Harness(authAction)
      val result     = controller.onPageLoad()(FakeRequest())
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe (controllers.routes.ErrorController.serviceUnavailable.url)
      verifyZeroInteractions(mockAuthConnector)
    }
  }
}
