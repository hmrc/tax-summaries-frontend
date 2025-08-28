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

package utils

import controllers.auth.requests
import controllers.auth.requests.AuthenticatedRequest
import play.api.i18n
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers.{stubBodyParser, stubControllerComponents, stubMessagesApi}
import services.PayeAtsService
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.domain.SaUtr
import utils.TestConstants.{testNino, testUtr}
import view_models._
import views.html._
import views.html.errors._

import scala.concurrent.ExecutionContext

trait ControllerBaseSpec extends BaseSpec {

  private val messagesActionBuilder: MessagesActionBuilder =
    new DefaultMessagesActionBuilderImpl(stubBodyParser[AnyContent](), stubMessagesApi())
  private val cc: ControllerComponents                     = stubControllerComponents()

  lazy val mcc: MessagesControllerComponents = DefaultMessagesControllerComponents(
    messagesActionBuilder,
    DefaultActionBuilder(stubBodyParser[AnyContent]()),
    cc.parsers,
    fakeApplication().injector.instanceOf[MessagesApi],
    cc.langs,
    cc.fileMimeTypes,
    ExecutionContext.global
  )

  implicit lazy val lang: Lang                 = Lang(java.util.Locale.getDefault)
  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mcc.messagesApi)

  val mockPayeAtsService: PayeAtsService                      = mock[PayeAtsService]
  lazy val taxFreeAmountView: TaxFreeAmountView               = inject[TaxFreeAmountView]
  lazy val genericErrorView: GenericErrorView                 = inject[GenericErrorView]
  lazy val atsMergePageView: AtsMergePageView                 = inject[AtsMergePageView]
  lazy val tokenErrorView: TokenErrorView                     = inject[TokenErrorView]
  lazy val taxsMainView: TaxsMainView                         = inject[TaxsMainView]
  lazy val capitalGainsView: CapitalGainsView                 = inject[CapitalGainsView]
  lazy val notAuthorisedView: NotAuthorisedView               = inject[NotAuthorisedView]
  lazy val howTaxIsSpentView: HowTaxIsSpentView               = inject[HowTaxIsSpentView]
  lazy val serviceUnavailableView: ServiceUnavailableView     = inject[ServiceUnavailableView]
  lazy val pageNotFoundTemplateView: PageNotFoundTemplateView = inject[PageNotFoundTemplateView]
  lazy val governmentSpendingView: GovernmentSpendingView     = inject[GovernmentSpendingView]
  lazy val incomeBeforeTaxView: IncomeBeforeTaxView           = inject[IncomeBeforeTaxView]
  lazy val summaryView: SummaryView                           = inject[SummaryView]
  lazy val nicsView: NicsView                                 = inject[NicsView]

  val fakeCredentials = new Credentials("provider ID", "provider type")

  lazy val request: AuthenticatedRequest[AnyContentAsEmpty.type] = requests.AuthenticatedRequest(
    "userId",
    None,
    Some(SaUtr(testUtr)),
    Some(testNino),
    isAgentActive = false,
    ConfidenceLevel.L50,
    fakeCredentials,
    FakeRequest("GET", s"?taxYear=$taxYear")
  )

  lazy val badRequest: AuthenticatedRequest[AnyContentAsEmpty.type] = requests.AuthenticatedRequest(
    "userId",
    None,
    Some(SaUtr(testUtr)),
    None,
    isAgentActive = false,
    ConfidenceLevel.L50,
    fakeCredentials,
    FakeRequest("GET", s"?taxYear=${currentTaxYearForTesting}5")
  )

  protected val totalIncomeTaxModel: IncomeTaxAndNI = IncomeTaxAndNI(
    year = taxYear,
    utr = testUtr,
    employeeNicAmount = Amount(1200, "GBP"),
    totalIncomeTaxAndNics = Amount(1400, "GBP"),
    yourTotalTax = Amount(1800, "GBP"),
    totalTaxFree = Amount(9440, "GBP"),
    totalTaxFreeAllowance = Amount(9740, "GBP"),
    yourIncomeBeforeTax = Amount(11600, "GBP"),
    totalIncomeTaxAmount = Amount(372, "GBP"),
    totalCapitalGainsTax = Amount(5500, "GBP"),
    taxableGains = Amount(20000, "GBP"),
    cgTaxPerCurrencyUnit = Amount(0.1234, "GBP"),
    nicsAndTaxPerCurrencyUnit = Amount(0.5678, "GBP"),
    totalCgTaxRate = Rate("12.34%"),
    nicsAndTaxRate = Rate("56.78%"),
    startingRateForSavings = Amount(110, "GBP"),
    startingRateForSavingsAmount = Amount(140, "GBP"),
    basicRateIncomeTax = Amount(1860, "GBP"),
    basicRateIncomeTaxAmount = Amount(372, "GBP"),
    higherRateIncomeTax = Amount(130, "GBP"),
    higherRateIncomeTaxAmount = Amount(70, "GBP"),
    additionalRateIncomeTax = Amount(80, "GBP"),
    additionalRateIncomeTaxAmount = Amount(60, "GBP"),
    ordinaryRate = Amount(100, "GBP"),
    ordinaryRateAmount = Amount(50, "GBP"),
    upperRate = Amount(30, "GBP"),
    upperRateAmount = Amount(120, "GBP"),
    additionalRate = Amount(10, "GBP"),
    additionalRateAmount = Amount(40, "GBP"),
    otherAdjustmentsIncreasing = Amount(90, "GBP"),
    marriageAllowanceReceivedAmount = Amount(0, "GBP"),
    otherAdjustmentsReducing = Amount(-20, "GBP"),
    ScottishTax.empty,
    totalIncomeTax = Amount(372, "GBP"),
    scottishIncomeTax = Amount(100, "GBP"),
    welshIncomeTax = Amount(100, "GBP"),
    SavingsTax.empty,
    incomeTaxStatus = "0002",
    startingRateForSavingsRateRate = Rate("10%"),
    basicRateIncomeTaxRateRate = Rate("20%"),
    higherRateIncomeTaxRateRate = Rate("40%"),
    additionalRateIncomeTaxRateRate = Rate("45%"),
    ordinaryRateTaxRateRate = Rate("10%"),
    upperRateRateRate = Rate("32.5%"),
    additionalRateRateRate = Rate("37.5%"),
    ScottishRates.empty,
    SavingsRates.empty,
    "Mr",
    "forename",
    "surname"
  )
}
