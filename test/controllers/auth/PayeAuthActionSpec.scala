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

import controllers.auth.actions.{PayeAuthAction, PayeAuthActionImpl}
import controllers.paye.routes
import org.mockito.ArgumentMatchers.any
import play.api.http.Status.SEE_OTHER
import play.api.mvc.{Action, AnyContent, InjectedController}
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, _}
import services.PertaxAuthService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import utils.BaseSpec
import utils.RetrievalOps._
import utils.TestConstants.fakeCredentials

import scala.concurrent.Future

class PayeAuthActionSpec extends BaseSpec {

  val mockAuthConnector: DefaultAuthConnector = mock[DefaultAuthConnector]

  val unauthorisedRoute: String = routes.PayeErrorController.notAuthorised.url

  private val mockPertaxAuthService = mock[PertaxAuthService]

  class Harness(authAction: PayeAuthAction) extends InjectedController {
    def onPageLoad(): Action[AnyContent] = authAction { request =>
      Ok(s"Nino: ${request.nino.nino} and Credentials: ${request.credentials.providerType}")
    }
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockPertaxAuthService)
    when(mockPertaxAuthService.authorise(any())).thenReturn(Future.successful(None))
  }

  "A user with a confidence level 200 and a Nino" must {
    "create an authenticated request with no IR-SA" in {
      val nino                                                                       = new Generator().nextNino.nino
      val retrievalResult: Future[Enrolments ~ Option[String] ~ Option[Credentials]] =
        Future.successful(Enrolments(Set.empty) ~ Some(nino) ~ Some(fakeCredentials))

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[Credentials]](any(), any())(any(), any())
      )
        .thenReturn(retrievalResult)

      val authAction = new PayeAuthActionImpl(mockAuthConnector, FakePayeAuthAction.mcc, mockPertaxAuthService)
      val controller = new Harness(authAction)

      val result = controller.onPageLoad()(FakeRequest())
      status(result) mustBe OK
      contentAsString(result) must include(nino)
      contentAsString(result) must include("provider type")
    }

    "A user will be redirected to the not authorised page" when {
      "they have no NINO" in {
        val retrievalResult: Future[Enrolments ~ Option[String] ~ Option[Credentials]] =
          Future.successful(Enrolments(Set.empty) ~ None ~ Some(fakeCredentials))

        when(
          mockAuthConnector
            .authorise[Enrolments ~ Option[String] ~ Option[Credentials]](any(), any())(any(), any())
        )
          .thenReturn(retrievalResult)

        val authAction = new PayeAuthActionImpl(mockAuthConnector, FakePayeAuthAction.mcc, mockPertaxAuthService)
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(FakeRequest())
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.paye.routes.PayeErrorController.notAuthorised.url)
      }

      "they have no credentials" in {
        val retrievalResult: Future[Enrolments ~ Option[String] ~ Option[Credentials]] =
          Future.successful(Enrolments(Set.empty) ~ Some("") ~ None)

        when(
          mockAuthConnector
            .authorise[Enrolments ~ Option[String] ~ Option[Credentials]](any(), any())(any(), any())
        )
          .thenReturn(retrievalResult)

        val authAction = new PayeAuthActionImpl(mockAuthConnector, FakePayeAuthAction.mcc, mockPertaxAuthService)
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(FakeRequest())
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.paye.routes.PayeErrorController.notAuthorised.url)
      }
    }
  }

  "A user with a confidence level 200 and no Nino" must {
    "return 303 and be redirected to not authorised page" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(new InternalError))
      val authAction = new PayeAuthActionImpl(mockAuthConnector, FakePayeAuthAction.mcc, mockPertaxAuthService)
      val controller = new Harness(authAction)
      val result     = controller.onPageLoad()(FakeRequest())
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get must endWith(unauthorisedRoute)
    }
  }

  "A user with NoActiveSession type exception" must {
    "return 303 and be redirected to GG sign in page" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(new SessionRecordNotFound))
      val authAction = new PayeAuthActionImpl(mockAuthConnector, FakePayeAuthAction.mcc, mockPertaxAuthService)
      val controller = new Harness(authAction)
      val result     = controller.onPageLoad()(FakeRequest())
      status(result) mustBe SEE_OTHER

      redirectLocation(result).get must startWith(appConfig.payeLoginUrl)
    }
  }

  "A user without credential strength strong" must {
    "return 303 and be redirected to not authorised page" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(IncorrectCredentialStrength()))
      val authAction = new PayeAuthActionImpl(mockAuthConnector, FakePayeAuthAction.mcc, mockPertaxAuthService)
      val controller = new Harness(authAction)
      val result     = controller.onPageLoad()(FakeRequest())
      status(result) mustBe SEE_OTHER

      redirectLocation(result).get must endWith(unauthorisedRoute)
    }
  }

  "A user visiting the service when it is shuttered" must {
//    "be directed to the service unavailable page without calling auth" in {
//      reset(mockAuthConnector)
//      when(appConfig.payeShuttered).thenReturn(true)
//      val authAction = new PayeAuthActionImpl(mockAuthConnector, FakePayeAuthAction.mcc, mockPertaxAuthService)
//
//      val controller = new Harness(authAction)
//      val result     = controller.onPageLoad()(FakeRequest())
//      status(result) mustBe SEE_OTHER
//      redirectLocation(result).get mustBe routes.PayeErrorController.serviceUnavailable.url
//      verifyZeroInteractions(mockAuthConnector)
//    }
  }
}
