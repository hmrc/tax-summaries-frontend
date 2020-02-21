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
import view_models.Amount
import view_models.paye.{PayeGovernmentSpend, SpendRow}

import scala.concurrent.Future
import scala.io.Source

class PayeGovernmentSpendControllerSpec  extends UnitSpec with MockitoSugar with JsonUtil with GuiceOneAppPerTest with ScalaFutures with I18nSupport with IntegrationPatience {

  implicit val hc = HeaderCarrier()
  override def messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]

  val taxYear = 2020
  val fakeAuthenticatedRequest = PayeAuthenticatedRequest("userId", testNino, FakeRequest("GET", s"?taxYear=$taxYear"))

  val expectedViewModel =  PayeGovernmentSpend(2019, List(
    SpendRow("welfare", 24.52, Amount(5863.22, "GBP")),
    SpendRow("health", 18.87, Amount(4512.19, "GBP")),
    SpendRow("pension", 12.12, Amount(2898.13, "GBP")),
    SpendRow("education", 13.15, Amount(3144.43, "GBP")),
    SpendRow("defence", 5.31, Amount(1269.73, "GBP")),
    SpendRow("national_debt_interest", 7.00, Amount(1673.84, "GBP")),
    SpendRow("transport", 2.95, Amount(705.4, "GBP")),
    SpendRow("criminal_justice", 4.40, Amount(1052.13, "GBP")),
    SpendRow("business_and_industry", 2.74, Amount(655.19, "GBP")),
    SpendRow("government_administration", 2.05, Amount(490.2, "GBP")),
    SpendRow("housing_and_utilities", 1.64, Amount(392.16, "GBP")),
    SpendRow("environment", 1.66, Amount(396.94, "GBP")),
    SpendRow("culture", 1.69, Amount(404.11, "GBP")),
    SpendRow("overseas_aid", 1.15, Amount(274.99, "GBP")),
    SpendRow("uk_contribution_to_eu_budget", 0.75, Amount(179.34, "GBP")))
    , totalAmount = Amount(4512.00,"GBP"))

  class TestController extends PayeGovernmentSpendController {

    override val payeAuthAction: PayeAuthAction = FakePayeAuthAction
    override val payeAtsService = mock[PayeAtsService]
    override val payeYear = 2019

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

      document.title should include(Messages("paye.ats.treasury_spending.title")+ Messages("generic.to_from", (taxYear-1).toString, taxYear.toString))
    }

    "have correct data for 2019" in new TestController {

      when(payeAtsService.getPayeATSData(eqTo(testNino), eqTo(2019))(any[HeaderCarrier]))
        .thenReturn(Right(expectedResponse.as[PayeAtsData]))

      val result = Future.successful(show(fakeAuthenticatedRequest))

      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("welfare").text() shouldBe "Welfare (24.52%)"
//      document.getElementById("health").text() shouldBe "Health (18.87%)"
//      document.select("#health").text() should include("18.87%")
//      document.select("#education + td").text() shouldBe "£3,144.43"
//      document.select("#education").text() should include("13.15%")
//      document.select("#pension + td").text() shouldBe "£2,898.13"
//      document.select("#pension").text() should include("12.12%")
//      document.select("#national_debt_interest + td").text() shouldBe "£1,673.84"
//      document.select("#national_debt_interest").text() should include("7.0%")
//      document.select("#defence + td").text() shouldBe "£1,269.73"
//      document.select("#defence").text() should include("5.31%")
//      document.select("#criminal_justice + td").text() shouldBe "£1,052.13"
//      document.select("#criminal_justice").text() should include("4.4%")
//      document.select("#transport + td").text() shouldBe "£705.40"
//      document.select("#transport").text() should include("2.95%")
//      document.select("#business_and_industry + td").text() shouldBe "£655.19"
//      document.select("#business_and_industry").text() should include("2.74%")
//      document.select("#government_administration + td").text() shouldBe "£490.20"
//      document.select("#government_administration").text() should include("2.05%")
//      document.select("#Culture + td").text() shouldBe "£404.11"
//      document.select("#Culture").text() should include("1.69%")
//      document.select("#Environment + td").text() shouldBe "£396.94"
//      document.select("#Environment").text() should include("1.66%")
//      document.select("#HousingAndUtilities + td").text() shouldBe "£392.16"
//      document.select("#HousingAndUtilities").text() should include("1.64%")
//      document.select("#overseas_aid + td").text() shouldBe "£274.99"
//      document.select("#overseas_aid").text() should include("1.15%")
//      document.select("#uk_contribution_to_eu_budget + td").text() shouldBe "£179.34"
//      document.select("#uk_contribution_to_eu_budget").text() should include("0.75%")
//      document.select("#gov-spend-total + td").text() shouldBe "£23,912.00"
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
