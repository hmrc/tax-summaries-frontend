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
import controllers.auth.FakeAuthAction.mcc
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc._
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.test.UnitSpec
import utils.RetrievalOps._
import utils.TestConstants

import scala.concurrent.{ExecutionContext, Future}

class RoutingActionSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar with TestConstants {

  val mockAuthConnector: DefaultAuthConnector = mock[DefaultAuthConnector]

  implicit lazy val appConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val ec = app.injector.instanceOf[ExecutionContext]

  val content = "Block completed"

  class Harness(routingAction: RoutingActionImpl) extends InjectedController {
    def onPageLoad(): Action[AnyContent] = routingAction { request: Request[_] =>
      Ok(content)
    }
  }

  "RoutingAction" should {

    "default to the given block (continue to ATS SA)" when {

      "a user only has an IR-SA enrolment" in {

        val retrievalResult: Future[Enrolments ~ Option[String]] =
          Future.successful(
            Enrolments(Set(Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", testUtr)), "Activated", None))) ~ None
          )

        when(mockAuthConnector.authorise[Enrolments ~ Option[String]](any(), any())(any(), any())) thenReturn retrievalResult

        val routingAction = new RoutingActionImpl(mockAuthConnector, mcc)
        val controller = new Harness(routingAction)

        val result = controller.onPageLoad()(FakeRequest("", ""))
        status(result) shouldBe OK
        contentAsString(result) shouldBe content
      }

      "a user has an IR-SA enrolment and a nino" in {

        val retrievalResult: Future[Enrolments ~ Option[String]] =
          Future.successful(
            Enrolments(Set(Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", testUtr)), "Activated", None))) ~ Some(
              testNino.nino)
          )

        when(mockAuthConnector.authorise[Enrolments ~ Option[String]](any(), any())(any(), any())) thenReturn retrievalResult

        val routingAction = new RoutingActionImpl(mockAuthConnector, mcc)
        val controller = new Harness(routingAction)

        val result = controller.onPageLoad()(FakeRequest("", ""))
        status(result) shouldBe OK
        contentAsString(result) shouldBe content
      }
    }

    "redirect a user to PAYE ATS" when {

      "a user has a nino without an IR-SA enrolement" in {

        val retrievalResult: Future[Enrolments ~ Option[String]] =
          Future.successful(Enrolments(Set[Enrolment]()) ~ Some(testNino.nino))

        when(mockAuthConnector.authorise[Enrolments ~ Option[String]](any(), any())(any(), any())) thenReturn retrievalResult

        val routingAction = new RoutingActionImpl(mockAuthConnector, mcc)
        val controller = new Harness(routingAction)

        val result = controller.onPageLoad()(FakeRequest("", ""))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.paye.routes.PayeMultipleYearsController.onPageLoad().url)

      }
    }

    "redirect to the no ATS page" when {

      "a user had no nino or IR-SA enrolment" in {

        val retrievalResult: Future[Enrolments ~ Option[String]] =
          Future.successful(Enrolments(Set[Enrolment]()) ~ None)

        when(mockAuthConnector.authorise[Enrolments ~ Option[String]](any(), any())(any(), any())) thenReturn retrievalResult

        val routingAction = new RoutingActionImpl(mockAuthConnector, mcc)
        val controller = new Harness(routingAction)

        val result = controller.onPageLoad()(FakeRequest("", ""))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.ErrorController.authorisedNoAts().url)
      }
    }

    "redirect to the not authorised page" when {

      "auth throws InsufficientEnrolments" in {

        when(mockAuthConnector.authorise[Enrolments ~ Option[String]](any(), any())(any(), any())) thenReturn Future
          .failed(InsufficientEnrolments("Oops"))

        val routingAction = new RoutingActionImpl(mockAuthConnector, mcc)
        val controller = new Harness(routingAction)

        val result = controller.onPageLoad()(FakeRequest("", ""))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.ErrorController.notAuthorised().url)
      }
    }

    "redirect to GG login" when {

      "auth throws NoActiveSession" in {

        val ggSignInUrl =
          "http://localhost:9553/bas-gateway/sign-in?continue_url=http://localhost:9217/annual-tax-summary&continue=http%3A%2F%2Flocalhost%3A9217%2Fannual-tax-summary&origin=tax-summaries-frontend"

        when(mockAuthConnector.authorise(any(), any())(any(), any()))
          .thenReturn(Future.failed(new SessionRecordNotFound))

        val routingAction = new RoutingActionImpl(mockAuthConnector, mcc)
        val controller = new Harness(routingAction)

        val result = controller.onPageLoad()(FakeRequest("", ""))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should endWith(ggSignInUrl)
      }
    }
  }
}
