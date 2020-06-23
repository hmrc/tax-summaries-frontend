/*
 * Copyright 2020 HM Revenue & Customs
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

import config.ApplicationConfig
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{I18nSupport, MessagesApi, MessagesProvider}
import play.api.test.Injecting
import services.PayeAtsService
import uk.gov.hmrc.play.bootstrap.tools.Stubs
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext

trait ControllerBaseSpec extends UnitSpec with GuiceOneAppPerSuite  with MockitoSugar with  I18nSupport with Injecting {

  override def messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]
  implicit lazy val formPartialRetriever = inject[FormPartialRetriever]
  implicit lazy val appConfig = inject[ApplicationConfig]
  implicit lazy val ec = inject[ExecutionContext]
  implicit lazy val msgProvider = inject[MessagesProvider]
  val mcc = Stubs.stubMessagesControllerComponents()
  val mockPayeAtsService: PayeAtsService = mock[PayeAtsService]
}
