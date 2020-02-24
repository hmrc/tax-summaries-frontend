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

package controllers.paye

import controllers.auth.{FakePayeAuthAction, PayeAuthAction, PayeAuthenticatedRequest}
import models.PayeAtsData
import org.jsoup.Jsoup
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation}
import services.PayeAtsService
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec
import utils.JsonUtil
import utils.TestConstants.testNino

import scala.concurrent.Future
import scala.io.Source

class PayeGovernmentSpendControllerSpec  extends UnitSpec with MockitoSugar with JsonUtil with GuiceOneAppPerTest with ScalaFutures with I18nSupport with IntegrationPatience {

  implicit val hc = HeaderCarrier()
  override def messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]

  val taxYear = 2019
  val fakeAuthenticatedRequest = PayeAuthenticatedRequest("userId", testNino, FakeRequest("GET", "/annual-tax-summary/paye/treasury-spending"))

  class TestController extends PayeGovernmentSpendController {

    override val payeAuthAction: PayeAuthAction = FakePayeAuthAction
    override val payeAtsService = mock[PayeAtsService]
    override val payeYear = taxYear

    private def readJson(path: String) = {
      val resource = getClass.getResourceAsStream(path)
      Json.parse(Source.fromInputStream(resource).getLines().mkString)
    }

    val expectedResponse: JsValue = readJson("/paye_ats.json")
  }

  "Government spend controller" should {

    "return OK response" in new TestController {

      when(payeAtsService.getPayeATSData(eqTo(testNino), eqTo(2019))(any[HeaderCarrier]))
        .thenReturn(Right(expectedResponse.as[PayeAtsData]))

      val result = show(fakeAuthenticatedRequest)

      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))

      document.title should include(Messages("paye.ats.treasury_spending.title")+ Messages("generic.to_from", (taxYear -1).toString, taxYear.toString))
    }

    "have correct data for 2019" in new TestController {

      when(payeAtsService.getPayeATSData(eqTo(testNino), eqTo(2019))(any[HeaderCarrier]))
        .thenReturn(Right(expectedResponse.as[PayeAtsData]))

      val result = Future.successful(show(fakeAuthenticatedRequest))

      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("Welfare").text() shouldBe "Welfare (24.52%)"
      document.select("#Welfare + dd").text() shouldBe "£5,863"

      document.getElementById("Health").text() shouldBe "Health (18.87%)"
      document.select("#Health + dd").text() shouldBe "£4,512"

      document.getElementById("StatePensions").text() shouldBe "State Pensions (12.12%)"
      document.select("#StatePensions + dd").text() shouldBe "£2,898"

      document.getElementById("Education").text() shouldBe "Education (13.15%)"
      document.select("#Education + dd").text() shouldBe "£3,144"

      document.getElementById("Defence").text() shouldBe "Defence (5.31%)"
      document.select("#Defence + dd").text() shouldBe "£1,269"

      document.getElementById("NationalDebtInterest").text() shouldBe "National Debt Interest (7.0%)"
      document.select("#NationalDebtInterest + dd").text() shouldBe "£1,673"

      document.getElementById("Transport").text() shouldBe "Transport (2.95%)"
      document.select("#Transport + dd").text() shouldBe "£705"

      document.getElementById("PublicOrderAndSafety").text() shouldBe "Public Order and Safety (4.4%)"
      document.select("#PublicOrderAndSafety + dd").text() shouldBe "£1,052"

      document.getElementById("BusinessAndIndustry").text() shouldBe "Business and Industry (2.74%)"
      document.select("#BusinessAndIndustry + dd").text() shouldBe "£655"

      document.getElementById("GovernmentAdministration").text() shouldBe "Government Administration (2.05%)"
      document.select("#GovernmentAdministration + dd").text() shouldBe "£490"

      document.getElementById("HousingAndUtilities").text() shouldBe "Housing and Utilities, like street lighting (1.64%)"
      document.select("#HousingAndUtilities + dd").text() shouldBe "£392"

      document.getElementById("Environment").text() shouldBe "Environment (1.66%)"
      document.select("#Environment + dd").text() shouldBe "£396"

      document.getElementById("Culture").text() shouldBe "Culture, like sports, libraries, museums (1.69%)"
      document.select("#Culture + dd").text() shouldBe "£404"

      document.getElementById("OverseasAid").text() shouldBe "Overseas Aid (1.15%)"
      document.select("#OverseasAid + dd").text() shouldBe "£274"

      document.getElementById("UkContributionToEuBudget").text() shouldBe "UK Contribution to the EU Budget (0.75%)"
      document.select("#UkContributionToEuBudget + dd").text() shouldBe "£179"

      document.select("#TotalAmount + dd").text() shouldBe "£4,512"

      document
        .select("h1")
        .text shouldBe "How your tax was spent 6 April 2019 to 5 April 2020"
    }

    "return bad request and errors when receiving any errors from service" in new TestController {

      when(payeAtsService.getPayeATSData(eqTo(testNino), eqTo(2019))(any[HeaderCarrier]))
        .thenReturn(Left(HttpResponse(responseStatus = NOT_FOUND, responseJson = Some(Json.toJson(NOT_FOUND)))))

      val result = show(fakeAuthenticatedRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe controllers.routes.ErrorController.authorisedNoAts().url

    }
  }

}
