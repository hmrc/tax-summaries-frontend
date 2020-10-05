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
import play.api.i18n
import play.api.i18n.{MessagesApi, MessagesImpl, _}
import play.api.mvc.{DefaultMessagesActionBuilderImpl, MessagesActionBuilder, _}
import play.api.test.Helpers.{stubBodyParser, stubControllerComponents, stubMessagesApi}
import play.api.test.{FakeRequest, Injecting}
import services.PayeAtsService
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import views.html.{IncomeBeforeTaxView, NicsView, SummaryView, _}
import views.html.errors.{GenericErrorView, ServiceUnavailableView, _}

import scala.concurrent.ExecutionContext

trait ControllerBaseSpec extends UnitSpec with GuiceOneAppPerSuite  with MockitoSugar with Injecting {

  private val messagesActionBuilder: MessagesActionBuilder = new DefaultMessagesActionBuilderImpl(stubBodyParser[AnyContent](), stubMessagesApi())
  private val cc: ControllerComponents = stubControllerComponents()

  val mcc: MessagesControllerComponents = DefaultMessagesControllerComponents(
    messagesActionBuilder,
    DefaultActionBuilder(stubBodyParser[AnyContent]()),
    cc.parsers,
    fakeApplication.injector.instanceOf[MessagesApi],
    cc.langs,
    cc.fileMimeTypes,
    ExecutionContext.global
  )

  implicit lazy val lang: Lang = Lang(java.util.Locale.getDefault)
  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mcc.messagesApi)

  val mockPayeAtsService: PayeAtsService = mock[PayeAtsService]
  implicit lazy val formPartialRetriever = inject[FormPartialRetriever]
  implicit lazy val appConfig = inject[ApplicationConfig]
  implicit val ec: ExecutionContext = mcc.executionContext

  lazy val taxFreeAmountView = inject[TaxFreeAmountView]
  lazy val genericErrorView = inject[GenericErrorView]
  lazy val tokenErrorView = inject[TokenErrorView]
  lazy val taxsMainView = inject[TaxsMainView]
  lazy val capitalGainsView = inject[CapitalGainsView]
  lazy val notAuthorisedView = inject[NotAuthorisedView]
  lazy val noAtsErrorView = inject[NoAtsErrorView]
  lazy val serviceUnavailableView = inject[ServiceUnavailableView]
  lazy val governmentSpendingView = inject[GovernmentSpendingView]
  lazy val incomeBeforeTaxView = inject[IncomeBeforeTaxView]
  lazy val taxsIndexView = inject[TaxsIndexView]
  lazy val totalIncomeTaxView = inject[TotalIncomeTaxView]
  lazy val summaryView = inject[SummaryView]
  lazy val nicsView = inject[NicsView]

}