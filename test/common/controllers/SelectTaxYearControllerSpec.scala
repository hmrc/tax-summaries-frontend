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

import common.config.ApplicationConfig
import common.controllers.auth.FakeAuthJourney
import common.forms.AtsYearChoiceFormProvider
import common.models.admin.{PAYEServiceToggle, SelfAssessmentServiceToggle}
import common.models.requests.AuthenticatedRequest
import common.models.{AtsErrorResponse, requests}
import common.services.YearListViewModelService
import common.utils.ControllerBaseSpec
import common.utils.TestConstants.{testNino, testUtr}
import common.view_models.{AtsList, YearListViewModel}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlag

import scala.concurrent.Future

class SelectTaxYearControllerSpec extends ControllerBaseSpec with ScalaFutures with BeforeAndAfterEach {
  val mockYearListViewModelService: YearListViewModelService = mock[YearListViewModelService]
  val atsForms: AtsYearChoiceFormProvider                    = inject[AtsYearChoiceFormProvider]

  implicit lazy val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  val sut                                            = new SelectTaxYearController(
    mockYearListViewModelService,
    FakeAuthJourney,
    mcc,
    selectTaxYearView,
    genericErrorView,
    atsForms,
    mockFeatureFlagService
  )

  lazy implicit val authRequest: AuthenticatedRequest[AnyContentAsEmpty.type] = requests.AuthenticatedRequest(
    "userId",
    None,
    Some(SaUtr(testUtr)),
    Some(testNino),
    isAgentActive = false,
    ConfidenceLevel.L50,
    fakeCredentials,
    FakeRequest("GET", s"?taxYear=$currentTaxYearSA")
  )

  val successViewModel: YearListViewModel =
    YearListViewModel(
      AtsList("", "", "", List(currentTaxYearSA)),
      List(currentTaxYearSA),
      mockAppConfig,
      ConfidenceLevel.L50
    )

  override def beforeEach(): Unit = {
    reset(mockAppConfig, mockFeatureFlagService)

    when(mockFeatureFlagService.get(ArgumentMatchers.eq(SelfAssessmentServiceToggle)))
      .thenReturn(Future.successful(FeatureFlag(SelfAssessmentServiceToggle, isEnabled = true)))

    when(mockFeatureFlagService.get(ArgumentMatchers.eq(PAYEServiceToggle)))
      .thenReturn(Future.successful(FeatureFlag(PAYEServiceToggle, isEnabled = true)))
    ()
  }

  "SelectTaxYearController for onPageLoad" must {

    "return a 200 response when called without '?ref=PORTAL' and must not put TAXS_USER_TYPE in session" in {

      when(mockYearListViewModelService.getSaAndPayeYearList(any(), any())).thenReturn(Future(Right(successViewModel)))

      val result = sut.onPageLoad(authRequest)

      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.text() contains "Select the tax year"
      document.text() contains currentTaxYearSA

      session(result).get("TAXS_USER_TYPE") mustBe None
    }

    "return a 200 response when called with '?ref=PORTAL' and put TAXS_USER_TYPE in session" in {
      val agentRequest = requests.AuthenticatedRequest(
        "userId",
        None,
        Some(SaUtr(testUtr)),
        None,
        isAgentActive = true,
        ConfidenceLevel.L50,
        fakeCredentials,
        FakeRequest("GET", common.controllers.routes.SelectTaxYearController.onPageLoad.toString + "?ref=PORTAL")
      )

      when(mockYearListViewModelService.getSaAndPayeYearList(any(), any())).thenReturn(Future(Right(successViewModel)))

      val result = sut.onPageLoad(agentRequest)

      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.text() contains "Select the tax year"
      document.text() contains currentTaxYearSA

      session(result).get("TAXS_USER_TYPE") mustBe Some("PORTAL")

    }

    "redirect to serviceUnavailable page if sa and paye are not enabled" in {

      when(mockFeatureFlagService.get(ArgumentMatchers.eq(SelfAssessmentServiceToggle)))
        .thenReturn(Future.successful(FeatureFlag(SelfAssessmentServiceToggle, isEnabled = false)))

      when(mockFeatureFlagService.get(ArgumentMatchers.eq(PAYEServiceToggle)))
        .thenReturn(Future.successful(FeatureFlag(PAYEServiceToggle, isEnabled = false)))

      when(mockYearListViewModelService.getSaAndPayeYearList(any(), any())).thenReturn(Future(Right(successViewModel)))

      val result = sut.onPageLoad(authRequest)

      status(result) mustBe 303
      redirectLocation(result).get mustBe common.controllers.routes.ErrorController.serviceUnavailable.url
    }

    "not redirect to serviceUnavailable page if sa is disabled and paye is enabled" in {

      when(mockFeatureFlagService.get(ArgumentMatchers.eq(SelfAssessmentServiceToggle)))
        .thenReturn(Future.successful(FeatureFlag(SelfAssessmentServiceToggle, isEnabled = false)))

      when(mockFeatureFlagService.get(ArgumentMatchers.eq(PAYEServiceToggle)))
        .thenReturn(Future.successful(FeatureFlag(PAYEServiceToggle, isEnabled = true)))

      when(mockYearListViewModelService.getSaAndPayeYearList(any(), any())).thenReturn(Future(Right(successViewModel)))

      val result = sut.onPageLoad(authRequest)

      status(result) mustBe 200

      redirectLocation(result) mustBe None
    }

    "not redirect to serviceUnavailable page if sa is enabled and paye is disabled" in {

      when(mockFeatureFlagService.get(ArgumentMatchers.eq(SelfAssessmentServiceToggle)))
        .thenReturn(Future.successful(FeatureFlag(SelfAssessmentServiceToggle, isEnabled = true)))

      when(mockFeatureFlagService.get(ArgumentMatchers.eq(PAYEServiceToggle)))
        .thenReturn(Future.successful(FeatureFlag(PAYEServiceToggle, isEnabled = false)))

      when(mockYearListViewModelService.getSaAndPayeYearList(any(), any())).thenReturn(Future(Right(successViewModel)))

      val result = sut.onPageLoad(authRequest)

      status(result) mustBe 200

      redirectLocation(result) mustBe None
    }

    "redirect to genericErrorView page if service returns an error" in {

      when(mockYearListViewModelService.getSaAndPayeYearList(any(), any()))
        .thenReturn(Future(Left(AtsErrorResponse("some error"))))

      val result = sut.onPageLoad(authRequest)

      status(result) mustBe 500
      val document = contentAsString(result)
      document mustBe contentAsString(genericErrorView())
    }
  }

  "SelectTaxYearController for onSubmit" must {

    "return a success response and redirect to sa main page when selected sa tax year" in {

      when(mockYearListViewModelService.getSaAndPayeYearList(any(), any())).thenReturn(Future(Right(successViewModel)))

      val form             = atsForms.atsYearChoiceForm.bind(Map("year" -> s"SA-$currentTaxYearSA"))
      val requestWithQuery = AuthenticatedRequest(
        "userId",
        None,
        Some(SaUtr(testUtr)),
        Some(testNino),
        isAgentActive = false,
        ConfidenceLevel.L50,
        fakeCredentials,
        FakeRequest().withMethod("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      )

      val result = sut.onSubmit(requestWithQuery)

      status(result) mustBe 303
      redirectLocation(
        result
      ).get mustBe (sa.controllers.routes.AtsMainController.authorisedAtsMain.url + s"?taxYear=$currentTaxYearSA")

    }

    "return a success response and redirect to paye main page when selected paye tax year" in {

      when(mockYearListViewModelService.getSaAndPayeYearList(any(), any())).thenReturn(Future(Right(successViewModel)))

      val form             = atsForms.atsYearChoiceForm.bind(Map("year" -> s"PAYE-$currentTaxYearSA"))
      val requestWithQuery = AuthenticatedRequest(
        "userId",
        None,
        Some(SaUtr(testUtr)),
        Some(testNino),
        isAgentActive = false,
        ConfidenceLevel.L50,
        fakeCredentials,
        FakeRequest().withMethod("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      )

      val result = sut.onSubmit(requestWithQuery)

      status(result) mustBe 303
      redirectLocation(result).get mustBe paye.controllers.routes.PayeAtsMainController
        .show(currentTaxYearSA)
        .toString

    }

    "return a success response and redirect to no ats page when selected no ats tax year for a user with nino and utr" in {

      when(mockYearListViewModelService.getSaAndPayeYearList(any(), any())).thenReturn(Future(Right(successViewModel)))

      val form             = atsForms.atsYearChoiceForm.bind(Map("year" -> s"NoATS-$currentTaxYearSA"))
      val requestWithQuery = AuthenticatedRequest(
        "userId",
        None,
        Some(SaUtr(testUtr)),
        Some(testNino),
        isAgentActive = false,
        ConfidenceLevel.L50,
        fakeCredentials,
        FakeRequest().withMethod("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      )

      val result = sut.onSubmit(requestWithQuery)

      status(result) mustBe 303
      redirectLocation(result).get mustBe common.controllers.routes.ErrorController
        .authorisedNoAts(currentTaxYearSA)
        .toString

    }

    "return a success response and redirect to no ats page when selected no ats tax year for a user with nino and no utr" in {

      when(mockYearListViewModelService.getSaAndPayeYearList(any(), any())).thenReturn(Future(Right(successViewModel)))

      val form             = atsForms.atsYearChoiceForm.bind(Map("year" -> s"NoATS-$currentTaxYearSA"))
      val requestWithQuery = AuthenticatedRequest(
        "userId",
        None,
        None,
        Some(testNino),
        isAgentActive = false,
        ConfidenceLevel.L50,
        fakeCredentials,
        FakeRequest().withMethod("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      )

      val result = sut.onSubmit(requestWithQuery)

      status(result) mustBe 303
      redirectLocation(result).get mustBe common.controllers.routes.ErrorController
        .authorisedNoAts(currentTaxYearSA)
        .toString

    }

    "return a success response and redirect to no ats page when selected no ats tax year for a user with utr and no nino" in {

      when(mockYearListViewModelService.getSaAndPayeYearList(any(), any())).thenReturn(Future(Right(successViewModel)))

      val form             = atsForms.atsYearChoiceForm.bind(Map("year" -> s"NoATS-$currentTaxYearSA"))
      val requestWithQuery = AuthenticatedRequest(
        "userId",
        None,
        Some(SaUtr(testUtr)),
        None,
        isAgentActive = false,
        ConfidenceLevel.L50,
        fakeCredentials,
        FakeRequest().withMethod("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      )

      val result = sut.onSubmit(requestWithQuery)

      status(result) mustBe 303
      redirectLocation(result).get mustBe common.controllers.routes.ErrorController
        .authorisedNoAts(currentTaxYearSA)
        .toString

    }

    "return a success response and stay on the same page with form errors and display the error" in {

      when(mockYearListViewModelService.getSaAndPayeYearList(any(), any())).thenReturn(Future(Right(successViewModel)))

      val form             = atsForms.atsYearChoiceForm.bind(Map("year" -> ""))
      val requestWithQuery = requests.AuthenticatedRequest(
        "userId",
        None,
        Some(SaUtr(testUtr)),
        Some(testNino),
        isAgentActive = false,
        ConfidenceLevel.L50,
        fakeCredentials,
        FakeRequest().withMethod("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      )

      val result = sut.onSubmit(requestWithQuery)

      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.text() contains "There is a problem with the form"
      document.text() contains "Select an option for the tax year"
    }

    "return a success response and stay on the same page with form errors if the HTML has been changed to submit an invalid value" in {

      when(mockYearListViewModelService.getSaAndPayeYearList(any(), any())).thenReturn(Future(Right(successViewModel)))

      val form             = atsForms.atsYearChoiceForm.bind(Map("year" -> "nonsense value"))
      val requestWithQuery = requests.AuthenticatedRequest(
        "userId",
        None,
        Some(SaUtr(testUtr)),
        Some(testNino),
        isAgentActive = false,
        ConfidenceLevel.L50,
        fakeCredentials,
        FakeRequest().withMethod("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      )

      val result = sut.onSubmit(requestWithQuery)

      status(result) mustBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.text() contains "There is a problem with the form"
      document.text() contains "Select an option for the tax year"
    }
  }
}
