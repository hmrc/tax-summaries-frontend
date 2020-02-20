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

import controllers.auth.{AuthConnector, PayeAuthAction, PayeAuthActionImpl}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.http.Status.SEE_OTHER
import play.api.mvc.{Action, AnyContent, Controller}
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, _}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import utils.RetrievalOps._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

class PayeAuthActionSpec extends UnitSpec with OneAppPerSuite with MockitoSugar {

  class BrokenAuthConnector(exception: Throwable) extends AuthConnector {
    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
      Future.failed(exception)
  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  val ggSignInUrl = "http://localhost:9025/gg/sign-in?continue=http://localhost:9217/paye/annual-tax-summary&continue=http%3A%2F%2Flocalhost%3A9217%2Fannual-tax-summary&origin=tax-summaries-frontend"

  val unauthorisedUrl = "/annual-tax-summary/not-authorised"

  implicit val timeout: FiniteDuration = 5 seconds

  class Harness(authAction: PayeAuthAction) extends Controller {
    def onPageLoad(): Action[AnyContent] = authAction { request => Ok(
        s"Nino: ${request.nino.nino}") }
  }

  "A user with a confidence level 200 and a Nino" should {
    "create an authenticated request" in {
      val nino =  new Generator().nextNino.nino
      val retrievalResult: Future[
        Option[String] ~ Option[String]] =
        Future.successful(
          Some("") ~ Some(nino)
        )

      when(mockAuthConnector
        .authorise[Option[String] ~ Option[String]](any(), any())(any(), any()))
        .thenReturn(retrievalResult)

      val authAction = new PayeAuthActionImpl(mockAuthConnector, app.configuration)
      val controller = new Harness(authAction)

      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) shouldBe OK
      contentAsString(result) should include(nino)
    }
  }

  "A user with a confidence level 200 and no Nino" should {
    "return 303 and be redirected to not authorised page" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(new InternalError))
      val authAction = new PayeAuthActionImpl(mockAuthConnector, app.configuration)
      val controller = new Harness(authAction)
      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get should endWith(unauthorisedUrl)
    }
  }

  "A user with no active session" should {
    "return 303 and be redirected to GG sign in page" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(new SessionRecordNotFound))
      val authAction = new PayeAuthActionImpl(mockAuthConnector, app.configuration)
      val controller = new Harness(authAction)
      val result = controller.onPageLoad()(FakeRequest("", ""))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get should endWith(ggSignInUrl)
    }
  }
}
