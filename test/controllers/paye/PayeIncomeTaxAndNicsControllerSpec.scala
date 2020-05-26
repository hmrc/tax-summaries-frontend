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

import controllers.auth.FakePayeAuthAction
import models.PayeAtsData
import org.jsoup.Jsoup
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.http.Status._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import utils.TestConstants.testNino

class PayeIncomeTaxAndNicsControllerSpec extends PayeControllerSpecHelpers with GuiceOneAppPerTest with I18nSupport {

  override def messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]

  val fakeAuthenticatedRequest = buildPayeRequest("/annual-tax-summary/paye/total-income-tax")

  implicit lazy val formPartialRetriever = fakeApplication.injector.instanceOf[FormPartialRetriever]
  val sut = new PayeIncomeTaxAndNicsController(mockPayeAtsService, FakePayeAuthAction)

  "Paye your income tax and nics controller" should {

    "return OK response" in {

      when(mockPayeAtsService.getPayeATSData(eqTo(testNino), eqTo(2018))(any[HeaderCarrier]))
        .thenReturn(Right(expectedResponse.as[PayeAtsData]))

      val result = sut.show(fakeAuthenticatedRequest)

      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))

      document.title should include(Messages("paye.ats.total_income_tax.title") + Messages("generic.to_from", taxYear.toString, (taxYear + 1).toString))
    }

    "redirect user to noAts page when receiving NOT_FOUND from service" in{

      when(mockPayeAtsService.getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier]))
        .thenReturn(Left(HttpResponse(responseStatus = NOT_FOUND, responseJson = Some(Json.toJson(NOT_FOUND)))))

      val result = sut.show(fakeAuthenticatedRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.PayeErrorController.authorisedNoAts().url
    }

    "redirect user to generic error page when receiving INTERNAL_SERVER_ERROR from service" in {

      when(mockPayeAtsService.getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier]))
        .thenReturn(Left(HttpResponse(responseStatus = INTERNAL_SERVER_ERROR, responseJson = Some(Json.toJson(INTERNAL_SERVER_ERROR)))))

      val result = sut.show(fakeAuthenticatedRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.PayeErrorController.genericError(500).url
    }

    "redirect user to generic error page when receiving BAD_REQUEST from service" in {

      when(mockPayeAtsService.getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier]))
        .thenReturn(Left(HttpResponse(responseStatus = BAD_REQUEST, responseJson = Some(Json.toJson(BAD_REQUEST)))))

      val result = sut.show(fakeAuthenticatedRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.PayeErrorController.genericError(400).url
    }
  }
}
