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

package controllers.auth

import config.ApplicationConfig
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.SEE_OTHER
import play.api.mvc.{Action, AnyContent, InjectedController}
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, _}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

class MinAuthActionSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar {

  val mockAuthConnector: DefaultAuthConnector = mock[DefaultAuthConnector]
  implicit lazy val appConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val ec = app.injector.instanceOf[ExecutionContext]

  class Harness(minAuthAction: MinAuthActionImpl) extends InjectedController {
    def onPageLoad(): Action[AnyContent] = minAuthAction { request =>
      Ok
    }
  }

  val ggSignInUrl =
    "http://localhost:9553/bas-gateway/sign-in?continue_url=http://localhost:9217/annual-tax-summary&continue=http%3A%2F%2Flocalhost%3A9217%2Fannual-tax-summary&origin=tax-summaries-frontend"
  implicit val timeout: FiniteDuration = 5 seconds

  "A user with no active session" should {
    "return 303 and be redirected to GG sign in page" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(new SessionRecordNotFound))
      val minAuthAction = new MinAuthActionImpl(mockAuthConnector, FakeMinAuthAction.mcc)
      val controller = new Harness(minAuthAction)
      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get should endWith(ggSignInUrl)
    }
  }

  "A user with insufficient enrolments" should {
    "be redirected to the Sorry there is a problem page" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(InsufficientEnrolments()))
      val minAuthAction = new MinAuthActionImpl(mockAuthConnector, FakeMinAuthAction.mcc)
      val controller = new Harness(minAuthAction)
      val result = controller.onPageLoad()(FakeRequest("", ""))

      whenReady(result.failed) { ex =>
        ex shouldBe an[InsufficientEnrolments]
      }
    }
  }

  "A user with a confidence level 50" should {
    "create a minimum authenticated request" in {
      val retrievalResult: Future[Option[String]] =
        Future.successful(Some(""))

      when(
        mockAuthConnector
          .authorise[Option[String]](any(), any())(any(), any()))
        .thenReturn(retrievalResult)

      val minAuthAction = new MinAuthActionImpl(mockAuthConnector, FakeMinAuthAction.mcc)
      val controller = new Harness(minAuthAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) shouldBe OK
    }
  }
}
