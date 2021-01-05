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

package utils

import com.typesafe.config.ConfigException
import config.ApplicationConfig
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec

class CategoriesUtilsSpec extends UnitSpec with MockitoSugar {

  val appConfig = mock[ApplicationConfig]

  "SwapDataUtils" should {

    "swap the data when categories are returned from config" in {

      val taxYear = 2020

      when(
        appConfig
          .spendCategories(taxYear))
        .thenReturn(List("A", "C", "B", "D"))

      val spendData = List("A" -> 1.5, "B" -> 1.20, "C" -> 1.20, "D" -> 0.5)

      val expectedResponse = List("A" -> 1.5, "C" -> 1.20, "B" -> 1.20, "D" -> 0.5)

      val result = CategoriesUtils.reorderCategories(appConfig, taxYear, spendData)

      result shouldBe expectedResponse
    }

    "not swap the data when categories are not returned from config" in {

      val taxYear = 2018

      when(
        appConfig
          .spendCategories(taxYear))
        .thenThrow(new ConfigException.Missing(s"categoryOrder.$taxYear"))

      val spendData = List("A" -> 1.5, "B" -> 1.20, "C" -> 1.20, "D" -> 0.5)

      val result = CategoriesUtils.reorderCategories(appConfig, taxYear, spendData)

      result shouldBe spendData
    }
  }

}
