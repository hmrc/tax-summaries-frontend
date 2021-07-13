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
import models.MatchingDetails
import org.scalatest.concurrent.ScalaFutures
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.{Action, AnyContent, InjectedController, Results}
import play.api.http.Status._
import play.api.test.Helpers.{contentAsString, redirectLocation}
import play.api.test.{FakeRequest, Injecting}
import services.{CitizenDetailsService, FailedMatchingDetailsResponse, SucccessMatchingDetailsResponse}
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.domain.{Generator, SaUtr, SaUtrGenerator}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.test.UnitSpec
import utils.TestConstants.{fakeCredentials, testUar}
import utils.RetrievalOps._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class SelfAssessmentActionSpec
    extends UnitSpec with MockitoSugar with ScalaFutures with GuiceOneAppPerSuite with Injecting {

  lazy implicit val ec = inject[ExecutionContext]
  implicit val timeout: FiniteDuration = 5 seconds
  lazy implicit val appConfig = inject[ApplicationConfig]
  implicit val hc = HeaderCarrier()

  val unauthorizedRoute = controllers.routes.ErrorController.notAuthorised().url

  val mockAuthConnector: DefaultAuthConnector = mock[DefaultAuthConnector]
  val citizenDetailsService = mock[CitizenDetailsService]
  val ninoAuthAction = mock[NinoAuthAction]

  val action = new SelfAssessmentActionImpl(citizenDetailsService, ninoAuthAction, appConfig)

  class Harness(minAuthAction: AuthAction, selfAssessmentAction: SelfAssessmentAction) extends InjectedController {
    def onPageLoad(): Action[AnyContent] = (minAuthAction andThen selfAssessmentAction) { request =>
      Ok(s"utr is ${request.saUtr}")
    }
  }

  "refine with empty utr" should {
    "do nothing" in {
      val utr = new SaUtrGenerator().nextSaUtr.utr
      val uar = testUar

      val retrievalResult: Future[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String]] =
        Future.successful(
          Enrolments(
            Set(
              Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", uar)), ""),
              Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", utr)), ""))) ~ Some("") ~ Some(fakeCredentials) ~ Some(
            utr)
        )

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String]](any(), any())(any(), any()))
        .thenReturn(retrievalResult)

      val authAction = new AuthActionImpl(mockAuthConnector, FakeAuthAction.mcc)
      val controller = new Harness(selfAssessmentAction = action, minAuthAction = authAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) shouldBe OK
      contentAsString(result)(timeout) should include(utr)
      verifyZeroInteractions(citizenDetailsService)
    }
  }

  "refine with non empty utr" should {
    "redirect to unauthorized if nino is not present" in {
      val utr = new SaUtrGenerator().nextSaUtr.utr
      val uar = testUar

      val retrievalResult: Future[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String]] =
        Future.successful(
          Enrolments(Set(Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", uar)), ""))) ~ Some("") ~ Some(
            fakeCredentials) ~ None
        )

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String]](any(), any())(any(), any()))
        .thenReturn(retrievalResult)

      when(ninoAuthAction.getNino()(any())).thenReturn(Future(NoAtsNinoFound))

      val authAction = new AuthActionImpl(mockAuthConnector, FakeAuthAction.mcc)
      val controller = new Harness(selfAssessmentAction = action, minAuthAction = authAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result)(timeout).get should endWith(unauthorizedRoute)
      verifyZeroInteractions(citizenDetailsService)
    }

    "redirect to uplift if nino is present but confidence level is too low" in {
      val uar = testUar

      val retrievalResult: Future[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String]] =
        Future.successful(
          Enrolments(Set(Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", uar)), ""))) ~ Some("") ~ Some(
            fakeCredentials) ~ None
        )

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String]](any(), any())(any(), any()))
        .thenReturn(retrievalResult)

      when(ninoAuthAction.getNino()(any())).thenReturn(Future(UpliftRequiredAtsNino))

      val authAction = new AuthActionImpl(mockAuthConnector, FakeAuthAction.mcc)
      val controller = new Harness(selfAssessmentAction = action, minAuthAction = authAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result)(timeout).get should startWith(appConfig.identityVerificationUpliftUrl)
      verifyZeroInteractions(citizenDetailsService)
    }

    "redirect to unauthorized if utr cant be found for nino" in {
      val uar = testUar
      val nino = new Generator().nextNino

      val retrievalResult: Future[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String]] =
        Future.successful(
          Enrolments(Set(Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", uar)), ""))) ~ Some("") ~ Some(
            fakeCredentials) ~ None
        )

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String]](any(), any())(any(), any()))
        .thenReturn(retrievalResult)

      when(ninoAuthAction.getNino()(any())).thenReturn(Future(SuccessAtsNino(nino.toString())))
      when(citizenDetailsService.getUtr(any())(any())).thenReturn(Future(FailedMatchingDetailsResponse))

      val authAction = new AuthActionImpl(mockAuthConnector, FakeAuthAction.mcc)
      val controller = new Harness(selfAssessmentAction = action, minAuthAction = authAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result)(timeout).get should endWith(unauthorizedRoute)
      verify(citizenDetailsService, times(1)).getUtr(any())(any())
    }

    "return OK if utr can be found for nino" in {
      reset(citizenDetailsService)
      val utr = new SaUtrGenerator().nextSaUtr.utr
      val uar = testUar
      val nino = new Generator().nextNino

      val retrievalResult: Future[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String]] =
        Future.successful(
          Enrolments(Set(Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", uar)), ""))) ~ Some("") ~ Some(
            fakeCredentials) ~ None
        )

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[Credentials] ~ Option[String]](any(), any())(any(), any()))
        .thenReturn(retrievalResult)

      when(ninoAuthAction.getNino()(any())).thenReturn(Future(SuccessAtsNino(nino.toString())))
      when(citizenDetailsService.getUtr(any())(any()))
        .thenReturn(Future(SucccessMatchingDetailsResponse(MatchingDetails(Some(SaUtr(utr))))))

      val authAction = new AuthActionImpl(mockAuthConnector, FakeAuthAction.mcc)
      val controller = new Harness(selfAssessmentAction = action, minAuthAction = authAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) shouldBe OK
      contentAsString(result)(timeout) should include(utr)
      verify(citizenDetailsService, times(1)).getUtr(any())(any())
    }
  }

}
