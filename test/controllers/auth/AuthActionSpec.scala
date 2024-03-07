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

import config.ApplicationConfig
import connectors.DataCacheConnector
import controllers.auth.actions.AuthAction
import models.{AgentToken, MatchingDetails}
import org.mockito.ArgumentMatchers.any
import play.api.http.Status.SEE_OTHER
import play.api.mvc.Results.Redirect
import play.api.mvc.{Action, AnyContent, InjectedController}
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, _}
import services.{CitizenDetailsService, PertaxAuthService, SucccessMatchingDetailsResponse}
import uk.gov.hmrc.auth.core.ConfidenceLevel.L50
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import utils.BaseSpec
import utils.RetrievalOps.Ops

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class AuthActionSpec extends BaseSpec {

  private val mockDataCacheConnector    = mock[DataCacheConnector]
  private val mockCitizenDetailsService = mock[CitizenDetailsService]
  private val mockPertaxAuthService     = mock[PertaxAuthService]

  private class Harness(authAction: AuthAction) extends InjectedController {
    def onPageLoad(
      saShutterCheck: Boolean = false,
      agentTokenCheck: Boolean = false,
      utrCheck: Boolean = false
    ): Action[AnyContent] =
      authAction(saShutterCheck = saShutterCheck, agentTokenCheck = agentTokenCheck, utrCheck = utrCheck) { request =>
        Ok(
          s"SaUtr: ${request.saUtr.map(_.utr).getOrElse("fail")}," +
            s"AgentRef: ${request.agentRef.map(_.uar).getOrElse("fail")}" +
            s"isSa: ${request.isSa}" +
            s"credentials: ${request.credentials.providerType}"
        )
      }
  }

  override implicit lazy val appConfig: ApplicationConfig = mock[ApplicationConfig]

  private def createHarness: Harness = {
    val authAction: AuthAction = new AuthAction(
      authConnector = mockAuthConnector,
      cc = FakeAuthAction.mcc,
      dataCacheConnector = mockDataCacheConnector,
      citizenDetailsService = mockCitizenDetailsService,
      pertaxAuthService = mockPertaxAuthService
    )(ec, appConfig)
    new Harness(authAction)
  }

  val fakeCredentials: Credentials            = Credentials("foo", "bar")
  val mockAuthConnector: DefaultAuthConnector = mock[DefaultAuthConnector]

  val ggSignInUrl                      =
    "http://localhost:9553/bas-gateway/sign-in?continue_url=http%3A%2F%2Flocalhost%3A9217%2Fannual-tax-summary&origin=tax-summaries-frontend"
  implicit val timeout: FiniteDuration = 5 seconds

  override def beforeEach(): Unit = {
    reset(appConfig)
    reset(mockAuthConnector)
    reset(mockDataCacheConnector)
    reset(mockCitizenDetailsService)
    reset(mockPertaxAuthService)
    when(mockPertaxAuthService.authorise(any())).thenReturn(Future.successful(None))
    when(appConfig.saShuttered).thenReturn(false)
  }

  private val extId: String          = "123"
  private val nino: String           = "CS121212C"
  private val utr: String            = "123"
  private val agentRef: String       = "123"
  private val agentToken: AgentToken = AgentToken(agentUar = agentRef, clientUtr = utr, timestamp = 1L)

  private val fakeRequest = FakeRequest("GET", "http://test.com")

  private def whenRetrieval(
    enrolments: Set[Enrolment] = Set.empty,
    externalId: Option[String] = Some(extId),
    creds: Option[Credentials] = Some(fakeCredentials),
    utr: Option[String] = None,
    nino: Option[String] = None,
    confidenceLevel: ConfidenceLevel = L50
  ): Unit =
    when(
      mockAuthConnector
        .authorise[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String] ~ Option[
          String
        ] ~ ConfidenceLevel](any(), any())(any(), any())
    ).thenReturn(
      Future.successful(
        Enrolments(enrolments) ~ externalId ~ creds ~ utr ~ nino ~ confidenceLevel
      )
    )

  "invokeBlock" when {

    "shutter, agent token and utr checks are all false" must {
      "Return OK when an agent is active" in {
        whenRetrieval(enrolments =
          Set(
            Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", agentRef)), "Activated"),
            Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", utr)), "Activated")
          )
        )
        when(mockDataCacheConnector.getAgentToken(any(), any())).thenReturn(Future.successful(None))
        val result = createHarness.onPageLoad()(fakeRequest)
        status(result) mustBe OK
      }

      "redirect to not authorised page when an agent is inactive" in {
        whenRetrieval(enrolments =
          Set(
            Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", agentRef)), "Inactive"),
            Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", utr)), "Activated")
          )
        )
        when(mockDataCacheConnector.getAgentToken(any(), any())).thenReturn(Future.successful(None))
        val result = createHarness.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.ErrorController.notAuthorised.url)
      }

      "Call citizen details & find no utr and return OK when a non-agent with nino but no utr and authorised successfully" in {
        whenRetrieval(nino = Some(nino))
        when(mockPertaxAuthService.authorise(any())).thenReturn(Future.successful(None))
        when(mockCitizenDetailsService.getMatchingDetails(any())(any()))
          .thenReturn(Future(SucccessMatchingDetailsResponse(MatchingDetails(None))))
        val result = createHarness.onPageLoad()(fakeRequest)
        status(result) mustBe OK
      }

      "Call citizen details and find a utr and return OK when a non-agent with nino but no utr and authorised successfully" in {
        whenRetrieval(nino = Some(nino))
        when(mockPertaxAuthService.authorise(any())).thenReturn(Future.successful(None))
        when(mockCitizenDetailsService.getMatchingDetails(any())(any()))
          .thenReturn(Future(SucccessMatchingDetailsResponse(MatchingDetails(Some(SaUtr(utr))))))
        val result = createHarness.onPageLoad()(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) must include(s"SaUtr: $utr")
      }

      "redirect to failure url when authorisation fails" in {
        whenRetrieval(nino = Some(nino))
        when(mockPertaxAuthService.authorise(any())).thenReturn(Future.successful(Some(Redirect("/dummy"))))
        when(mockCitizenDetailsService.getMatchingDetails(any())(any()))
          .thenReturn(Future(SucccessMatchingDetailsResponse(MatchingDetails(None))))
        val result = createHarness.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/dummy")
      }

      "when no active session return 303 and be redirected to GG sign in page" in {
        when(mockAuthConnector.authorise(any(), any())(any(), any()))
          .thenReturn(Future.failed(new SessionRecordNotFound))

        when(appConfig.loginUrl).thenReturn("/loginUrl")
        when(appConfig.loginCallback).thenReturn("/loginCallback")
        when(appConfig.appName).thenReturn("appname")

        val result = createHarness.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/loginUrl?continue_url=%2FloginCallback&origin=appname")
      }

      "Throw exception when no credentials" in {
        whenRetrieval(creds = None)
        val result = createHarness.onPageLoad()(fakeRequest)
        val ex     = intercept[RuntimeException] {
          await(result)
        }
        ex.getMessage must include("Can't find credentials for user")
      }
    }

    "utr check is true" must {

      "Return OK when citizen details returns a utr" in {
        whenRetrieval(nino = Some(nino))
        when(mockPertaxAuthService.authorise(any())).thenReturn(Future.successful(None))
        when(mockCitizenDetailsService.getMatchingDetails(any())(any()))
          .thenReturn(Future(SucccessMatchingDetailsResponse(MatchingDetails(Some(SaUtr(utr))))))
        val result = createHarness.onPageLoad(utrCheck = true)(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) must include(s"SaUtr: $utr")
      }

      "Redirect to not authorised when citizen details returns no utr" in {
        whenRetrieval(nino = Some(nino))
        when(mockPertaxAuthService.authorise(any())).thenReturn(Future.successful(None))
        when(mockCitizenDetailsService.getMatchingDetails(any())(any()))
          .thenReturn(Future(SucccessMatchingDetailsResponse(MatchingDetails(None))))
        val result = createHarness.onPageLoad(utrCheck = true)(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.ErrorController.notAuthorised.url)
      }
    }

    "shutter check is true" must {
      "redirect to service unavailable page when SA is shuttered" in {
        when(appConfig.saShuttered).thenReturn(true)
        val result = createHarness.onPageLoad(saShutterCheck = true)(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.ErrorController.serviceUnavailable.url)
      }
      "return OK when SA is NOT shuttered" in {
        when(appConfig.saShuttered).thenReturn(false)
        whenRetrieval(nino = Some(nino))
        when(mockPertaxAuthService.authorise(any())).thenReturn(Future.successful(None))
        when(mockCitizenDetailsService.getMatchingDetails(any())(any()))
          .thenReturn(Future(SucccessMatchingDetailsResponse(MatchingDetails(None))))
        val result = createHarness.onPageLoad()(fakeRequest)
        status(result) mustBe OK
      }
    }

    "agent token check is true" must {
      "direct to OK when agent token present in db & query params present" in {
        when(appConfig.saShuttered).thenReturn(false)

        whenRetrieval(enrolments =
          Set(
            Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", agentRef)), "Activated"),
            Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", utr)), "Activated")
          )
        )
        when(mockDataCacheConnector.getAgentToken(any(), any())).thenReturn(Future.successful(Some(agentToken)))
        val result =
          createHarness.onPageLoad(agentTokenCheck = true)(
            FakeRequest("GET", "http://test.com?ref=PORTAL&id=something")
          )
        status(result) mustBe OK
      }

      "direct to OK when agent token absent from db & query params present" in {
        when(appConfig.saShuttered).thenReturn(false)

        whenRetrieval(enrolments =
          Set(
            Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", agentRef)), "Activated"),
            Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", utr)), "Activated")
          )
        )
        when(mockDataCacheConnector.getAgentToken(any(), any())).thenReturn(Future.successful(None))
        val result =
          createHarness.onPageLoad(agentTokenCheck = true)(
            FakeRequest("GET", "http://test.com?ref=PORTAL&id=something")
          )
        status(result) mustBe OK
      }

      "redirect to not authorised page when agent token present in db & query params absent" in {
        when(appConfig.saShuttered).thenReturn(false)

        whenRetrieval(enrolments =
          Set(
            Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", agentRef)), "Activated"),
            Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", utr)), "Activated")
          )
        )
        when(mockDataCacheConnector.getAgentToken(any(), any())).thenReturn(Future.successful(None))
        val result =
          createHarness.onPageLoad(agentTokenCheck = true)(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.ErrorController.serviceUnavailable.url)
      }
    }
  }
}
