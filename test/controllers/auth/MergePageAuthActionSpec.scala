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

package controllers.auth

import connectors.DataCacheConnector
import models.{AgentToken, MatchingDetails}
import org.mockito.ArgumentMatchers.any
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.SEE_OTHER
import play.api.mvc.{Action, AnyContent, InjectedController}
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, _}
import services.{CitizenDetailsService, SucccessMatchingDetailsResponse}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.domain.{SaUtr, SaUtrGenerator}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import utils.BaseSpec
import utils.RetrievalOps._
import utils.TestConstants._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class MergePageAuthActionSpec extends BaseSpec with GuiceOneAppPerSuite {

  class Harness(authAction: MergePageAuthAction) extends InjectedController {
    def onPageLoad(): Action[AnyContent] = authAction { request =>
      Ok(
        s"SaUtr: ${request.saUtr.map(_.utr).getOrElse("fail")}," +
          s"AgentRef: ${request.agentRef.map(_.uar).getOrElse("fail")}" +
          s"isSa: ${request.isSa}" +
          s"credentials: ${request.credentials.providerType}" +
          s"nino: ${request.nino.map(_.nino).getOrElse("fail")}"
      )
    }
  }
  val fakeCredentials                              = Credentials("foo", "bar")
  val mockAuthConnector: DefaultAuthConnector      = mock[DefaultAuthConnector]
  val citizenDetailsService: CitizenDetailsService = mock[CitizenDetailsService]
  val dataCacheConnector: DataCacheConnector       = mock[DataCacheConnector]

  val ggSignInUrl                      =
    "http://localhost:9553/bas-gateway/sign-in?continue_url=http%3A%2F%2Flocalhost%3A9217%2Fannual-tax-summary&origin=tax-summaries-frontend"
  implicit val timeout: FiniteDuration = 5 seconds
  val unauthorisedRoute                = controllers.routes.ErrorController.notAuthorised.url
  implicit val hc                      = new HeaderCarrier

  override def beforeEach() = {
    reset(mockAuthConnector)
    reset(citizenDetailsService)
    reset(dataCacheConnector)
  }

  "A user with no active session" must {
    "return 303 and be redirected to GG sign in page" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(new SessionRecordNotFound))
      val authAction =
        new MergePageAuthActionImpl(
          citizenDetailsService,
          dataCacheConnector,
          mockAuthConnector,
          new FakeMergePageAuthAction(true).mcc
        )
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
      val authAction =
        new MergePageAuthActionImpl(
          citizenDetailsService,
          dataCacheConnector,
          mockAuthConnector,
          new FakeMergePageAuthAction(true).mcc
        )
      val controller = new Harness(authAction)
      val result     = controller.onPageLoad()(FakeRequest("", ""))

      redirectLocation(result) mustBe Some("/annual-tax-summary/not-authorised")
    }
  }

  "A user with a confidence level 50 and an SA enrolment" must {
    "create an authenticated request and not call citizen details" in {
      val utr = new SaUtrGenerator().nextSaUtr.utr
      val retrievalResult: Future[
        Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[String] ~ ConfidenceLevel
      ]       =
        Future.successful(
          Enrolments(Set(Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", utr)), "Activated"))) ~ Some("") ~ Some(
            fakeCredentials
          ) ~ Some(utr) ~ None ~ ConfidenceLevel.L50
        )

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[
            String
          ] ~ ConfidenceLevel](any(), any())(any(), any())
      )
        .thenReturn(retrievalResult)

      when(citizenDetailsService.getMatchingDetails(any())(any()))
        .thenReturn(Future(SucccessMatchingDetailsResponse(MatchingDetails(None))))
      when(dataCacheConnector.getAgentToken(any(), any())) thenReturn Future.successful(None)
      val authAction =
        new MergePageAuthActionImpl(
          citizenDetailsService,
          dataCacheConnector,
          mockAuthConnector,
          new FakeMergePageAuthAction(true).mcc
        )
      val controller = new Harness(authAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) mustBe OK
      contentAsString(result) must include(utr)
      verify(citizenDetailsService, never).getMatchingDetails(any())(any())
    }
  }

  "A user with a confidence level 200, a nino and an SA enrolment" must {
    "create an authenticated request and not call citizen details" in {
      val utr = new SaUtrGenerator().nextSaUtr.utr
      val retrievalResult: Future[
        Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[String] ~ ConfidenceLevel
      ]       =
        Future.successful(
          Enrolments(Set(Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", utr)), "Activated"))) ~ Some("") ~ Some(
            fakeCredentials
          ) ~ Some(utr) ~ Some(testNino.nino) ~ ConfidenceLevel.L50
        )

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[
            String
          ] ~ ConfidenceLevel](any(), any())(any(), any())
      )
        .thenReturn(retrievalResult)

      when(citizenDetailsService.getMatchingDetails(any())(any()))
        .thenReturn(Future(SucccessMatchingDetailsResponse(MatchingDetails(None))))
      when(dataCacheConnector.getAgentToken(any(), any())) thenReturn Future.successful(None)
      val authAction =
        new MergePageAuthActionImpl(
          citizenDetailsService,
          dataCacheConnector,
          mockAuthConnector,
          new FakeMergePageAuthAction(true).mcc
        )
      val controller = new Harness(authAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) mustBe OK
      contentAsString(result) must include(utr)
      contentAsString(result) must include(testNino.nino)
      verify(citizenDetailsService, never).getMatchingDetails(any())(any())
    }
  }

  "A user with a confidence level 200, a nino and no utr" must {
    "create an authenticated request and call citizen details" in {
      val utr = new SaUtrGenerator().nextSaUtr.utr
      val retrievalResult: Future[
        Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[String] ~ ConfidenceLevel
      ]       =
        Future.successful(
          Enrolments(Set(Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", utr)), "Activated"))) ~ Some("") ~ Some(
            fakeCredentials
          ) ~ None ~ Some(testNino.nino) ~ ConfidenceLevel.L50
        )

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[
            String
          ] ~ ConfidenceLevel](any(), any())(any(), any())
      )
        .thenReturn(retrievalResult)

      when(citizenDetailsService.getMatchingDetails(any())(any()))
        .thenReturn(Future(SucccessMatchingDetailsResponse(MatchingDetails(Some(SaUtr(utr))))))

      when(dataCacheConnector.getAgentToken(any(), any())) thenReturn Future.successful(None)

      val authAction =
        new MergePageAuthActionImpl(
          citizenDetailsService,
          dataCacheConnector,
          mockAuthConnector,
          new FakeMergePageAuthAction(true).mcc
        )
      val controller = new Harness(authAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) mustBe OK
      contentAsString(result) must include(utr)
      contentAsString(result) must include(testNino.nino)
      verify(citizenDetailsService, times(1)).getMatchingDetails(any())(any())
    }
  }

  "A user with a confidence level 50 and an active IR-SA-AGENT enrolment" must {
    "create an authenticated request" in {
      val uar = testUar

      val retrievalResult: Future[
        Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[String] ~ ConfidenceLevel
      ] =
        Future.successful(
          Enrolments(Set(Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", uar)), "Activated"))) ~
            Some("") ~ Some(fakeCredentials) ~ Some(testUtr) ~ None ~ ConfidenceLevel.L50
        )

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[
            String
          ] ~ ConfidenceLevel](any(), any())(any(), any())
      )
        .thenReturn(retrievalResult)

      when(dataCacheConnector.getAgentToken(any(), any())) thenReturn Future.successful(None)

      val authAction =
        new MergePageAuthActionImpl(
          citizenDetailsService,
          dataCacheConnector,
          mockAuthConnector,
          new FakeMergePageAuthAction(true).mcc
        )
      val controller = new Harness(authAction)

      val result = controller.onPageLoad()(FakeRequest("", "/annual-tax-summary?ref=PORTAL&id=agentToken"))
      status(result) mustBe OK
      contentAsString(result) must include(uar)
      contentAsString(result) must include(testUtr)
      contentAsString(result) must include("bar")
    }
  }

  "A user with a confidence level 50 and an inactive IR-SA-AGENT enrolment" must {
    "redirect to unauthorised" in {
      val uar = testUar

      val retrievalResult: Future[
        Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[String] ~ ConfidenceLevel
      ] =
        Future.successful(
          Enrolments(Set(Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", uar)), ""))) ~
            Some("") ~ Some(fakeCredentials) ~ Some(testUtr) ~ None ~ ConfidenceLevel.L50
        )

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[
            String
          ] ~ ConfidenceLevel](any(), any())(any(), any())
      )
        .thenReturn(retrievalResult)

      when(dataCacheConnector.getAgentToken(any(), any())) thenReturn Future.successful(None)
      val authAction =
        new MergePageAuthActionImpl(
          citizenDetailsService,
          dataCacheConnector,
          mockAuthConnector,
          new FakeMergePageAuthAction(true).mcc
        )
      val controller = new Harness(authAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get must endWith(unauthorisedRoute)
    }
  }

  "A user with a confidence level 50 and an active IR-SA-AGENT enrolment, no nino and no utr" must {
    "create an authenticated request" in {
      val uar = testUar

      val retrievalResult: Future[
        Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[String] ~ ConfidenceLevel
      ] =
        Future.successful(
          Enrolments(Set(Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", uar)), "Activated"))) ~
            Some("") ~ Some(fakeCredentials) ~ None ~ None ~ ConfidenceLevel.L50
        )

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[
            String
          ] ~ ConfidenceLevel](any(), any())(any(), any())
      )
        .thenReturn(retrievalResult)
      when(dataCacheConnector.getAgentToken(any(), any())) thenReturn Future.successful(None)

      val authAction =
        new MergePageAuthActionImpl(
          citizenDetailsService,
          dataCacheConnector,
          mockAuthConnector,
          new FakeMergePageAuthAction(true).mcc
        )
      val controller = new Harness(authAction)

      val result = controller.onPageLoad()(FakeRequest("", "/annual-tax-summary?ref=PORTAL&id=agentToken"))
      status(result) mustBe OK
      contentAsString(result) must include(uar)
      contentAsString(result) must include("false")
      contentAsString(result) must include("bar")
    }

    "with empty agent token(with ref not present) must see access denied" in {
      val uar = testUar

      val retrievalResult: Future[
        Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[String] ~ ConfidenceLevel
      ] =
        Future.successful(
          Enrolments(Set(Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", uar)), "Activated"))) ~
            Some("") ~ Some(fakeCredentials) ~ None ~ None ~ ConfidenceLevel.L50
        )

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[
            String
          ] ~ ConfidenceLevel](any(), any())(any(), any())
      )
        .thenReturn(retrievalResult)
      when(dataCacheConnector.getAgentToken(any(), any())) thenReturn Future.successful(None)

      val authAction =
        new MergePageAuthActionImpl(
          citizenDetailsService,
          dataCacheConnector,
          mockAuthConnector,
          new FakeMergePageAuthAction(true).mcc
        )
      val controller = new Harness(authAction)

      val result = controller.onPageLoad()(FakeRequest("", "/annual-tax-summary"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some("/annual-tax-summary/not-authorised")
    }

    "with empty agent token(with id not present) must see access denied" in {
      val uar = testUar

      val retrievalResult: Future[
        Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[String] ~ ConfidenceLevel
      ] =
        Future.successful(
          Enrolments(Set(Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", uar)), "Activated"))) ~
            Some("") ~ Some(fakeCredentials) ~ None ~ None ~ ConfidenceLevel.L50
        )

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[
            String
          ] ~ ConfidenceLevel](any(), any())(any(), any())
      )
        .thenReturn(retrievalResult)
      when(dataCacheConnector.getAgentToken(any(), any())) thenReturn Future.successful(None)

      val authAction =
        new MergePageAuthActionImpl(
          citizenDetailsService,
          dataCacheConnector,
          mockAuthConnector,
          new FakeMergePageAuthAction(true).mcc
        )
      val controller = new Harness(authAction)

      val result = controller.onPageLoad()(FakeRequest("", "/annual-tax-summary?ref=PORTAL"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some("/annual-tax-summary/not-authorised")
    }

    "with agent navigating with token in cache must be authorized" in {
      val uar = testUar

      val agentToken = AgentToken(
        agentUar = testUar,
        clientUtr = testUtr,
        timestamp = 0
      )
      val retrievalResult: Future[
        Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[String] ~ ConfidenceLevel
      ]              =
        Future.successful(
          Enrolments(Set(Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", uar)), "Activated"))) ~
            Some("") ~ Some(fakeCredentials) ~ None ~ None ~ ConfidenceLevel.L50
        )

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[
            String
          ] ~ ConfidenceLevel](any(), any())(any(), any())
      )
        .thenReturn(retrievalResult)
      when(dataCacheConnector.getAgentToken(any(), any())) thenReturn Future.successful(Some(agentToken))

      val authAction =
        new MergePageAuthActionImpl(
          citizenDetailsService,
          dataCacheConnector,
          mockAuthConnector,
          new FakeMergePageAuthAction(true).mcc
        )
      val controller = new Harness(authAction)

      val result = controller.onPageLoad()(FakeRequest("", "/annual-tax-summary"))
      status(result) mustBe OK
      contentAsString(result) must include(uar)
      contentAsString(result) must include("false")
      contentAsString(result) must include("bar")
    }
  }

  "A user with a confidence level 50, no nino and no utr" must {
    "see access denied" in {

      val retrievalResult: Future[
        Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[String] ~ ConfidenceLevel
      ] =
        Future.successful(
          Enrolments(Set(Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", testUtr)), "Activated"))) ~
            Some("") ~ Some(fakeCredentials) ~ None ~ None ~ ConfidenceLevel.L50
        )

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[
            String
          ] ~ ConfidenceLevel](any(), any())(any(), any())
      )
        .thenReturn(retrievalResult)
      when(dataCacheConnector.getAgentToken(any(), any())) thenReturn Future.successful(None)

      val authAction =
        new MergePageAuthActionImpl(
          citizenDetailsService,
          dataCacheConnector,
          mockAuthConnector,
          new FakeMergePageAuthAction(true).mcc
        )
      val controller = new Harness(authAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some("/annual-tax-summary/not-authorised")
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

    when(
      mockAuthConnector
        .authorise[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[
          String
        ] ~ ConfidenceLevel](any(), any())(any(), any())
    )
      .thenReturn(retrievalResult)

    val authAction =
      new MergePageAuthActionImpl(
        citizenDetailsService,
        dataCacheConnector,
        mockAuthConnector,
        new FakeMergePageAuthAction(true).mcc
      )
    val controller = new Harness(authAction)

    val ex = intercept[RuntimeException] {
      await(controller.onPageLoad()(FakeRequest("", "")))
    }

    ex.getMessage must include("Can't find credentials for user")

  }

}
