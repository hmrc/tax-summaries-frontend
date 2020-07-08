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

import controllers.ControllerBaseSpec
import controllers.auth.{FakePayeAuthAction, PayeAuthenticatedRequest}
import models.PayeAtsData
import org.jsoup.Jsoup
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import play.api.http.Status._
import play.api.i18n.Messages
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.TestConstants.testNino

class PayeAtsMainControllerSpec extends PayeControllerSpecHelpers with ControllerBaseSpec {

  val fakeAuthenticatedRequest = buildPayeRequest("/annual-tax-summary/paye/treasury-spending")

  def sut = new PayeAtsMainController(mockPayeAtsService, FakePayeAuthAction, mcc)

  "AtsMain controller" should {

    "return OK response" in {

      when(mockPayeAtsService.getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier], any[PayeAuthenticatedRequest[_]]))
        .thenReturn(Right(mock[PayeAtsData]))

      val result = sut.show(fakeAuthenticatedRequest)

      status(result) shouldBe 200

      val document = Jsoup.parse(contentAsString(result))

      document.title should include(Messages("paye.ats.index.html.title") + Messages("generic.to_from", taxYear.toString, (taxYear + 1).toString))

      document.getElementById("index-page-description").text() shouldBe(Messages("paye.ats.index.html.lede"))

      document.getElementById("tax-services-link").text shouldBe(Messages("paye.ats.index.html.tax_spend_link"))

      document.getElementsByTag("p").get(2).text shouldBe(Messages("paye.ats.index.html.tax_calc_description"))
    }

    "redirect user to noAts page when receiving NOT_FOUND from service" in  {

      when(mockPayeAtsService.getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier], any[PayeAuthenticatedRequest[_]]))
        .thenReturn(Left(HttpResponse(responseStatus = NOT_FOUND)))

      val result = sut.show(fakeAuthenticatedRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.PayeErrorController.authorisedNoAts().url)
    }

    "show Generic Error page and return INTERNAL_SERVER_ERROR if error received from NPS service" in {

      when(mockPayeAtsService.getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier], any[PayeAuthenticatedRequest[_]]))
        .thenReturn(Left(HttpResponse(responseStatus = INTERNAL_SERVER_ERROR)))

      val result = sut.show(fakeAuthenticatedRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.PayeErrorController.genericError(INTERNAL_SERVER_ERROR).url)
    }
  }
}
