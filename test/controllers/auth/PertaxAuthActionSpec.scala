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

import cats.data.EitherT
import connectors.PertaxConnector
import controllers.paye.routes
import models.PertaxApiResponse
import org.mockito.Matchers.any
import org.mockito.Mockito.{reset, when}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, InjectedController}
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import utils.BaseSpec
import utils.RetrievalOps.Ops
import utils.TestConstants.fakeCredentials
import views.html.errors.ServiceUnavailableView

import scala.concurrent.Future

class PertaxAuthActionSpec extends BaseSpec {

  val mockAuthConnector: DefaultAuthConnector = mock[DefaultAuthConnector]

  val mockPertaxConnector: PertaxConnector = mock[PertaxConnector]

  val unauthorisedRoute = routes.PayeErrorController.notAuthorised.url

  class Harness(authJourney: AuthJourney) extends InjectedController {
    def onPageLoad(): Action[AnyContent] = authJourney.authWithSingleGGCheck { request =>
      Ok(s"Nino: ${request.nino.nino}")
    }
  }

  override def beforeEach(): Unit =
    reset(mockAuthConnector, mockPertaxConnector)

  val payeAuthAction = new PayeAuthActionImpl(
    mockAuthConnector,
    FakePayeAuthAction.mcc
  )

  val pertaxAuthAction = new PertaxAuthActionImpl(
    mockAuthConnector,
    FakePertaxAuthAction.mcc,
    mockPertaxConnector,
    inject[ServiceUnavailableView]
  )

  val authJourney =
    new AuthJourneyImpl(inject[AuthAction], inject[SelfAssessmentAction], payeAuthAction, pertaxAuthAction)

  val controller = new Harness(authJourney)

  "A user with a Nino" must {
    "create an authenticated request if PertaxConnector returns an ACCESS_GRANTED code" in {
      val nino                                                                       = new Generator().nextNino.nino
      val retrievalResult: Future[Enrolments ~ Option[String] ~ Option[Credentials]] =
        Future.successful(Enrolments(Set.empty) ~ Some(nino) ~ Some(fakeCredentials))

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[Credentials]](any(), any())(any(), any())
      )
        .thenReturn(retrievalResult)

      when(mockPertaxConnector.pertaxAuth(any())(any()))
        .thenReturn(
          EitherT[Future, UpstreamErrorResponse, PertaxApiResponse](
            Future.successful(Right(PertaxApiResponse("ACCESS_GRANTED", "", None)))
          )
        )

      val result = controller.onPageLoad()(FakeRequest())
      status(result) mustBe OK
    }

    "create an authenticated request if PertaxConnector returns an NO_HMRC_PT_ENROLMENT code" in {
      val nino                                                                       = new Generator().nextNino.nino
      val retrievalResult: Future[Enrolments ~ Option[String] ~ Option[Credentials]] =
        Future.successful(Enrolments(Set.empty) ~ Some(nino) ~ Some(fakeCredentials))

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[Credentials]](any(), any())(any(), any())
      )
        .thenReturn(retrievalResult)

      when(mockPertaxConnector.pertaxAuth(any())(any()))
        .thenReturn(
          EitherT[Future, UpstreamErrorResponse, PertaxApiResponse](
            Future.successful(Right(PertaxApiResponse("NO_HMRC_PT_ENROLMENT", "", Some("/redirect"))))
          )
        )

      val result = controller.onPageLoad()(FakeRequest())
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe s"/redirect?redirectUrl="
    }

    "create an authenticated request if PertaxConnector returns an unrecognised code" in {
      val nino                                                                       = new Generator().nextNino.nino
      val retrievalResult: Future[Enrolments ~ Option[String] ~ Option[Credentials]] =
        Future.successful(Enrolments(Set.empty) ~ Some(nino) ~ Some(fakeCredentials))

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[Credentials]](any(), any())(any(), any())
      )
        .thenReturn(retrievalResult)

      when(mockPertaxConnector.pertaxAuth(any())(any()))
        .thenReturn(
          EitherT[Future, UpstreamErrorResponse, PertaxApiResponse](
            Future.successful(Right(PertaxApiResponse("", "", None)))
          )
        )

      val result = controller.onPageLoad()(FakeRequest())
      status(result) mustBe SEE_OTHER
      redirectLocation(
        result
      ).get mustBe controllers.paye.routes.PayeErrorController.notAuthorised.url
    }

    "create an authenticated request if PertaxConnector returns a Left" in {
      val nino                                                                       = new Generator().nextNino.nino
      val retrievalResult: Future[Enrolments ~ Option[String] ~ Option[Credentials]] =
        Future.successful(Enrolments(Set.empty) ~ Some(nino) ~ Some(fakeCredentials))

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[Credentials]](any(), any())(any(), any())
      )
        .thenReturn(retrievalResult)

      when(mockPertaxConnector.pertaxAuth(any())(any()))
        .thenReturn(
          EitherT[Future, UpstreamErrorResponse, PertaxApiResponse](
            Future.successful(Left(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR)))
          )
        )

      val result = controller.onPageLoad()(FakeRequest())
      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }
}
