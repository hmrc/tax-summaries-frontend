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

import config.ApplicationConfig
import controllers.auth.{FakePayeAuthAction, PayeAuthenticatedRequest}
import models.PayeAtsData
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK, SEE_OTHER}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import utils.TestConstants.testNino

class PayeYourTaxableIncomeControllerSpec extends PayeControllerSpecHelpers with GuiceOneAppPerSuite with I18nSupport {

  override def messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]

  val fakeAuthenticatedRequest = PayeAuthenticatedRequest(testNino, FakeRequest("GET", "/annual-tax-summary/paye/treasury-spending"))

  implicit lazy val formPartialRetriever = fakeApplication.injector.instanceOf[FormPartialRetriever]
  implicit lazy val appConfig = fakeApplication.injector.instanceOf[ApplicationConfig]

  val sut = new PayeYourTaxableIncomeController(mockPayeAtsService, FakePayeAuthAction)

  "Government spend controller" should {

    "return OK response" in {

      when(mockPayeAtsService.getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier],any[PayeAuthenticatedRequest[_]]))
        .thenReturn(Right(expectedResponse.as[PayeAtsData]))

      val result = sut.show(fakeAuthenticatedRequest)

      status(result) shouldBe OK

      contentAsString(result) should include(
        Messages("paye.ats.income_before_tax.title") + Messages("generic.to_from", taxYear.toString, (taxYear+1).toString))
    }

    "redirect user to noAts page when receiving NOT_FOUND from service" in {

      when(mockPayeAtsService.getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier],any[PayeAuthenticatedRequest[_]]))
        .thenReturn(Left(HttpResponse(responseStatus = NOT_FOUND, responseJson = Some(Json.toJson(NOT_FOUND)))))

      val result = sut.show(fakeAuthenticatedRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.PayeErrorController.authorisedNoAts().url
    }

    "show Generic Error page and return INTERNAL_SERVER_ERROR if error received from NPS service" in {

      when(mockPayeAtsService.getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier],any[PayeAuthenticatedRequest[_]]))
        .thenReturn(Left(HttpResponse(responseStatus = INTERNAL_SERVER_ERROR)))

      val result = sut.show(fakeAuthenticatedRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.PayeErrorController.genericError(INTERNAL_SERVER_ERROR).url
    }
  }

}
