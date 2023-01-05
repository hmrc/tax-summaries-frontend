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

import models.MatchingDetails
import org.mockito.ArgumentMatchers.any
import org.scalatest.concurrent.ScalaFutures
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.{Action, AnyContent, BodyParser, InjectedController, Request, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.{CitizenDetailsService, FailedMatchingDetailsResponse, SucccessMatchingDetailsResponse}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.{Generator, SaUtr, SaUtrGenerator, Uar}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import utils.{BaseSpec, ControllerBaseSpec}
import utils.TestConstants.testUar

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

class SelfAssessmentActionSpec
    extends BaseSpec
    with MockitoSugar
    with ScalaFutures
    with GuiceOneAppPerSuite
    with Injecting
    with BeforeAndAfterEach {

  implicit val timeout: FiniteDuration = 5 seconds
  implicit val hc                      = HeaderCarrier()

  val unauthorizedRoute = controllers.routes.ErrorController.notAuthorised.url

  val mockAuthConnector: DefaultAuthConnector = mock[DefaultAuthConnector]
  val citizenDetailsService                   = mock[CitizenDetailsService]
  val ninoAuthAction                          = mock[NinoAuthAction]

  val action = new SelfAssessmentActionImpl(citizenDetailsService, ninoAuthAction, appConfig)

  class FakeSelfAssessmentAction(utr: Option[SaUtr], uar: Option[Uar]) extends ControllerBaseSpec with AuthAction {

    override val parser: BodyParser[AnyContent]               = mcc.parsers.anyContent
    override protected val executionContext: ExecutionContext = mcc.executionContext

    override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] =
      block(
        AuthenticatedRequest(
          "userId",
          uar,
          utr,
          None,
          utr.isDefined,
          uar.isDefined,
          ConfidenceLevel.L50,
          fakeCredentials,
          request
        )
      )
  }

  override def beforeEach(): Unit =
    reset(citizenDetailsService)

  class Harness(minAuthAction: FakeSelfAssessmentAction, selfAssessmentAction: SelfAssessmentAction)
      extends InjectedController {
    def onPageLoad(): Action[AnyContent] = (minAuthAction andThen selfAssessmentAction) { request =>
      Ok(s"utr is ${request.saUtr}")
    }
  }

  "refine with non empty utr and non empty agent ref" must {
    "do nothing" in {
      val utr = new SaUtrGenerator().nextSaUtr.utr
      val uar = testUar

      val authAction = new FakeSelfAssessmentAction(Some(SaUtr(utr)), Some(Uar(uar)))
      val controller = new Harness(selfAssessmentAction = action, minAuthAction = authAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) mustBe OK
      contentAsString(result)(timeout) must include(utr)
      verifyZeroInteractions(citizenDetailsService)
    }
  }

  "refine with empty utr and non empty agent ref" must {
    "do nothing" in {
      val uar = testUar

      val authAction = new FakeSelfAssessmentAction(None, Some(Uar(uar)))
      val controller = new Harness(selfAssessmentAction = action, minAuthAction = authAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) mustBe OK
      contentAsString(result)(timeout) must include("None")
      verifyZeroInteractions(citizenDetailsService)
    }
  }

  "refine with non empty utr and not an agent" must {
    "do nothing" in {
      val utr = new SaUtrGenerator().nextSaUtr.utr

      val authAction = new FakeSelfAssessmentAction(Some(SaUtr(utr)), None)
      val controller = new Harness(selfAssessmentAction = action, minAuthAction = authAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) mustBe OK
      contentAsString(result)(timeout) must include(utr)
      verifyZeroInteractions(citizenDetailsService)
    }
  }

  "refine with empty utr and not an agent" must {
    "redirect to unauthorized if nino is not present" in {
      when(ninoAuthAction.getNino()(any())).thenReturn(Future(NoAtsNinoFound))

      val authAction = new FakeSelfAssessmentAction(None, None)
      val controller = new Harness(selfAssessmentAction = action, minAuthAction = authAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) mustBe SEE_OTHER
      redirectLocation(result)(timeout).get must endWith(unauthorizedRoute)
      verifyZeroInteractions(citizenDetailsService)
    }

    "redirect to uplift if nino is present but confidence level is too low" in {
      when(ninoAuthAction.getNino()(any())).thenReturn(Future(UpliftRequiredAtsNino))

      val authAction = new FakeSelfAssessmentAction(None, None)
      val controller = new Harness(selfAssessmentAction = action, minAuthAction = authAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) mustBe SEE_OTHER
      redirectLocation(result)(timeout).get must startWith(appConfig.identityVerificationUpliftUrl)
      verifyZeroInteractions(citizenDetailsService)
    }

    "redirect to unauthorized if matching details cant be found for nino" in {
      val nino = new Generator().nextNino

      when(ninoAuthAction.getNino()(any())).thenReturn(Future(SuccessAtsNino(nino.toString())))
      when(citizenDetailsService.getMatchingDetails(any())(any())).thenReturn(Future(FailedMatchingDetailsResponse))

      val authAction = new FakeSelfAssessmentAction(None, None)
      val controller = new Harness(selfAssessmentAction = action, minAuthAction = authAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) mustBe SEE_OTHER
      redirectLocation(result)(timeout).get must endWith(unauthorizedRoute)
      verify(citizenDetailsService, times(1)).getMatchingDetails(any())(any())
    }

    "redirect to unauthorized if utr cant be found for nino" in {
      val nino = new Generator().nextNino

      when(ninoAuthAction.getNino()(any())).thenReturn(Future(SuccessAtsNino(nino.toString())))
      when(citizenDetailsService.getMatchingDetails(any())(any()))
        .thenReturn(Future(SucccessMatchingDetailsResponse(MatchingDetails(None))))

      val authAction = new FakeSelfAssessmentAction(None, None)
      val controller = new Harness(selfAssessmentAction = action, minAuthAction = authAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) mustBe SEE_OTHER
      redirectLocation(result)(timeout).get must endWith(unauthorizedRoute)
      verify(citizenDetailsService, times(1)).getMatchingDetails(any())(any())
    }

    "redirect to unauthorized if user doesn't have strong credentials" in {

      when(ninoAuthAction.getNino()(any())).thenReturn(Future(InsufficientCredsNino))

      val authAction = new FakeSelfAssessmentAction(None, None)
      val controller = new Harness(selfAssessmentAction = action, minAuthAction = authAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) mustBe SEE_OTHER
      redirectLocation(result)(timeout).get must endWith(unauthorizedRoute)
      verifyZeroInteractions(citizenDetailsService)
    }

    "return OK if utr can be found for nino" in {
      val utr  = new SaUtrGenerator().nextSaUtr.utr
      val nino = new Generator().nextNino

      when(ninoAuthAction.getNino()(any())).thenReturn(Future(SuccessAtsNino(nino.toString())))
      when(citizenDetailsService.getMatchingDetails(any())(any()))
        .thenReturn(Future(SucccessMatchingDetailsResponse(MatchingDetails(Some(SaUtr(utr))))))

      val authAction = new FakeSelfAssessmentAction(None, None)
      val controller = new Harness(selfAssessmentAction = action, minAuthAction = authAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) mustBe OK
      contentAsString(result)(timeout) must include(utr)
      verify(citizenDetailsService, times(1)).getMatchingDetails(any())(any())
    }
  }
}
