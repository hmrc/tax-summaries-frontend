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
import services.atsData.PayeAtsTestData
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec
import utils.JsonUtil
import utils.TestConstants.testNino

import scala.io.Source

class PayeYourIncomeAndTaxesControllerSpec  extends UnitSpec with MockitoSugar with JsonUtil with GuiceOneAppPerTest with ScalaFutures with I18nSupport with IntegrationPatience {

  implicit val hc = HeaderCarrier()
  override def messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]

  val taxYear = 2019
  val fakeAuthenticatedRequest = PayeAuthenticatedRequest(testNino, FakeRequest("GET", "/annual-tax-summary/paye/treasury-spending"))

  class TestController extends PayeYourIncomeAndTaxesController {

    override val payeAuthAction: PayeAuthAction = FakePayeAuthAction
    override val payeYear = taxYear
    override val payeAtsService = mock[PayeAtsService]

    private def readJson(path: String) = {
      val resource = getClass.getResourceAsStream(path)
      Json.parse(Source.fromInputStream(resource).getLines().mkString)
    }

    val expectedSuccessResponse: JsValue = readJson("/paye_ats.json")
  }

  "Paye your income and taxes controller" should {

    "return OK response" in new TestController {

      when(payeAtsService.getPayeATSData(eqTo(testNino), eqTo(2019))(any[HeaderCarrier]))
        .thenReturn(Right(expectedSuccessResponse.as[PayeAtsData]))

      val result = show(fakeAuthenticatedRequest)

      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))

      document.title should include(Messages("paye.ats.summary.title")+ Messages("generic.to_from", (taxYear - 1).toString, taxYear.toString))
    }

    "throw internal server exception when total_income_before_tax is missing in PAYE ATS data" in new TestController {

      when(payeAtsService.getPayeATSData(eqTo(testNino), eqTo(2019))(any[HeaderCarrier]))
        .thenReturn(Right(PayeAtsTestData.malformedYourIncomeAndTaxesData))

      val result = show(fakeAuthenticatedRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR

      contentAsString(result) shouldBe "Missing total_income_before_tax in payload"
    }

    "throw internal server exception when summary data is missing" in new TestController {

      when(payeAtsService.getPayeATSData(eqTo(testNino), eqTo(2019))(any[HeaderCarrier]))
        .thenReturn(Right(PayeAtsTestData.missingYourIncomeAndTaxesData))

      val result = show(fakeAuthenticatedRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR

      contentAsString(result) shouldBe "Missing summary_data in payeAtsData"
    }

    "redirect user to noAts page when receiving NOT_FOUND from service" in new TestController {

      when(payeAtsService.getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier]))
        .thenReturn(Left(HttpResponse(responseStatus = NOT_FOUND, responseJson = Some(Json.toJson(NOT_FOUND)))))

      val result = show(fakeAuthenticatedRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe controllers.routes.ErrorController.authorisedNoAts().url
    }

    "return BAD_REQUEST response when receiving BAD_REQUEST from service" in new TestController {

      when(payeAtsService.getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier]))
        .thenReturn(Left(HttpResponse(responseStatus = BAD_REQUEST, responseJson = Some(Json.toJson(BAD_REQUEST)))))

      val result = show(fakeAuthenticatedRequest)

      status(result) shouldBe BAD_REQUEST
    }
  }
}
