/*
 * Copyright 2022 HM Revenue & Customs
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
import models.{AtsErrorResponse, AtsNotFoundResponse, PayeAtsData}
import org.mockito.ArgumentMatchers.any
import play.api.http.Status._
import play.api.libs.json.{Json, Reads}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import utils.JsonUtil
import utils.TestConstants.testNino
import view_models.paye.PayeAtsMain
import views.html.errors.PayeGenericErrorView
import views.html.paye.PayeTaxsMainView

import scala.concurrent.Future

class PayeAtsMainControllerSpec extends PayeControllerSpecHelpers with JsonUtil {

  implicit val fakeAuthenticatedRequest = buildPayeRequest("/annual-tax-summary/paye/treasury-spending")

  lazy val mainView             = inject[PayeTaxsMainView]
  lazy val payeGenericErrorView = inject[PayeGenericErrorView]

  def getSingleYearData: PayeAtsData =
    parseData[PayeAtsData](
      loadAndReplace("/paye_ats_2020.json", Map("$nino" -> testNino.nino))
    )

  private def parseData[A](str: String)(implicit reads: Reads[A]): A = Json.parse(str).as[A]

  def sut: PayeAtsMainController =
    new PayeAtsMainController(mockPayeAtsService, FakePayeAuthAction, mcc, mainView, payeGenericErrorView)

  "AtsMain controller" when {

    "return OK response" in {

      when(
        mockPayeAtsService
          .getPayeATSData(any(), any())(any())
      )
        .thenReturn(Future(Right(mock[PayeAtsData])))

      val result = sut.show(taxYear)(fakeAuthenticatedRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe mainView(PayeAtsMain(taxYear)).toString
    }

    "redirect user to noAts page when receiving NOT_FOUND from service" in {

      when(
        mockPayeAtsService
          .getPayeATSData(any(), any())(any())
      )
        .thenReturn(Future(Left(AtsNotFoundResponse("Not found"))))

      val result = sut.show(taxYear)(fakeAuthenticatedRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.ErrorController.authorisedNoAts(appConfig.taxYear).url)
    }

    "show Generic Error page and return INTERNAL_SERVER_ERROR if error received from NPS service" in {
      when(
        mockPayeAtsService
          .getPayeATSData(any(), any())(any())
      )
        .thenReturn(Future(Left(AtsErrorResponse("Error occurred"))))

      val result = sut.show(taxYear)(fakeAuthenticatedRequest)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) mustBe payeGenericErrorView().toString()
    }
  }
}
