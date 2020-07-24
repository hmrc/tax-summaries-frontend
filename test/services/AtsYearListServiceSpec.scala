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

package services

import controllers.auth.AuthenticatedRequest
import models.{AgentToken, AtsListData}
import org.mockito.Matchers
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.MustMatchers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.test.FakeRequest
import services.atsData.AtsTestData
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import utils.GenericViewModel
import utils.TestConstants._
import view_models.{AtsList, TaxYearEnd}

import scala.concurrent.Future
import scala.io.Source

class AtsYearListServiceSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar with ScalaFutures {

  val data = {
    val source = Source.fromURL(getClass.getResource("/test_list_utr.json")).mkString
    val json = Json.parse(source)
    Json.fromJson[AtsListData](json).get
  }
  val mockAtsListService=mock[AtsListService]
  class TestService extends AtsYearListService(mockAtsListService) {

    implicit val request = AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest())
    implicit val hc = new HeaderCarrier

    val agentToken = AgentToken(
      agentUar = testUar,
      clientUtr = testUtr,
      timestamp = 0
    )
    val atsService = mock[AtsService]
  }

  "storeSelectedAtsTaxYear" should {

    "Return a successful future upon success" in new TestService {

      when(mockAtsListService.storeSelectedTaxYear(eqTo(2014))(any[HeaderCarrier])).thenReturn(Future.successful(2014))

      val result = storeSelectedAtsTaxYear(2014)

      whenReady(result) { result =>
        result shouldBe 2014
      }
    }

    "Return a failed future when None is returned from the dataCache" in new TestService {

      when(mockAtsListService.storeSelectedTaxYear(eqTo(2014))(any[HeaderCarrier])).thenReturn(Future.failed(new Exception("failed")))

      val result = storeSelectedAtsTaxYear(2014)

      whenReady(result.failed) { exception =>
        exception shouldBe a [Exception]
      }
    }

  }


  "getAtsListData" should {

    "Return a successful future upon success" in new TestService {

      val atsListModel: AtsList = AtsList(
        utr = testUtr,
        forename = "forename",
        surname = "surname",
        yearList = List(
          TaxYearEnd(Some("2014")),
          TaxYearEnd(Some("2015"))
        )
      )

      override def getAtsListData(implicit hc: HeaderCarrier, request: AuthenticatedRequest[_]): Future[GenericViewModel] = {
        mockAtsListService.createModel(atsList)
      }

      def atsList: AtsListData => GenericViewModel =
        (output: AtsListData) => {
          new AtsList(output.utr,
            output.taxPayer.get.taxpayer_name.get("forename"),
            output.taxPayer.get.taxpayer_name.get("surname"),
            output.atsYearList.get.map(year => TaxYearEnd(Some(year.toString)))
          )
        }

      val model: GenericViewModel = atsListModel

      when(mockAtsListService.createModel(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(model)

      val result = getAtsListData(hc, request)

      result.toString().trim mustEqual Future.successful(model).toString().trim()


    }

  }

  "AtsYearListService.atsListDataConverter" should {
    "return an AtsList when given complete AtsListData" in new TestService {
      val atsListData = AtsTestData.atsListData
      val result = atsListDataConverter(atsListData)

      result shouldBe AtsList(
        "1111111111",
        "John",
        "Smith",
        List(
          TaxYearEnd(
            Some("2018")
          )
        )
      )
    }
  }

}
