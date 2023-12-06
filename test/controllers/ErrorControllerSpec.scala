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

package controllers

import cats.data.EitherT
import controllers.auth._
import models.AtsErrorResponse
import org.mockito.ArgumentMatchers.any
import play.api.http.Status.OK
import play.api.i18n.MessagesApi
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.GovernmentSpendService
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.time.CurrentTaxYear
import utils.ControllerBaseSpec
import utils.TestConstants.{testUtr, _}

import java.time.LocalDate
import scala.concurrent.Future

class ErrorControllerSpec extends ControllerBaseSpec with CurrentTaxYear {

  override def now: () => LocalDate = () => LocalDate.now()

  val mockGovernmentSpendService: GovernmentSpendService = mock[GovernmentSpendService]

  override def beforeEach() = {
    super.beforeEach()
    reset(mockGovernmentSpendService)
  }

  def sut: ErrorController                  =
    new ErrorController(
      mockGovernmentSpendService,
      new FakeMergePageAuthAction(true),
      FakeMinAuthAction,
      mcc,
      notAuthorisedView,
      howTaxIsSpentView,
      serviceUnavailableView
    )
  implicit lazy val messageApi: MessagesApi = inject[MessagesApi]

  "ErrorController" when {

    "authorisedNoAts is called" must {

      "show the how tax was spent page" when {

        "the service returns the government spend data with utr" in {

          val response: Seq[(String, Double)] = fakeGovernmentSpend.govSpendAmountData.map { case (key, value) =>
            key -> value.percentage.toDouble
          }

          val serviceResponse: EitherT[Future, AtsErrorResponse, Seq[(String, Double)]] =
            EitherT.rightT(response)

          when(mockGovernmentSpendService.getGovernmentSpendFigures(any())(any(), any())) thenReturn serviceResponse

          implicit lazy val request =
            AuthenticatedRequest(
              "userId",
              None,
              Some(SaUtr(testUtr)),
              None,
              true,
              false,
              ConfidenceLevel.L50,
              fakeCredentials,
              FakeRequest(),
              None
            )
          val result                = sut.authorisedNoAts(fakeTaxYear)(request)
          val document              = contentAsString(result)

          status(result) mustBe OK
          document mustBe contentAsString(howTaxIsSpentView(response, fakeTaxYear))
        }

        "the service returns the government spend data with nino" in {
          val controller                      =
            new ErrorController(
              mockGovernmentSpendService,
              new FakeMergePageAuthAction(false),
              FakeMinAuthAction,
              mcc,
              notAuthorisedView,
              howTaxIsSpentView,
              serviceUnavailableView
            )
          val response: Seq[(String, Double)] = fakeGovernmentSpend.govSpendAmountData.map { case (key, value) =>
            key -> value.percentage.toDouble
          }

          val serviceResponse: EitherT[Future, AtsErrorResponse, Seq[(String, Double)]] =
            EitherT.rightT(response)

          val ninoIdentifier = Some(testNino)

          when(mockGovernmentSpendService.getGovernmentSpendFigures(any())(any(), any())) thenReturn serviceResponse

          implicit lazy val request =
            AuthenticatedRequest(
              "userId",
              None,
              None,
              ninoIdentifier,
              false,
              false,
              ConfidenceLevel.L50,
              fakeCredentials,
              FakeRequest(),
              None
            )
          val result                = controller.authorisedNoAts(fakeTaxYear)(request)
          val document              = contentAsString(result)

          status(result) mustBe OK
          document mustBe contentAsString(howTaxIsSpentView(response, fakeTaxYear))
        }

      }

      "return forbidden request" when {

        "the service tries to access a future year" in {

          val response: Seq[(String, Double)] = fakeGovernmentSpend.govSpendAmountData.map { case (key, value) =>
            key -> value.percentage.toDouble
          }

          val serviceResponse: EitherT[Future, AtsErrorResponse, Seq[(String, Double)]] =
            EitherT.rightT(response)

          when(mockGovernmentSpendService.getGovernmentSpendFigures(any())(any(), any())) thenReturn serviceResponse

          implicit lazy val request =
            AuthenticatedRequest(
              "userId",
              None,
              Some(SaUtr(testUtr)),
              None,
              true,
              false,
              ConfidenceLevel.L50,
              fakeCredentials,
              FakeRequest(),
              None
            )

          val result   = sut.authorisedNoAts(appConfig.taxYear + 1)(request)
          val document = contentAsString(result)

          status(result) mustBe FORBIDDEN
          document mustBe contentAsString(serviceUnavailableView())
        }

        "the service tries to access a year before the current year minus the max years to be displayed" in {

          val response: Seq[(String, Double)] = fakeGovernmentSpend.govSpendAmountData.map { case (key, value) =>
            key -> value.percentage.toDouble
          }

          val serviceResponse: EitherT[Future, AtsErrorResponse, Seq[(String, Double)]] =
            EitherT.rightT(response)

          when(mockGovernmentSpendService.getGovernmentSpendFigures(any())(any(), any())) thenReturn serviceResponse

          implicit lazy val request =
            AuthenticatedRequest(
              "userId",
              None,
              Some(SaUtr(testUtr)),
              None,
              true,
              false,
              ConfidenceLevel.L50,
              fakeCredentials,
              FakeRequest(),
              None
            )

          val result   = sut.authorisedNoAts(appConfig.taxYear - appConfig.maxTaxYearsTobeDisplayed - 1)(request)
          val document = contentAsString(result)

          status(result) mustBe FORBIDDEN
          document mustBe contentAsString(serviceUnavailableView())
        }

      }

      "return bad request" when {

        "the service throws an illegal argument exception" in {

          val response: EitherT[Future, AtsErrorResponse, Seq[(String, Double)]] =
            EitherT.leftT(AtsErrorResponse("some error occured"))

          when(mockGovernmentSpendService.getGovernmentSpendFigures(any())(any(), any())).thenReturn(response)

          implicit lazy val request =
            AuthenticatedRequest(
              "userId",
              None,
              Some(SaUtr(testUtr)),
              None,
              true,
              false,
              ConfidenceLevel.L50,
              fakeCredentials,
              FakeRequest(),
              None
            )

          val result   = sut.authorisedNoAts(appConfig.taxYear)(request)
          val document = contentAsString(result)

          status(result) mustBe INTERNAL_SERVER_ERROR
          document mustBe contentAsString(serviceUnavailableView())
        }
      }

      "return internal server error" when {
        "the service return an UpstreamErrorResponse" in {

          val response: EitherT[Future, AtsErrorResponse, Seq[(String, Double)]] =
            EitherT.leftT(AtsErrorResponse("some error occured"))

          def sutWithMockAppConfig =
            new ErrorController(
              mockGovernmentSpendService,
              new FakeMergePageAuthAction(true),
              FakeMinAuthAction,
              mcc,
              notAuthorisedView,
              howTaxIsSpentView,
              serviceUnavailableView
            )

          when(mockGovernmentSpendService.getGovernmentSpendFigures(any())(any(), any())).thenReturn(response)

          implicit lazy val request =
            AuthenticatedRequest(
              "userId",
              None,
              Some(SaUtr(testUtr)),
              None,
              true,
              false,
              ConfidenceLevel.L50,
              fakeCredentials,
              FakeRequest(),
              None
            )

          val result   = sutWithMockAppConfig.authorisedNoAts(taxYear)(request)
          val document = contentAsString(result)

          status(result) mustBe INTERNAL_SERVER_ERROR
          document mustBe contentAsString(serviceUnavailableView()(implicitly, implicitly))
        }
      }
    }

    "notAuthorised is called" must {

      "show the not authorised view" in {

        implicit lazy val request =
          AuthenticatedRequest(
            "userId",
            None,
            None,
            None,
            true,
            false,
            ConfidenceLevel.L50,
            fakeCredentials,
            FakeRequest(),
            None
          )
        val result                = sut.notAuthorised()(request)
        val document              = contentAsString(result)

        status(result) mustBe OK

        document mustBe contentAsString(notAuthorisedView())
      }
    }

    "serviceUnavailable is called" must {

      "show the service unavailable view" in {

        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
        val result                                                = sut.serviceUnavailable()(request)
        val document                                              = contentAsString(result)

        status(result) mustBe OK
        document mustBe contentAsString(serviceUnavailableView())
      }
    }
  }
}
