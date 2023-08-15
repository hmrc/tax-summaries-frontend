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

package services

import cats.data.EitherT
import connectors.MessageFrontendConnector
import models.admin.SCAWrapperToggle
import org.mockito.ArgumentMatchers.any
import play.api.http.Status.{OK, UNAUTHORIZED}
import play.api.test.FakeRequest
import play.api.test.Helpers.GET
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlag
import utils.BaseSpec

import scala.concurrent.Future
import scala.util.Random

class MessageFrontendServiceSpec extends BaseSpec {

  val mockMessageFrontendConnector: MessageFrontendConnector = mock[MessageFrontendConnector]

  val messageFrontendService = new MessageFrontendService(mockMessageFrontendConnector, mockFeatureFlagService)

  "MessageFrontendConnector getUnreadMessageCount" must {
    "return a future optional Int when returned a HttpResponse with valid json from MessageFrontendConnector" in {

      val messageCount = Random.between(1, 100)

      when(mockFeatureFlagService.get(org.mockito.ArgumentMatchers.eq(SCAWrapperToggle))) thenReturn Future
        .successful(
          FeatureFlag(SCAWrapperToggle, isEnabled = false)
        )

      when(mockMessageFrontendConnector.getUnreadMessageCount()(any(), any()))
        .thenReturn(
          EitherT[Future, UpstreamErrorResponse, HttpResponse](
            Future.successful(Right(HttpResponse.apply(OK, s"""{"count": $messageCount}""")))
          )
        )

      val result = messageFrontendService.getUnreadMessageCount(FakeRequest(GET, ""))
      result.futureValue mustBe Some(messageCount)
    }

    "return None when returned a HttpResponse with invalid json from MessageFrontendConnector" in {

      when(mockMessageFrontendConnector.getUnreadMessageCount()(any(), any()))
        .thenReturn(
          EitherT[Future, UpstreamErrorResponse, HttpResponse](
            Future.successful(Right(HttpResponse.apply(OK, s"""{"invalid": json}""")))
          )
        )

      val result = messageFrontendService.getUnreadMessageCount(FakeRequest(GET, ""))
      result.futureValue mustBe None
    }

    "return None optional Int when returned a UpstreamErrorResponse from MessageFrontendConnector" in {

      when(mockMessageFrontendConnector.getUnreadMessageCount()(any(), any()))
        .thenReturn(
          EitherT[Future, UpstreamErrorResponse, HttpResponse](
            Future.successful(Left(UpstreamErrorResponse.apply("Invalid request", UNAUTHORIZED)))
          )
        )

      val result = messageFrontendService.getUnreadMessageCount(FakeRequest(GET, ""))
      result.futureValue mustBe None
    }
  }
}
