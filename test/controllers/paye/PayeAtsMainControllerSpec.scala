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

import controllers.auth.{FakePayeAuthAction, PayeAuthenticatedRequest}
import models.PayeAtsData
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import play.api.http.Status._
import play.api.libs.json.{Json, Reads}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.JsonUtil
import utils.TestConstants.testNino
import view_models.paye.PayeAtsMain
import views.html.paye.PayeTaxsMainView

class PayeAtsMainControllerSpec extends PayeControllerSpecHelpers with JsonUtil {

  implicit val fakeAuthenticatedRequest = buildPayeRequest("/annual-tax-summary/paye/treasury-spending")

  lazy val mainView = inject[PayeTaxsMainView]

  def getSingleYearData: PayeAtsData =
    parseData[PayeAtsData](
      loadAndReplace("/paye_ats.json", Map("$nino" -> testNino.nino))
    )

  private def parseData[A](str: String)(implicit reads: Reads[A]): A = Json.parse(str).as[A]

  def sut: PayeAtsMainController =
    new PayeAtsMainController(mockPayeAtsService, FakePayeAuthAction, mcc, mainView)

  "AtsMain controller" when {

    "return OK response" in {

      when(
        mockPayeAtsService
          .getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier], any[PayeAuthenticatedRequest[_]]))
        .thenReturn(Right(mock[PayeAtsData]))

      val result = sut.show(taxYear)(fakeAuthenticatedRequest)

      status(result) shouldBe OK
      contentAsString(result) shouldBe mainView(PayeAtsMain(taxYear)).toString
    }

    "redirect user to noAts page when receiving NOT_FOUND from service" in {

      when(
        mockPayeAtsService
          .getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier], any[PayeAuthenticatedRequest[_]]))
        .thenReturn(Left(HttpResponse(NOT_FOUND, "Not found")))

      val result = sut.show(taxYear)(fakeAuthenticatedRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.routes.ErrorController.authorisedNoAts(appConfig.taxYear).url)
    }

    "show Generic Error page and return INTERNAL_SERVER_ERROR if error received from NPS service" in {

      when(
        mockPayeAtsService
          .getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier], any[PayeAuthenticatedRequest[_]]))
        .thenReturn(Left(HttpResponse(INTERNAL_SERVER_ERROR, "Error occurred")))

      val result = sut.show(taxYear)(fakeAuthenticatedRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.PayeErrorController.genericError(INTERNAL_SERVER_ERROR).url)
    }
  }
}
