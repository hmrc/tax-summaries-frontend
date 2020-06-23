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
import play.api.i18n._
import play.api.test.Injecting
import services.PayeAtsService
import uk.gov.hmrc.play.bootstrap.tools.Stubs
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext

trait ControllerBaseSpec extends UnitSpec with GuiceOneAppPerSuite  with MockitoSugar with Injecting {

  val mcc = Stubs.stubMessagesControllerComponents()
  val mockPayeAtsService: PayeAtsService = mock[PayeAtsService]
  implicit lazy val formPartialRetriever = inject[FormPartialRetriever]
  implicit lazy val appConfig = inject[ApplicationConfig]
  implicit val ec: ExecutionContext = mcc.executionContext
  //implicit val lang: Lang = mcc.langs.availables.head
  implicit val lang: Lang = Lang(java.util.Locale.getDefault)
  implicit val messagesProvider: MessagesProvider = MessagesImpl(lang, mcc.messagesApi)
}