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

package controllers.auth

import config.ApplicationConfig
import controllers.auth.actions.PayeAuthActionImpl
import controllers.paye.routes
import models.admin.PAYEServiceToggle
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verifyNoInteractions, when}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Action, AnyContent, InjectedController}
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, _}
import services.PertaxAuthService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlag
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import utils.RetrievalOps._
import utils.TestConstants.fakeCredentials
import utils.{BaseSpec, TaxYearUtil}

import scala.concurrent.Future

class PayeAuthActionSpec extends BaseSpec {
  override implicit lazy val appConfig: ApplicationConfig = mock[ApplicationConfig]
  private val mockAuthConnector: DefaultAuthConnector     = mock[DefaultAuthConnector]
  private val mockPertaxAuthService                       = mock[PertaxAuthService]
  override val taxYear                                    = currentTaxYearForTesting
  private val mockTaxYearUtil                             = mock[TaxYearUtil]

  private class Harness(authAction: PayeAuthActionImpl) extends InjectedController {
    def onPageLoad(): Action[AnyContent] = authAction { request =>
      Ok(s"Nino: ${request.nino.nino} and Credentials: ${request.credentials.providerType}")
    }
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockPertaxAuthService)
    reset(appConfig)
    reset(mockFeatureFlagService)
    reset(mockTaxYearUtil)
    when(mockFeatureFlagService.get(ArgumentMatchers.eq(PAYEServiceToggle)))
      .thenReturn(Future.successful(FeatureFlag(PAYEServiceToggle, isEnabled = true)))
    when(mockPertaxAuthService.authorise(any())).thenReturn(Future.successful(None))
    when(mockTaxYearUtil.isValidTaxYear(any())).thenReturn(true)
    ()
  }

  "A user with a confidence level 200 and a Nino" must {
    "create an authenticated request with no IR-SA" in {
      val nino                                                                       = new Generator().nextNino.nino
      val retrievalResult: Future[Enrolments ~ Option[String] ~ Option[Credentials]] =
        Future.successful(Enrolments(Set.empty) ~ Some(nino) ~ Some(fakeCredentials))

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[Credentials]](any(), any())(any(), any())
      )
        .thenReturn(retrievalResult)

      val authAction =
        new PayeAuthActionImpl(
          mockAuthConnector,
          FakePayeAuthAction.mcc,
          mockPertaxAuthService,
          mockFeatureFlagService,
          mockTaxYearUtil,
          taxYear
        )
      val controller = new Harness(authAction)

      val result = controller.onPageLoad()(FakeRequest())
      status(result) mustBe OK
      contentAsString(result) must include(nino)
      contentAsString(result) must include("provider type")
    }
    "redirect to not found when tax year out of range" in {
      val nino                                                                       = new Generator().nextNino.nino
      val retrievalResult: Future[Enrolments ~ Option[String] ~ Option[Credentials]] =
        Future.successful(Enrolments(Set.empty) ~ Some(nino) ~ Some(fakeCredentials))

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[Credentials]](any(), any())(any(), any())
      )
        .thenReturn(retrievalResult)

      when(mockTaxYearUtil.isValidTaxYear(any())).thenReturn(false)

      val authAction =
        new PayeAuthActionImpl(
          mockAuthConnector,
          FakePayeAuthAction.mcc,
          mockPertaxAuthService,
          mockFeatureFlagService,
          mockTaxYearUtil,
          taxYear
        )
      val controller = new Harness(authAction)

      val result = controller.onPageLoad()(FakeRequest())
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.ErrorController.authorisedNoAts(taxYear).url)
    }
    "redirect to failure url when authorisation fails" in {

      when(mockPertaxAuthService.authorise(any())).thenReturn(Future.successful(Some(Redirect("/dummy"))))

      val nino                                                                       = new Generator().nextNino.nino
      val retrievalResult: Future[Enrolments ~ Option[String] ~ Option[Credentials]] =
        Future.successful(Enrolments(Set.empty) ~ Some(nino) ~ Some(fakeCredentials))

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[Credentials]](any(), any())(any(), any())
      )
        .thenReturn(retrievalResult)

      val authAction =
        new PayeAuthActionImpl(
          mockAuthConnector,
          FakePayeAuthAction.mcc,
          mockPertaxAuthService,
          mockFeatureFlagService,
          mockTaxYearUtil,
          taxYear
        )
      val controller = new Harness(authAction)

      val result = controller.onPageLoad()(FakeRequest())

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some("/dummy")
    }
  }

  "A user visiting the service when it is not enabled" must {
    "be directed to the service unavailable page without calling auth" in {
      reset(mockAuthConnector)
      when(mockFeatureFlagService.get(ArgumentMatchers.eq(PAYEServiceToggle)))
        .thenReturn(Future.successful(FeatureFlag(PAYEServiceToggle, isEnabled = false)))
      val authAction =
        new PayeAuthActionImpl(
          mockAuthConnector,
          FakePayeAuthAction.mcc,
          mockPertaxAuthService,
          mockFeatureFlagService,
          mockTaxYearUtil,
          taxYear
        )

      val controller = new Harness(authAction)
      val result     = controller.onPageLoad()(FakeRequest())
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.PayeErrorController.serviceUnavailable.url
      verifyNoInteractions(mockAuthConnector)
    }
  }
}
