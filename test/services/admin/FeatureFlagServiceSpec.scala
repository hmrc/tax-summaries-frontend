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

package services.admin

import akka.Done
import models.admin.PertaxBackendToggle
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import play.api.cache.AsyncCacheApi
import repositories.admin.FeatureFlagRepository
import utils.BaseSpec

import scala.concurrent.Future
import scala.jdk.CollectionConverters._

class FeatureFlagServiceSpec extends BaseSpec {

  val mockFeatureFlagRepository: FeatureFlagRepository = mock[FeatureFlagRepository]
  val mockCache: AsyncCacheApi                         = mock[AsyncCacheApi]

  override def beforeEach(): Unit = {
    reset(mockFeatureFlagRepository)
    reset(mockCache)
  }

  val featureFlagService: FeatureFlagService = new FeatureFlagService(appConfig, mockFeatureFlagRepository, mockCache)

  "set" must {
    "set a feature flag" in {
      when(mockCache.remove(any())).thenReturn(Future.successful(Done))
      when(mockFeatureFlagRepository.setFeatureFlag(any(), any())).thenReturn(Future.successful(true))

      val result = featureFlagService.set(PertaxBackendToggle, true).futureValue

      result mustBe true

      val eventCaptor = ArgumentCaptor.forClass(classOf[String])
      verify(mockCache, times(2)).remove(eventCaptor.capture())
      verify(mockFeatureFlagRepository, times(1)).setFeatureFlag(any(), any())

      val arguments: List[String] = eventCaptor.getAllValues.asScala.toList
      arguments.sorted mustBe List(
        PertaxBackendToggle.toString,
        "*$*$allFeatureFlags*$*$"
      ).sorted
    }
  }

  "setAll" must {
    "set all the feature flags provided" in {
      when(mockCache.remove(any())).thenReturn(Future.successful(Done))
      when(mockFeatureFlagRepository.setFeatureFlags(any()))
        .thenReturn(
          Future.successful(
            ()
          )
        )

      val result: Unit = featureFlagService
        .setAll(
          Map(PertaxBackendToggle -> true)
        )
        .futureValue

      result mustBe ()

      val eventCaptor = ArgumentCaptor.forClass(classOf[String])
      verify(mockCache, times(2)).remove(eventCaptor.capture())
      verify(mockFeatureFlagRepository, times(1)).setFeatureFlags(any())

      val arguments: List[String] = eventCaptor.getAllValues.asScala.toList
      arguments.sorted mustBe List(
        PertaxBackendToggle.toString,
        "*$*$allFeatureFlags*$*$"
      ).sorted
    }

    "return false when failing to set all the feature flags provided" in {
      when(mockCache.remove(any())).thenReturn(Future.successful(Done))
      when(mockFeatureFlagRepository.setFeatureFlags(any()))
        .thenReturn(
          Future.successful(())
        )

      val result = featureFlagService
        .setAll(
          Map(PertaxBackendToggle -> true)
        )
        .futureValue

      result mustBe ()

      val eventCaptor = ArgumentCaptor.forClass(classOf[String])
      verify(mockCache, times(2)).remove(eventCaptor.capture())
      verify(mockFeatureFlagRepository, times(1)).setFeatureFlags(any())
    }
  }
}
