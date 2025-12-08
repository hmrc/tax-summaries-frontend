/*
 * Copyright 2025 HM Revenue & Customs
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

package common.controllers

import cats.data.EitherT
import common.controllers.auth.*
import common.models.requests.AuthenticatedRequest
import common.models.{AtsErrorResponse, SpendData, requests}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import common.services.GovernmentSpendService
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.time.CurrentTaxYear
import common.utils.TestConstants.*
import common.utils.{ControllerBaseSpec, TaxYearUtil}
import common.view_models.{Amount, GovernmentSpend}

import java.time.LocalDate
import scala.concurrent.Future

class ErrorControllerSpec extends ControllerBaseSpec with CurrentTaxYear {

  override def now: () => LocalDate = () => LocalDate.now()

  private val mockGovernmentSpendService: GovernmentSpendService = mock[GovernmentSpendService]
  private val mockTaxYearUtil                                    = mock[TaxYearUtil]

  private val fakeGovernmentSpend: GovernmentSpend =
    GovernmentSpend(
      currentTaxYearGovSpend,
      testUtr,
      List(
        ("Welfare", SpendData(Amount(2898.13, "GBP"), 23.5)),
        ("Health", SpendData(Amount(2898.13, "GBP"), 20.2)),
        ("StatePensions", SpendData(Amount(2898.13, "GBP"), 11.8)),
        ("Education", SpendData(Amount(2898.13, "GBP"), 12.8)),
        ("Defence", SpendData(Amount(2898.13, "GBP"), 5.3)),
        ("NationalDebtInterest", SpendData(Amount(2898.13, "GBP"), 5.3)),
        ("PublicOrderAndSafety", SpendData(Amount(2898.13, "GBP"), 4.3)),
        ("Transport", SpendData(Amount(2898.13, "GBP"), 4.3)),
        ("BusinessAndIndustry", SpendData(Amount(2898.13, "GBP"), 3.6)),
        ("GovernmentAdministration", SpendData(Amount(2898.13, "GBP"), 2.1)),
        ("HousingAndUtilities", SpendData(Amount(2898.13, "GBP"), 1.6)),
        ("Culture", SpendData(Amount(2898.13, "GBP"), 1.5)),
        ("Environment", SpendData(Amount(2898.13, "GBP"), 1.5)),
        ("OverseasAid", SpendData(Amount(2898.13, "GBP"), 1.2)),
        ("UkContributionToEuBudget", SpendData(Amount(2898.13, "GBP"), 1)),
        govSpendTotalTuple
      ),
      "Mr",
      "John",
      "Doe",
      Amount(23912.00, "GBP"),
      "0002",
      Amount(2000.00, "GBP")
    )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockGovernmentSpendService, mockTaxYearUtil)
    when(mockTaxYearUtil.isValidTaxYear(any())).thenReturn(true)
    ()
  }

  private def sut: ErrorController =
    new ErrorController(
      mockGovernmentSpendService,
      FakeAuthJourney,
      mcc,
      notAuthorisedView,
      howTaxIsSpentView,
      serviceUnavailableView,
      pageNotFoundTemplateView,
      mockTaxYearUtil
    )

  private val defaultRequest: AuthenticatedRequest[AnyContentAsEmpty.type] =
    requests.AuthenticatedRequest(
      userId = "userId",
      agentRef = None,
      saUtr = Some(SaUtr(testUtr)),
      nino = None,
      isAgentActive = false,
      confidenceLevel = ConfidenceLevel.L50,
      credentials = fakeCredentials,
      request = FakeRequest()
    )

  "ErrorController" when {
    "authorisedNoAts is called" must {
      "show the how tax was spent page" when {
        "the service returns the government spend data with utr" in {
          val response: Seq[(String, Double)] = fakeGovernmentSpend.govSpendAmountData.map { case (key, value) =>
            key -> value.percentage.toDouble
          }

          val serviceResponse: EitherT[Future, AtsErrorResponse, Seq[(String, Double)]] = EitherT.rightT(response)

          when(mockGovernmentSpendService.getGovernmentSpendFigures(any())(any(), any())) thenReturn serviceResponse

          implicit lazy val request: AuthenticatedRequest[AnyContentAsEmpty.type] = defaultRequest
          val result                                                              = sut.authorisedNoAts(currentTaxYearGovSpend)(request)
          val document                                                            = contentAsString(result)

          status(result) mustBe OK
          document mustBe contentAsString(howTaxIsSpentView(response, currentTaxYearGovSpend))
        }

        "the service returns the government spend data with nino" in {
          val response: Seq[(String, Double)] = fakeGovernmentSpend.govSpendAmountData.map { case (key, value) =>
            key -> value.percentage.toDouble
          }

          val serviceResponse: EitherT[Future, AtsErrorResponse, Seq[(String, Double)]] =
            EitherT.rightT(response)

          val ninoIdentifier = Some(testNino)

          when(mockGovernmentSpendService.getGovernmentSpendFigures(any())(any(), any())) thenReturn serviceResponse

          implicit lazy val request: AuthenticatedRequest[AnyContentAsEmpty.type] =
            defaultRequest.copy(saUtr = None, nino = ninoIdentifier)
          val result                                                              = sut.authorisedNoAts(currentTaxYearGovSpend)(request)
          val document                                                            = contentAsString(result)

          status(result) mustBe OK
          document mustBe contentAsString(howTaxIsSpentView(response, currentTaxYearGovSpend))
        }
      }

      "return forbidden request" when {
        "the service tries to access an invalid year" in {
          when(mockTaxYearUtil.isValidTaxYear(any())).thenReturn(false)
          val response: Seq[(String, Double)] = fakeGovernmentSpend.govSpendAmountData.map { case (key, value) =>
            key -> value.percentage.toDouble
          }

          val serviceResponse: EitherT[Future, AtsErrorResponse, Seq[(String, Double)]] =
            EitherT.rightT(response)

          when(mockGovernmentSpendService.getGovernmentSpendFigures(any())(any(), any())) thenReturn serviceResponse

          implicit lazy val request: AuthenticatedRequest[AnyContentAsEmpty.type] = defaultRequest

          val result   = sut.authorisedNoAts(currentTaxYearGovSpend)(request)
          val document = contentAsString(result)

          status(result) mustBe NOT_FOUND
          document mustBe contentAsString(pageNotFoundTemplateView())
        }
      }

      "return bad request" when {
        "the service throws an illegal argument exception" in {
          val response: EitherT[Future, AtsErrorResponse, Seq[(String, Double)]] =
            EitherT.leftT(AtsErrorResponse("some error occured"))

          when(mockGovernmentSpendService.getGovernmentSpendFigures(any())(any(), any())).thenReturn(response)

          implicit lazy val request: AuthenticatedRequest[AnyContentAsEmpty.type] = defaultRequest

          val result   = sut.authorisedNoAts(currentTaxYearGovSpend)(request)
          val document = contentAsString(result)

          status(result) mustBe INTERNAL_SERVER_ERROR
          document mustBe contentAsString(serviceUnavailableView())
        }
      }

      "return internal server error" when {
        "the service return an UpstreamErrorResponse" in {
          val response: EitherT[Future, AtsErrorResponse, Seq[(String, Double)]] =
            EitherT.leftT(AtsErrorResponse("some error occured"))
          when(mockGovernmentSpendService.getGovernmentSpendFigures(any())(any(), any())).thenReturn(response)

          implicit lazy val request: AuthenticatedRequest[AnyContentAsEmpty.type] = defaultRequest

          val result   = sut.authorisedNoAts(currentTaxYearGovSpend)(request)
          val document = contentAsString(result)

          status(result) mustBe INTERNAL_SERVER_ERROR
          document mustBe contentAsString(serviceUnavailableView()(implicitly, implicitly))
        }
      }
    }

    "authorisedNoTaxYear is called" must {
      "return not found" when {
        "the service throws an illegal argument exception" in {
          val response: EitherT[Future, AtsErrorResponse, Seq[(String, Double)]] =
            EitherT.leftT(AtsErrorResponse("some error occured"))

          when(mockGovernmentSpendService.getGovernmentSpendFigures(any())(any(), any())).thenReturn(response)

          implicit lazy val request: AuthenticatedRequest[AnyContentAsEmpty.type] = defaultRequest

          val result   = sut.authorisedNoTaxYear(request)
          val document = contentAsString(result)

          status(result) mustBe NOT_FOUND
          document mustBe contentAsString(pageNotFoundTemplateView())
        }
      }

      "return internal server error" when {
        "the service return an UpstreamErrorResponse" in {
          val response: EitherT[Future, AtsErrorResponse, Seq[(String, Double)]] =
            EitherT.leftT(AtsErrorResponse("some error occured"))

          when(mockGovernmentSpendService.getGovernmentSpendFigures(any())(any(), any())).thenReturn(response)

          implicit lazy val request: AuthenticatedRequest[AnyContentAsEmpty.type] = defaultRequest

          val result   = sut.authorisedNoAts(currentTaxYearGovSpend)(request)
          val document = contentAsString(result)

          status(result) mustBe INTERNAL_SERVER_ERROR
          document mustBe contentAsString(serviceUnavailableView()(implicitly, implicitly))
        }
      }
    }

    "notAuthorised is called" must {
      "show the not authorised view" in {
        implicit lazy val request: AuthenticatedRequest[AnyContentAsEmpty.type] = defaultRequest.copy(saUtr = None)
        val result                                                              = sut.notAuthorised()(request)
        val document                                                            = contentAsString(result)

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
