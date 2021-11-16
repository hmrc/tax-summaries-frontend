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

import config.ApplicationConfig
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.test.Injecting
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext

trait BaseSpec
    extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with BeforeAndAfterEach with MockitoSugar with Injecting
    with ScalaFutures with IntegrationPatience {

  class FakeAppConfig extends ApplicationConfig(inject[ServicesConfig], inject[Configuration]) {
    override val currentTaxYearSpendData: Boolean = false
  }

  implicit lazy val appConfig: FakeAppConfig = new FakeAppConfig

  val taxYear: Int = appConfig.taxYear

  implicit lazy val ec = inject[ExecutionContext]
}
