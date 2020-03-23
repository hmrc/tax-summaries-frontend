/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.auth.paye

import config.ApplicationConfig
import controllers.auth.{AuthConnector, PayeAuthAction, PayeAuthActionImpl}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.http.Status.SEE_OTHER
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Action, AnyContent, Controller}
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, _}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

class PayeAuthActionSpec extends UnitSpec with OneAppPerSuite with MockitoSugar {

  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "paye.login.url" -> "http://localhost:9025/gg/sign-in",
        "shuttering.paye" -> "false"
      )
      .build()

  val ggSignInUrl = fakeApplication.configuration.getString("paye.login.url").getOrElse("Config key not found")
  val identityVerificationServiceUrl = "http://localhost:9948/mdtp/uplift"

  val unauthorisedRoute = controllers.paye.routes.PayeErrorController.notAuthorised().url

  class Harness(authAction: PayeAuthAction) extends Controller {
    def onPageLoad(): Action[AnyContent] = authAction { request =>
      Ok(s"Nino: ${request.nino.nino}")
    }
  }

  "A user with a confidence level 200 and a Nino" should {
    "create an authenticated request" in {
      val nino = new Generator().nextNino.nino
      val retrievalResult: Future[Option[String]] = Future.successful(Some(nino))

      when(mockAuthConnector
        .authorise[Option[String]](any(), any())(any(), any()))
        .thenReturn(retrievalResult)

      val authAction = new PayeAuthActionImpl(mockAuthConnector, fakeApplication.configuration)
      val controller = new Harness(authAction)

      val result = controller.onPageLoad()(FakeRequest())
      status(result) shouldBe OK
      contentAsString(result) should include(nino)
    }
  }

  "A user with a confidence level 200 and no Nino" should {
    "return 303 and be redirected to not authorised page" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(new InternalError))
      val authAction = new PayeAuthActionImpl(mockAuthConnector, fakeApplication.configuration)
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
      val authAction = new PayeAuthActionImpl(mockAuthConnector, fakeApplication.configuration)
      val controller = new Harness(authAction)
      val result = controller.onPageLoad()(FakeRequest())
      status(result) shouldBe SEE_OTHER

      redirectLocation(result).get should startWith(ggSignInUrl)
    }
  }

  "A user with Insufficient confidence level type exception" should {
    "return 303 and be redirected to Identity verification service" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(InsufficientConfidenceLevel()))
      val authAction = new PayeAuthActionImpl(mockAuthConnector, fakeApplication.configuration)
      val controller = new Harness(authAction)
      val result = controller.onPageLoad()(FakeRequest())
      status(result) shouldBe SEE_OTHER

      redirectLocation(result).get should startWith(identityVerificationServiceUrl)
    }
  }

  "A user without credential strength strong" should {
    "return 303 and be redirected to not authorised page" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(IncorrectCredentialStrength()))
      val authAction = new PayeAuthActionImpl(mockAuthConnector, fakeApplication.configuration)
      val controller = new Harness(authAction)
      val result = controller.onPageLoad()(FakeRequest())
      status(result) shouldBe SEE_OTHER

      redirectLocation(result).get should endWith(unauthorisedRoute)
    }
  }


  "A user visiting the service when it is shuttered" should {
    "be directed to the service unavailable page without calling auth" in {
      reset(mockAuthConnector)
      val shutteredApplication = new GuiceApplicationBuilder()
        .configure(
          "login.paye.url" -> "http://localhost:9025/gg/sign-in?continue=http://localhost:9217/paye/annual-tax-summary",
          "shuttering.paye" -> "true"
        )
        .build()

      val authAction = new PayeAuthActionImpl(mockAuthConnector, shutteredApplication.configuration)
      val controller = new Harness(authAction)
      val result = controller.onPageLoad()(FakeRequest())
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe (controllers.paye.routes.PayeErrorController.serviceUnavailable().url)
      verifyZeroInteractions(mockAuthConnector)
    }
  }
}
