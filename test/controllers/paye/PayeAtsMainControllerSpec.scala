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

class PayeAtsMainControllerSpec extends UnitSpec with MockitoSugar with GuiceOneAppPerTest with I18nSupport {

  implicit val hc = HeaderCarrier()
  override def messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]

  val taxYear = 2018
  val fakeAuthenticatedRequest = PayeAuthenticatedRequest(testNino, FakeRequest("GET", "/annual-tax-summary/paye/treasury-spending"))

  class TestController extends PayeAtsMainController {

    override val payeAuthAction: PayeAuthAction = FakePayeAuthAction
    override val payeYear = taxYear
    override val payeAtsService = mock[PayeAtsService]
  }

  "AtsMain controller" should {

    "return OK response" in new TestController {


      when(payeAtsService.getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier]))
        .thenReturn(Right(mock[PayeAtsData]))

      val result = show(fakeAuthenticatedRequest)

      status(result) shouldBe 200

      val document = Jsoup.parse(contentAsString(result))

      document.title should include(Messages("paye.ats.index.html.title") + Messages("generic.to_from", taxYear.toString, (taxYear + 1).toString))

      document.getElementById("index-page-description").text() shouldBe(Messages("paye.ats.index.html.lede"))

      document.getElementById("tax-services-link").text shouldBe(Messages("paye.ats.index.html.tax_spend_link"))

      document.getElementsByTag("p").get(2).text shouldBe(Messages("paye.ats.index.html.tax_calc_description"))
    }

    "redirect user to noAts page when receiving NOT_FOUND from service" in new TestController {

      when(payeAtsService.getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier]))
        .thenReturn(Left(HttpResponse(responseStatus = NOT_FOUND)))

      val result = show(fakeAuthenticatedRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.paye.routes.PayeErrorController.authorisedNoAts().url)
    }

    "show Generic Error page and return INTERNAL_SERVER_ERROR if error received from NPS service" in new TestController {

      when(payeAtsService.getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier]))
        .thenReturn(Left(HttpResponse(responseStatus = INTERNAL_SERVER_ERROR)))

      val result = show(fakeAuthenticatedRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe controllers.paye.routes.PayeErrorController.genericError(INTERNAL_SERVER_ERROR).url
    }
  }
}
