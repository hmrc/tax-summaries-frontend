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
import controllers.paye.routes
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.SEE_OTHER
import play.api.mvc.{Action, AnyContent, InjectedController}
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, _}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.domain.{Generator, SaUtrGenerator}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PayeAuthActionSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar {

  val mockAuthConnector: DefaultAuthConnector = mock[DefaultAuthConnector]
  implicit lazy val appConfig = app.injector.instanceOf[ApplicationConfig]

  val unauthorisedRoute = routes.PayeErrorController.notAuthorised().url

  class Harness(authAction: PayeAuthAction) extends InjectedController {
    def onPageLoad(): Action[AnyContent] = authAction { request =>
      Ok(s"isSa: ${request.isSa} and Nino: ${request.nino.nino}")
    }
  }

  "A user with a confidence level 200 and a Nino" should {
    "create an authenticated request with no IR-SA" in {
      val nino = new Generator().nextNino.nino
      val retrievalResult: Future[~[Enrolments, Option[String]]] =
        Future.successful(new ~(Enrolments(Set.empty), Some(nino)))

      when(
        mockAuthConnector
          .authorise[~[Enrolments, Option[String]]](any(), any())(any(), any()))
        .thenReturn(retrievalResult)

      val authAction = new PayeAuthActionImpl(mockAuthConnector, FakePayeAuthAction.mcc)
      val controller = new Harness(authAction)

      val result = controller.onPageLoad()(FakeRequest())
      status(result) shouldBe OK
      contentAsString(result) should include(nino)
      contentAsString(result) should include("false")
    }

    "create an authenticated request with IR-SA" in {
      val nino = new Generator().nextNino.nino
      val utr = new SaUtrGenerator().nextSaUtr.utr

      val retrievalResult: Future[~[Enrolments, Option[String]]] =
        Future.successful(
          new ~(Enrolments(Set(Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", utr)), "Activated"))), Some(nino)))

      when(
        mockAuthConnector
          .authorise[~[Enrolments, Option[String]]](any(), any())(any(), any()))
        .thenReturn(retrievalResult)

      val authAction = new PayeAuthActionImpl(mockAuthConnector, FakePayeAuthAction.mcc)
      val controller = new Harness(authAction)

      val result = controller.onPageLoad()(FakeRequest())
      status(result) shouldBe OK
      contentAsString(result) should include(nino)
      contentAsString(result) should include("true")
    }
  }

  "A user with a confidence level 200 and no Nino" should {
    "return 303 and be redirected to not authorised page" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(new InternalError))
      val authAction = new PayeAuthActionImpl(mockAuthConnector, FakePayeAuthAction.mcc)
      val controller = new Harness(authAction)
      val result = controller.onPageLoad()(FakeRequest())
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get should endWith(unauthorisedRoute)
    }
  }

  "A user with NoActiveSession type exception" should {
    "return 303 and be redirected to GG sign in page" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(new SessionRecordNotFound))
      val authAction = new PayeAuthActionImpl(mockAuthConnector, FakePayeAuthAction.mcc)
      val controller = new Harness(authAction)
      val result = controller.onPageLoad()(FakeRequest())
      status(result) shouldBe SEE_OTHER

      redirectLocation(result).get should startWith(appConfig.payeLoginUrl)
    }
  }

  "A user with Insufficient confidence level type exception" should {
    "return 303 and be redirected to Identity verification service" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(InsufficientConfidenceLevel()))
      val authAction = new PayeAuthActionImpl(mockAuthConnector, FakePayeAuthAction.mcc)
      val controller = new Harness(authAction)
      val result = controller.onPageLoad()(FakeRequest())
      status(result) shouldBe SEE_OTHER

      redirectLocation(result).get should startWith(appConfig.identityVerificationUpliftUrl)
    }
  }

  "A user without credential strength strong" should {
    "return 303 and be redirected to not authorised page" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(IncorrectCredentialStrength()))
      val authAction = new PayeAuthActionImpl(mockAuthConnector, FakePayeAuthAction.mcc)
      val controller = new Harness(authAction)
      val result = controller.onPageLoad()(FakeRequest())
      status(result) shouldBe SEE_OTHER

      redirectLocation(result).get should endWith(unauthorisedRoute)
    }
  }

  "A user visiting the service when it is shuttered" should {
    "be directed to the service unavailable page without calling auth" in {
      reset(mockAuthConnector)

      val authAction = new PayeAuthActionImpl(mockAuthConnector, FakePayeAuthAction.mcc) {
        override val payeShuttered: Boolean = true
      }

      val controller = new Harness(authAction)
      val result = controller.onPageLoad()(FakeRequest())
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.PayeErrorController.serviceUnavailable().url
      verifyZeroInteractions(mockAuthConnector)
    }
  }
}
