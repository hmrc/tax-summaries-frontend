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

package controllers.paye

import config.ApplicationConfig
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n
import play.api.i18n.{MessagesApi, MessagesImpl, _}
import play.api.mvc.{DefaultMessagesActionBuilderImpl, MessagesActionBuilder, _}
import play.api.test.Helpers.{stubBodyParser, stubControllerComponents, stubMessagesApi}
import play.api.test.Injecting
import services.PayeAtsService
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import views.html.errors.{GenericErrorView, ServiceUnavailableView, _}
import views.html.{IncomeBeforeTaxView, NicsView, SummaryView, _}

import scala.concurrent.ExecutionContext

trait AppConfigBaseSpec extends UnitSpec with GuiceOneAppPerSuite with Injecting {

  implicit lazy val appConfig = inject[ApplicationConfig]

  val taxYear: Int = appConfig.payeYear

}
