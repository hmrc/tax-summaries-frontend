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
import play.api.libs.json.{Json, Reads}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.JsonUtil
import utils.TestConstants.testNino
import view_models.AtsForms
import view_models.paye.PayeAtsMain
import views.html.paye.{PayeMultipleYearsView, PayeTaxsMainView}

import scala.concurrent.Future

class PayeAtsMainControllerSpec extends PayeControllerSpecHelpers with ControllerBaseSpec with JsonUtil {

  implicit val fakeAuthenticatedRequest = buildPayeRequest("/annual-tax-summary/paye/treasury-spending")

  val taxYearPlus1: Int = taxYear + 1
  val taxYearList = (taxYear to taxYearPlus1).toList

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

  def sut(multipleYearsEnabled: Boolean): PayeAtsMainController =
    new PayeAtsMainController(mockPayeAtsService, FakePayeAuthAction, mcc, mainView, multipleYearsView) {
      override val isMultipleYearsEnabled: Boolean = multipleYearsEnabled
    }

  "AtsMain controller" when {

    "multiple years is enabled" should {

      "return OK and show multiple years page" when {

        "multiple years of data are returned" in {

          when(
            mockPayeAtsService
              .getPayeATSMultipleYearData(eqTo(testNino), eqTo(taxYear), eqTo(taxYearPlus1))(
                any[HeaderCarrier],
                any[PayeAuthenticatedRequest[_]]))
            .thenReturn(Future.successful(Right(getMultiYearData)))

          val result = sut(true).show(fakeAuthenticatedRequest)

          status(result) shouldBe OK
          contentAsString(result) shouldBe multipleYearsView(taxYearList, AtsForms.atsYearFormMapping).toString
        }
      }

      "return OK and ATS main page" when {

        "a single year of data is returned" in {

          when(
            mockPayeAtsService
              .getPayeATSMultipleYearData(eqTo(testNino), eqTo(taxYear), eqTo(taxYearPlus1))(
                any[HeaderCarrier],
                any[PayeAuthenticatedRequest[_]]))
            .thenReturn(Future.successful(Right(List(getSingleYearData))))

          val result = sut(true).show(fakeAuthenticatedRequest)

          status(result) shouldBe OK
          contentAsString(result) shouldBe mainView(PayeAtsMain(taxYear), needsBackButton = false).toString
        }

        "the form for multiple years is successfully submitted" in {

          val request =
            PayeAuthenticatedRequest(
              testNino,
              FakeRequest("POST", "/annual-tax-summary/paye/treasury-spending")
                .withSession("taxYearFrom" -> taxYear.toString, "taxYearTo" -> taxYearPlus1.toString)
                .withFormUrlEncodedBody("year" -> taxYearPlus1.toString)
            )
          val result = sut(true).onSubmit(request)

          status(result) shouldBe OK
          contentAsString(result) shouldBe mainView(PayeAtsMain(taxYearPlus1)).toString
        }
      }

      "redirect user to noAts page" when {

        "the data returned is empty" in {

          when(
            mockPayeAtsService
              .getPayeATSMultipleYearData(eqTo(testNino), eqTo(taxYear), eqTo(taxYearPlus1))(
                any[HeaderCarrier],
                any[PayeAuthenticatedRequest[_]]))
            .thenReturn(Right(List[PayeAtsData]()))

          val result = sut(true).show(fakeAuthenticatedRequest)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.PayeErrorController.authorisedNoAts().url)
        }

        "receiving NOT_FOUND from service" in {

          when(
            mockPayeAtsService
              .getPayeATSMultipleYearData(eqTo(testNino), eqTo(taxYear), eqTo(taxYearPlus1))(
                any[HeaderCarrier],
                any[PayeAuthenticatedRequest[_]]))
            .thenReturn(Left(HttpResponse(responseStatus = NOT_FOUND)))

          val result = sut(true).show(fakeAuthenticatedRequest)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.PayeErrorController.authorisedNoAts().url)
        }
      }

      "return BAD_REQUEST" when {

        "the form on the multiple years page is submitted with an error" in {
          val request =
            PayeAuthenticatedRequest(
              testNino,
              FakeRequest("POST", "/")
                .withSession("taxYearFrom" -> taxYear.toString, "taxYearTo" -> taxYearPlus1.toString)
                .withFormUrlEncodedBody("year" -> ""))
          val result = sut(true).onSubmit(request)

          status(result) shouldBe BAD_REQUEST
          contentAsString(result) should include(testMessages("ats.select_tax_year.required.summary"))
        }
      }

      "show Generic Error page and return INTERNAL_SERVER_ERROR if error received from NPS service" in {

        when(
          mockPayeAtsService
            .getPayeATSMultipleYearData(eqTo(testNino), eqTo(taxYear), eqTo(taxYearPlus1))(
              any[HeaderCarrier],
              any[PayeAuthenticatedRequest[_]]))
          .thenReturn(Left(HttpResponse(responseStatus = INTERNAL_SERVER_ERROR)))

        val result = sut(true).show(fakeAuthenticatedRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.PayeErrorController.genericError(INTERNAL_SERVER_ERROR).url)
      }
    }

    "multiple years is disabled" should {

      "return OK response" in {

        when(
          mockPayeAtsService
            .getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier], any[PayeAuthenticatedRequest[_]]))
          .thenReturn(Right(mock[PayeAtsData]))

        val result = sut(false).show(fakeAuthenticatedRequest)

        status(result) shouldBe OK

        val document = Jsoup.parse(contentAsString(result))

        document.title should include(
          Messages("paye.ats.index.html.title") + Messages("generic.to_from", taxYear.toString, taxYearPlus1.toString))

        document.getElementById("index-page-description").text() shouldBe (Messages("paye.ats.index.html.lede"))

        document.getElementById("tax-services-link").text shouldBe (Messages("paye.ats.index.html.tax_spend_link"))

        document.getElementsByTag("p").get(1).text shouldBe (Messages("English | Cymraeg"))
        document.getElementsByTag("p").get(2).text shouldBe (Messages("paye.ats.index.html.lede"))
        document.getElementsByTag("p").get(3).text shouldBe (Messages("paye.ats.index.html.tax_calc_description"))
      }

      "redirect user to noAts page when receiving NOT_FOUND from service" in {

        when(
          mockPayeAtsService
            .getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier], any[PayeAuthenticatedRequest[_]]))
          .thenReturn(Left(HttpResponse(responseStatus = NOT_FOUND)))

        val result = sut(false).show(fakeAuthenticatedRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.PayeErrorController.authorisedNoAts().url)
      }

      "show Generic Error page and return INTERNAL_SERVER_ERROR if error received from NPS service" in {

        when(
          mockPayeAtsService
            .getPayeATSData(eqTo(testNino), eqTo(taxYear))(any[HeaderCarrier], any[PayeAuthenticatedRequest[_]]))
          .thenReturn(Left(HttpResponse(responseStatus = INTERNAL_SERVER_ERROR)))

        val result = sut(false).show(fakeAuthenticatedRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.PayeErrorController.genericError(INTERNAL_SERVER_ERROR).url)
      }
    }
  }
}
