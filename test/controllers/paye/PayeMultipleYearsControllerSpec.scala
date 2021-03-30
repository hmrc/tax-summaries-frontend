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
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.JsonUtil
import utils.TestConstants.testNino
import view_models.AtsForms
import views.html.paye.{PayeMultipleYearsView, PayeTaxsMainView}

import scala.concurrent.Future

class PayeMultipleYearsControllerSpec extends PayeControllerSpecHelpers with JsonUtil {

  implicit val fakeAuthenticatedRequest = buildPayeRequest("/annual-tax-summary/paye/treasury-spending")

  val taxYearMinus1: Int = taxYear - 1
  val taxYearList: List[Int] = (taxYearMinus1 to taxYear).toList

  lazy val multipleYearsView = inject[PayeMultipleYearsView]
  lazy val mainView = inject[PayeTaxsMainView]

  def getSingleYearData: PayeAtsData =
    parseData[PayeAtsData](
      loadAndReplace("/paye_ats.json", Map("$nino" -> testNino.nino))
    )

  def getMultiYearData: List[PayeAtsData] =
    parseData[List[PayeAtsData]](
      loadAndReplace("/paye_ats_multiple_years.json", Map("$nino" -> testNino.nino))
    )

  private def parseData[A](str: String)(implicit reads: Reads[A]): A = Json.parse(str).as[A]

  def sut(multiYearEnabled: Boolean = true): PayeMultipleYearsController =
    new PayeMultipleYearsController(mockPayeAtsService, FakePayeAuthAction, mcc, multipleYearsView)(
      formPartialRetriever,
      templateRenderer,
      appConfig,
      ec
    )

  "AtsMain controller" when {

    "return OK and show multiple years page" when {

      "multiple years of data are returned" in {

        when(
          mockPayeAtsService
            .getPayeATSMultipleYearData(eqTo(testNino), eqTo(taxYearMinus1), eqTo(taxYear))(
              any[HeaderCarrier],
              any[PayeAuthenticatedRequest[_]]))
          .thenReturn(Future.successful(Right(getMultiYearData)))

        val result = sut().onPageLoad(fakeAuthenticatedRequest)

        status(result) shouldBe OK
        contentAsString(result) shouldBe multipleYearsView(taxYearList.reverse, AtsForms.atsYearFormMapping).toString
      }
    }

    "redirect to ATS main page" when {

      "a single year of data is returned" in {

        when(
          mockPayeAtsService
            .getPayeATSMultipleYearData(eqTo(testNino), eqTo(taxYearMinus1), eqTo(taxYear))(any[HeaderCarrier], any()))
          .thenReturn(Future.successful(Right(List(getSingleYearData))))

        val result = sut().onPageLoad(fakeAuthenticatedRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.PayeAtsMainController.show(taxYear).url)
      }

      "the form for multiple years is successfully submitted" in {

        val request =
          PayeAuthenticatedRequest(
            testNino,
            false,
            fakeCredentials,
            FakeRequest("POST", "/annual-tax-summary/paye/treasury-spending")
              .withSession("taxYearFrom" -> taxYearMinus1.toString, "taxYearTo" -> taxYear.toString)
              .withFormUrlEncodedBody("year" -> taxYear.toString)
          )
        val result = sut().onSubmit(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.PayeAtsMainController.show(taxYear).url)
      }
    }

    "redirect user to noAts page" when {

      "the data returned is empty" in {

        when(
          mockPayeAtsService
            .getPayeATSMultipleYearData(eqTo(testNino), eqTo(taxYearMinus1), eqTo(taxYear))(
              any[HeaderCarrier],
              any[PayeAuthenticatedRequest[_]]))
          .thenReturn(Right(List[PayeAtsData]()))

        val result = sut().onPageLoad(fakeAuthenticatedRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.PayeErrorController.authorisedNoAts().url)
      }

      "receiving NOT_FOUND from service" in {

        when(
          mockPayeAtsService
            .getPayeATSMultipleYearData(eqTo(testNino), eqTo(taxYearMinus1), eqTo(taxYear))(
              any[HeaderCarrier],
              any[PayeAuthenticatedRequest[_]]))
          .thenReturn(Left(HttpResponse(NOT_FOUND, "Not found")))

        val result = sut().onPageLoad(fakeAuthenticatedRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.PayeErrorController.authorisedNoAts().url)
      }
    }

    "return BAD_REQUEST" when {

      "the form is submitted with an error" in {

        val request =
          PayeAuthenticatedRequest(
            testNino,
            false,
            fakeCredentials,
            FakeRequest("POST", "/")
              .withSession("taxYearFrom" -> taxYearMinus1.toString, "taxYearTo" -> taxYear.toString)
              .withFormUrlEncodedBody("year" -> "")
          )
        val result = sut().onSubmit(request)

        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include(testMessages("ats.select_tax_year.required.summary"))
      }
    }

    "show Generic Error page and return INTERNAL_SERVER_ERROR" when {
      "error received from NPS service" in {

        when(
          mockPayeAtsService
            .getPayeATSMultipleYearData(eqTo(testNino), eqTo(taxYearMinus1), eqTo(taxYear))(
              any[HeaderCarrier],
              any[PayeAuthenticatedRequest[_]]))
          .thenReturn(Left(HttpResponse(INTERNAL_SERVER_ERROR, "Error occurred")))

        val result = sut().onPageLoad(fakeAuthenticatedRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.PayeErrorController.genericError(INTERNAL_SERVER_ERROR).url)
      }
    }
  }
}
