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

package controllers

import controllers.auth.{AuthAction, _}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api.http.Status.OK
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContent, BodyParser, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.GovernmentSpendService
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Nino}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.time.CurrentTaxYear
import utils.ControllerBaseSpec
import utils.TestConstants.{testUtr, _}
import java.time.LocalDate

import org.mockito.Matchers

import scala.concurrent.{ExecutionContext, Future}

class ErrorControllerSpec extends ControllerBaseSpec with CurrentTaxYear {

  override def now: () => LocalDate = () => LocalDate.now()

  val mockGovernmentSpendService: GovernmentSpendService = mock[GovernmentSpendService]

  def sut(utr: Option[SaUtr] = Some(SaUtr(testUtr))) =
    new ErrorController(
      mockGovernmentSpendService,
      FakeAuthAction,
      new FakeMergePageAuthAction(true),
      FakeMinAuthAction,
      mcc,
      notAuthorisedView,
      howTaxIsSpentView,
      serviceUnavailableView
    )
  implicit lazy val messageApi = inject[MessagesApi]

  "ErrorController" when {

    "authorisedNoAts is called" must {

      "show the how tax was spent page" when {

        "the service returns the government spend data with utr" in {

          val response: Seq[(String, Double)] = fakeGovernmentSpend.sortedSpendData.map {
            case (key, value) =>
              key -> value.percentage.toDouble
          }

          val saUtrIdentifier = Some(SaUtr(testUtr))
          when(mockGovernmentSpendService.getGovernmentSpendFigures(any(), Matchers.eq(saUtrIdentifier))(any(), any())) thenReturn Future
            .successful(response)

          implicit lazy val request =
            AuthenticatedRequest(
              "userId",
              None,
              Some(SaUtr(testUtr)),
              None,
              true,
              ConfidenceLevel.L50,
              fakeCredentials,
              FakeRequest())
          val result = sut().authorisedNoAts(appConfig.taxYear)(request)
          val document = contentAsString(result)

          status(result) mustBe OK
          document mustBe contentAsString(howTaxIsSpentView(response, appConfig.taxYear))
        }

        "the service returns the government spend data with nino" in {
          val controller =
            new ErrorController(
              mockGovernmentSpendService,
              FakeAuthAction,
              new FakeMergePageAuthAction(false),
              FakeMinAuthAction,
              mcc,
              notAuthorisedView,
              howTaxIsSpentView,
              serviceUnavailableView
            )
          val response: Seq[(String, Double)] = fakeGovernmentSpend.sortedSpendData.map {
            case (key, value) =>
              key -> value.percentage.toDouble
          }
          val ninoIdentifier = Some(testNino)
          when(mockGovernmentSpendService.getGovernmentSpendFigures(any(), Matchers.eq(ninoIdentifier))(any(), any())) thenReturn Future
            .successful(response)
          implicit lazy val request =
            AuthenticatedRequest(
              "userId",
              None,
              None,
              ninoIdentifier,
              false,
              ConfidenceLevel.L50,
              fakeCredentials,
              FakeRequest())
          val result = controller.authorisedNoAts(appConfig.taxYear)(request)
          val document = contentAsString(result)

          status(result) mustBe OK
          document mustBe contentAsString(howTaxIsSpentView(response, appConfig.taxYear))
        }

      }

      "return bad request" when {

        "the service throws an illegal argument exception" in {

          when(mockGovernmentSpendService.getGovernmentSpendFigures(any(), any())(any(), any())) thenReturn Future
            .failed(new IllegalArgumentException("Oops"))

          implicit lazy val request =
            AuthenticatedRequest(
              "userId",
              None,
              Some(SaUtr(testUtr)),
              None,
              true,
              ConfidenceLevel.L50,
              fakeCredentials,
              FakeRequest())

          val result = sut(None).authorisedNoAts(appConfig.taxYear)(request)
          val document = contentAsString(result)

          status(result) mustBe BAD_REQUEST
          document mustBe contentAsString(serviceUnavailableView())
        }
      }

      "return internal server error" when {

        "the service throws another exception" in {

          when(mockGovernmentSpendService.getGovernmentSpendFigures(any(), any())(any(), any())) thenReturn Future
            .failed(new Exception("Oops"))

          implicit lazy val request =
            AuthenticatedRequest(
              "userId",
              None,
              Some(SaUtr(testUtr)),
              None,
              true,
              ConfidenceLevel.L50,
              fakeCredentials,
              FakeRequest())

          val result = sut(None).authorisedNoAts(appConfig.taxYear)(request)
          val document = contentAsString(result)

          status(result) mustBe INTERNAL_SERVER_ERROR
          document mustBe contentAsString(serviceUnavailableView())
        }
      }
    }

    "notAuthorised is called" must {

      "show the not authorised view" in {

        implicit lazy val request =
          AuthenticatedRequest("userId", None, None, None, true, ConfidenceLevel.L50, fakeCredentials, FakeRequest())
        val result = sut().notAuthorised()(request)
        val document = contentAsString(result)

        status(result) mustBe OK

        document mustBe contentAsString(notAuthorisedView())
      }
    }

    "serviceUnavailable is called" must {

      "show the service unavailable view" in {

        implicit val request = FakeRequest()
        val result = sut().serviceUnavailable()(request)
        val document = contentAsString(result)

        status(result) mustBe OK
        document mustBe contentAsString(serviceUnavailableView())
      }
    }
  }
}
