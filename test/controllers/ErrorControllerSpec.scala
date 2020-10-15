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

package controllers

import java.time.LocalDate

import controllers.auth._
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.OK
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContent, BodyParser, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.GovernmentSpendService
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.time.CurrentTaxYear
import utils.TestConstants.{testUtr, _}

import scala.concurrent.{ExecutionContext, Future}

class ErrorControllerSpec extends ControllerBaseSpec with MockitoSugar with CurrentTaxYear {

  override def now: () => LocalDate = () => LocalDate.now()

  val mockGovernmentSpendService: GovernmentSpendService = mock[GovernmentSpendService]

  class CustomAuthAction(utr: Option[SaUtr]) extends AuthAction with ControllerBaseSpec {
    override def parser: BodyParser[AnyContent] = mcc.parsers.defaultBodyParser
    override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] =
      block(AuthenticatedRequest("userId", None, utr, None, None, None, None, request))
    override protected def executionContext: ExecutionContext = ec
  }

  def sut(utr: Option[SaUtr] = Some(SaUtr(testUtr))) =
    new ErrorController(
      mockGovernmentSpendService,
      new CustomAuthAction(utr),
      FakeMinAuthAction,
      mcc,
      notAuthorisedView,
      howTaxIsSpentView,
      serviceUnavailableView)
  implicit lazy val messageApi = inject[MessagesApi]

  "ErrorController" should {

    "Show No ATS page" when {

      "the service returns the government spend data" in {

        val response: Seq[(String, Double)] = fakeGovernmentSpend.sortedSpendData.map {
          case (key, value) =>
            key -> value.percentage.toDouble
        }

        when(mockGovernmentSpendService.getGovernmentSpendDataV2(any(), any())(any(), any())) thenReturn Future
          .successful(response)

        implicit lazy val request =
          AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest())
        val result = sut().authorisedNoAts()(request)
        val document = contentAsString(result)

        status(result) shouldBe OK
        document shouldBe contentAsString(howTaxIsSpentView(response, current.previous.startYear))
      }
    }

    "return bad request" when {

      "the service throws an illegal argument exception" in {

        when(mockGovernmentSpendService.getGovernmentSpendDataV2(any(), any())(any(), any())) thenReturn Future.failed(
          new IllegalArgumentException("Oops"))

        implicit lazy val request =
          AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest())

        val result = sut(None).authorisedNoAts()(request)
        val document = contentAsString(result)

        status(result) shouldBe BAD_REQUEST
        document shouldBe contentAsString(serviceUnavailableView())
      }
    }

    "show the not authorised view" when {

      "notAuthorised is called" in {

        implicit lazy val request = AuthenticatedRequest("userId", None, None, None, None, None, None, FakeRequest())
        val result = sut().notAuthorised()(request)
        val document = contentAsString(result)

        status(result) shouldBe OK

        document shouldBe contentAsString(notAuthorisedView())
      }
    }

    "return internal server error" when {

      "the service throws another exception" in {

        when(mockGovernmentSpendService.getGovernmentSpendDataV2(any(), any())(any(), any())) thenReturn Future.failed(
          new Exception("Oops"))

        implicit lazy val request =
          AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest())

        val result = sut(None).authorisedNoAts()(request)
        val document = contentAsString(result)

        status(result) shouldBe INTERNAL_SERVER_ERROR
        document shouldBe contentAsString(serviceUnavailableView())
      }
    }
  }
}
