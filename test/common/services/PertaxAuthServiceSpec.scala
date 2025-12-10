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

package common.services

import cats.data.EitherT
import common.config.ApplicationConfig
import common.connectors.PertaxConnector
import common.models.{ErrorView, PertaxApiResponse}
import common.utils.BaseSpec
import common.views.MainTemplate
import common.views.html.errors.ServiceUnavailableView
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, UNAUTHORIZED}
import play.api.i18n.MessagesApi
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.Html
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.partials.HtmlPartial

import scala.concurrent.Future

class PertaxAuthServiceSpec extends BaseSpec {

  private val serviceUnavailableView: ServiceUnavailableView = inject[ServiceUnavailableView]
  private val mainTemplate: MainTemplate                     = inject[MainTemplate]
  private val mockPertaxConnector                            = mock[PertaxConnector]
  private val mockAuthConnector: DefaultAuthConnector        = mock[DefaultAuthConnector]
  private val messagesApi: MessagesApi                       = inject[MessagesApi]
  private val mockApplicationConfig                          = mock[ApplicationConfig]

  private val dummyRedirect = "/dummy"

  override def beforeEach(): Unit =
    reset(mockPertaxConnector)

  "authorise" must {
    "return None when access granted" in {
      val service = new PertaxAuthService(
        mockAuthConnector,
        messagesApi,
        mockPertaxConnector,
        serviceUnavailableView,
        mainTemplate,
        mockApplicationConfig
      )

      val response: Either[UpstreamErrorResponse, PertaxApiResponse] = Right(
        PertaxApiResponse(
          code = "ACCESS_GRANTED",
          message = "",
          errorView = None,
          redirect = None
        )
      )
      when(mockPertaxConnector.pertaxPostAuthorise()(any(), any())).thenReturn(EitherT(Future(response)))
      whenReady(service.authorise[AnyContent, Request[AnyContent]](FakeRequest())) { result =>
        result mustBe None
      }
    }

    "return redirect when NO_HMRC_PT_ENROLMENT" in {
      val service = new PertaxAuthService(
        mockAuthConnector,
        messagesApi,
        mockPertaxConnector,
        serviceUnavailableView,
        mainTemplate,
        mockApplicationConfig
      )

      val response: Either[UpstreamErrorResponse, PertaxApiResponse] = Right(
        PertaxApiResponse(
          code = "NO_HMRC_PT_ENROLMENT",
          message = "",
          errorView = None,
          redirect = Some(dummyRedirect)
        )
      )
      when(mockPertaxConnector.pertaxPostAuthorise()(any(), any())).thenReturn(EitherT(Future(response)))
      whenReady(service.authorise[AnyContent, Request[AnyContent]](FakeRequest())) { result =>
        result mustBe Some(Redirect(s"$dummyRedirect?redirectUrl=%2F"))
      }
    }

    "return redirect when CONFIDENCE_LEVEL_UPLIFT_REQUIRED" in {
      val service                 = new PertaxAuthService(
        mockAuthConnector,
        messagesApi,
        mockPertaxConnector,
        serviceUnavailableView,
        mainTemplate,
        mockApplicationConfig
      )
      val appName                 = "appName"
      val loginCallBackUrl        = "callBack"
      val iVUpliftFailureCallback = "iVUpliftFailureCallback"
      when(mockApplicationConfig.appName).thenReturn(appName)
      when(mockApplicationConfig.loginCallback).thenReturn(loginCallBackUrl)
      when(mockApplicationConfig.iVUpliftFailureCallback).thenReturn(iVUpliftFailureCallback)

      val response: Either[UpstreamErrorResponse, PertaxApiResponse] = Right(
        PertaxApiResponse(
          code = "CONFIDENCE_LEVEL_UPLIFT_REQUIRED",
          message = "",
          errorView = None,
          redirect = Some(dummyRedirect)
        )
      )
      when(mockPertaxConnector.pertaxPostAuthorise()(any(), any())).thenReturn(EitherT(Future(response)))
      whenReady(service.authorise[AnyContent, Request[AnyContent]](FakeRequest())) { result =>
        result mustBe Some(
          Redirect(
            s"$dummyRedirect?origin=$appName&confidenceLevel=200&completionURL=$loginCallBackUrl&failureURL=$iVUpliftFailureCallback"
          )
        )
      }
    }

    "return internal server error when CREDENTIAL_STRENGTH_UPLIFT_REQUIRED" in {
      val service                                                    = new PertaxAuthService(
        mockAuthConnector,
        messagesApi,
        mockPertaxConnector,
        serviceUnavailableView,
        mainTemplate,
        mockApplicationConfig
      )
      val response: Either[UpstreamErrorResponse, PertaxApiResponse] = Right(
        PertaxApiResponse(
          code = "CREDENTIAL_STRENGTH_UPLIFT_REQUIRED",
          message = "",
          errorView = None,
          redirect = Some(dummyRedirect)
        )
      )
      when(mockPertaxConnector.pertaxPostAuthorise()(any(), any())).thenReturn(EitherT(Future(response)))
      whenReady(service.authorise[AnyContent, Request[AnyContent]](FakeRequest())) { result =>
        result.get.header.status mustBe 500
      }
    }

    "return partial rendered when partial success response returned" in {
      val service = new PertaxAuthService(
        mockAuthConnector,
        messagesApi,
        mockPertaxConnector,
        serviceUnavailableView,
        mainTemplate,
        mockApplicationConfig
      )

      val response: Either[UpstreamErrorResponse, PertaxApiResponse] = Right(
        PertaxApiResponse(
          code = "code",
          message = "test",
          errorView = Some(ErrorView(dummyRedirect, OK)),
          redirect = None
        )
      )
      when(mockPertaxConnector.pertaxPostAuthorise()(any(), any())).thenReturn(EitherT(Future(response)))
      when(mockPertaxConnector.loadPartial(any())(any(), any()))
        .thenReturn(Future.successful(HtmlPartial.Success(Some("title"), Html("test html"))))
      whenReady(service.authorise[AnyContent, Request[AnyContent]](FakeRequest())) { result =>
        result.get.header.status mustBe OK
        val content = contentAsString(Future(result.get))
        content.contains("title") mustBe true
        content.contains("test html") mustBe true
      }
    }

    "return internal server error when partial failure response returned" in {
      val service = new PertaxAuthService(
        mockAuthConnector,
        messagesApi,
        mockPertaxConnector,
        serviceUnavailableView,
        mainTemplate,
        mockApplicationConfig
      )

      val response: Either[UpstreamErrorResponse, PertaxApiResponse] = Right(
        PertaxApiResponse(
          code = "code",
          message = "test",
          errorView = Some(ErrorView(dummyRedirect, OK)),
          redirect = None
        )
      )
      when(mockPertaxConnector.pertaxPostAuthorise()(any(), any())).thenReturn(EitherT(Future(response)))
      when(mockPertaxConnector.loadPartial(any())(any(), any()))
        .thenReturn(Future.successful(HtmlPartial.Failure(Some(INTERNAL_SERVER_ERROR))))
      whenReady(service.authorise[AnyContent, Request[AnyContent]](FakeRequest())) { result =>
        result.get.header.status mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return internal server error when some other response (right) returned" in {
      val service                                                    = new PertaxAuthService(
        mockAuthConnector,
        messagesApi,
        mockPertaxConnector,
        serviceUnavailableView,
        mainTemplate,
        mockApplicationConfig
      )
      val response: Either[UpstreamErrorResponse, PertaxApiResponse] = Right(
        PertaxApiResponse(
          code = "SOME_OTHER",
          message = "",
          errorView = None,
          redirect = None
        )
      )
      when(mockPertaxConnector.pertaxPostAuthorise()(any(), any())).thenReturn(EitherT(Future(response)))
      whenReady(service.authorise[AnyContent, Request[AnyContent]](FakeRequest())) { result =>
        result.get.header.status mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return internal server error when some other response (left) returned" in {
      val service                                                    = new PertaxAuthService(
        mockAuthConnector,
        messagesApi,
        mockPertaxConnector,
        serviceUnavailableView,
        mainTemplate,
        mockApplicationConfig
      )
      val response: Either[UpstreamErrorResponse, PertaxApiResponse] = Left(
        UpstreamErrorResponse("", 500)
      )
      when(mockPertaxConnector.pertaxPostAuthorise()(any(), any())).thenReturn(EitherT(Future(response)))
      whenReady(service.authorise[AnyContent, Request[AnyContent]](FakeRequest())) { result =>
        result.get.header.status mustBe INTERNAL_SERVER_ERROR
      }
    }

    "redirect to login when UNAUTHORIZED response is returned" in {
      val service = new PertaxAuthService(
        mockAuthConnector,
        messagesApi,
        mockPertaxConnector,
        serviceUnavailableView,
        mainTemplate,
        mockApplicationConfig
      )

      when(mockApplicationConfig.loginUrl).thenReturn("/login")
      when(mockApplicationConfig.loginCallback).thenReturn("/callback")
      when(mockApplicationConfig.appName).thenReturn("testApp")

      val response: Either[UpstreamErrorResponse, PertaxApiResponse] = Left(
        UpstreamErrorResponse("Unauthorized", UNAUTHORIZED)
      )
      when(mockPertaxConnector.pertaxPostAuthorise()(any(), any())).thenReturn(EitherT(Future(response)))

      whenReady(service.authorise[AnyContent, Request[AnyContent]](FakeRequest())) { result =>
        result mustBe Some(
          Redirect(
            "/login",
            Map(
              "continue_url" -> Seq("/callback"),
              "origin"       -> Seq("testApp")
            )
          )
        )
      }
    }

  }
}
