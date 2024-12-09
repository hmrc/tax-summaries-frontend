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

import cats.data.EitherT
import config.ApplicationConfig
import controllers.auth.actions.AuthAction
import models.AgentToken
import models.admin.SelfAssessmentServiceToggle
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Action, AnyContent, AnyContentAsEmpty, InjectedController}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repository.TaxsAgentTokenSessionCacheRepository
import services.{CitizenDetailsService, PertaxAuthService}
import uk.gov.hmrc.auth.core.ConfidenceLevel.L50
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.mongo.cache.DataKey
import uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlag
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import utils.BaseSpec
import utils.RetrievalOps.Ops

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class AuthActionSpec extends BaseSpec {

  private val mockTaxsAgentTokenSessionCacheRepository           = mock[TaxsAgentTokenSessionCacheRepository]
  private val mockCitizenDetailsService                          = mock[CitizenDetailsService]
  private val mockPertaxAuthService                              = mock[PertaxAuthService]
  implicit lazy val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

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
      taxsAgentTokenSessionCacheRepository = mockTaxsAgentTokenSessionCacheRepository,
      citizenDetailsService = mockCitizenDetailsService,
      pertaxAuthService = mockPertaxAuthService,
      mockFeatureFlagService
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
    reset(mockTaxsAgentTokenSessionCacheRepository)
    reset(mockCitizenDetailsService)
    reset(mockPertaxAuthService)
    reset(mockFeatureFlagService)
    when(mockPertaxAuthService.authorise(any())).thenReturn(Future.successful(None))
    when(mockFeatureFlagService.get(ArgumentMatchers.eq(SelfAssessmentServiceToggle)))
      .thenReturn(Future.successful(FeatureFlag(SelfAssessmentServiceToggle, isEnabled = true)))
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
    nino: Option[String] = None,
    confidenceLevel: ConfidenceLevel = L50
  ): Unit = {
    val utr = enrolments.find(_.key == "IR-SA").flatMap(_.identifiers.find(_.key == "UTR").map(_.value))
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
  }

  "invokeBlock" when {

    "shutter, agent token and utr checks are all false" must {
      "Not call citizen details and return OK when SA UTR is in the enrolments" in {
        whenRetrieval(
          nino = Some(nino),
          enrolments = Set(
            Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", utr)), "Activated")
          )
        )

        val result = createHarness.onPageLoad()(fakeRequest)
        status(result) mustBe OK
        verify(mockCitizenDetailsService, times(0)).getMatchingSaUtr(any())(any())
      }

      "Call citizen details and return OK when no SA UTR is in the enrolments" in {
        whenRetrieval(nino = Some(nino))

        when(mockCitizenDetailsService.getMatchingSaUtr(any())(any()))
          .thenReturn(EitherT.rightT(Some(SaUtr(utr))))
        val result = createHarness.onPageLoad()(fakeRequest)
        status(result) mustBe OK
        verify(mockCitizenDetailsService, times(1)).getMatchingSaUtr(any())(any())
      }

      "Not call citizen details and redirect to not authorised page when an agent is inactive" in {
        whenRetrieval(enrolments =
          Set(Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", agentRef)), "Inactive"))
        )

        val result = createHarness.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.ErrorController.notAuthorised.url)
        verify(mockCitizenDetailsService, times(0)).getMatchingSaUtr(any())(any())
      }

      "Not call agent token check or citizen details and return OK when agent is active" in {
        whenRetrieval(enrolments =
          Set(
            Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", agentRef)), "Activated")
          )
        )

        val result = createHarness.onPageLoad()(fakeRequest)
        status(result) mustBe OK
        verify(mockCitizenDetailsService, times(0)).getMatchingSaUtr(any())(any())
        verify(mockTaxsAgentTokenSessionCacheRepository, times(0))
          .getFromSession[AgentToken](DataKey(any()))(any(), any())
      }

      "redirect to failure url when authorisation fails" in {
        whenRetrieval(nino = Some(nino))
        when(mockPertaxAuthService.authorise(any())).thenReturn(Future.successful(Some(Redirect("/dummy"))))
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
      "Not call citizen details and return OK when utr is already available in enrolments" in {
        whenRetrieval(
          nino = Some(nino),
          enrolments = Set(Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", utr)), "Activated"))
        )

        when(mockPertaxAuthService.authorise(any())).thenReturn(Future.successful(None))
        val result = createHarness.onPageLoad(utrCheck = true)(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) must include(s"SaUtr: $utr")
        verify(mockCitizenDetailsService, times(0)).getMatchingSaUtr(any())(any())
      }

      "Call citizen details and return OK when it returns a utr" in {
        whenRetrieval(nino = Some(nino))
        when(mockPertaxAuthService.authorise(any())).thenReturn(Future.successful(None))
        when(mockCitizenDetailsService.getMatchingSaUtr(any())(any()))
          .thenReturn(EitherT.rightT(Some(SaUtr(utr))))
        val result = createHarness.onPageLoad(utrCheck = true)(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) must include(s"SaUtr: $utr")
        verify(mockCitizenDetailsService, times(1)).getMatchingSaUtr(any())(any())
      }

      "Call citizen details and redirect to not authorised when it returns no utr" in {
        whenRetrieval(nino = Some(nino))
        when(mockPertaxAuthService.authorise(any())).thenReturn(Future.successful(None))
        when(mockCitizenDetailsService.getMatchingSaUtr(any())(any()))
          .thenReturn(EitherT.rightT(None))
        val result = createHarness.onPageLoad(utrCheck = true)(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.ErrorController.notAuthorised.url)
        verify(mockCitizenDetailsService, times(1)).getMatchingSaUtr(any())(any())
      }
    }

    "shutter check is true" must {
      "redirect to service unavailable page when SA is not enabled" in {
        when(mockFeatureFlagService.get(ArgumentMatchers.eq(SelfAssessmentServiceToggle)))
          .thenReturn(Future.successful(FeatureFlag(SelfAssessmentServiceToggle, isEnabled = false)))
        val result = createHarness.onPageLoad(saShutterCheck = true)(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.ErrorController.serviceUnavailable.url)
      }
      "return OK when SA is enabled" in {
        when(mockFeatureFlagService.get(ArgumentMatchers.eq(SelfAssessmentServiceToggle)))
          .thenReturn(Future.successful(FeatureFlag(SelfAssessmentServiceToggle, isEnabled = true)))
        whenRetrieval(nino = Some(nino))
        when(mockPertaxAuthService.authorise(any())).thenReturn(Future.successful(None))
        when(mockCitizenDetailsService.getMatchingSaUtr(any())(any()))
          .thenReturn(EitherT.rightT(None))
        val result = createHarness.onPageLoad()(fakeRequest)
        status(result) mustBe OK
      }
    }

    "agent token check is true" must {
      "direct to OK when agent token present in db & query params present" in {
        when(mockFeatureFlagService.get(ArgumentMatchers.eq(SelfAssessmentServiceToggle)))
          .thenReturn(Future.successful(FeatureFlag(SelfAssessmentServiceToggle, isEnabled = true)))

        whenRetrieval(enrolments =
          Set(
            Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", agentRef)), "Activated"),
            Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", utr)), "Activated")
          )
        )

        when(mockTaxsAgentTokenSessionCacheRepository.getFromSession[AgentToken](DataKey(any()))(any(), any()))
          .thenReturn(
            Future
              .successful(Some(agentToken))
          )

        val result =
          createHarness.onPageLoad(agentTokenCheck = true)(
            FakeRequest("GET", "http://test.com?ref=PORTAL&id=something")
          )
        status(result) mustBe OK
      }

      "direct to OK when agent token absent from db & query params present" in {
        when(mockFeatureFlagService.get(ArgumentMatchers.eq(SelfAssessmentServiceToggle)))
          .thenReturn(Future.successful(FeatureFlag(SelfAssessmentServiceToggle, isEnabled = true)))
        whenRetrieval(enrolments =
          Set(
            Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", agentRef)), "Activated"),
            Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", utr)), "Activated")
          )
        )

        when(mockTaxsAgentTokenSessionCacheRepository.getFromSession[AgentToken](DataKey(any()))(any(), any()))
          .thenReturn(
            Future
              .successful(None)
          )

        val result =
          createHarness.onPageLoad(agentTokenCheck = true)(
            FakeRequest("GET", "http://test.com?ref=PORTAL&id=something")
          )
        status(result) mustBe OK
      }

      "redirect to not authorised page when agent token present in db & query params absent" in {
        when(mockFeatureFlagService.get(ArgumentMatchers.eq(SelfAssessmentServiceToggle)))
          .thenReturn(Future.successful(FeatureFlag(SelfAssessmentServiceToggle, isEnabled = true)))
        whenRetrieval(enrolments =
          Set(
            Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", agentRef)), "Activated"),
            Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", utr)), "Activated")
          )
        )

        when(mockTaxsAgentTokenSessionCacheRepository.getFromSession[AgentToken](DataKey(any()))(any(), any()))
          .thenReturn(
            Future
              .successful(None)
          )

        val result =
          createHarness.onPageLoad(agentTokenCheck = true)(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.ErrorController.notAuthorised.url)
      }
    }
  }
}
