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
import models.AtsData
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.MustMatchers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.FakeRequest
import services.atsData.AtsTestData
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import utils.GenericViewModel
import utils.TestConstants._
import view_models._

import scala.concurrent.Await
import scala.concurrent.duration._

class TotalIncomeTaxServiceSpec extends UnitSpec with GuiceOneAppPerSuite with ScalaFutures with MockitoSugar {

  val genericViewModel: GenericViewModel =  AtsList(
    utr = "3000024376",
    forename = "forename",
    surname = "surname",
    yearList = List(
      TaxYearEnd(Some("2015"))
    )
  )

  class TestService extends TotalIncomeTaxService with MockitoSugar {
    override lazy val atsService: AtsService = mock[AtsService]
    override lazy val atsYearListService: AtsYearListService = mock[AtsYearListService]
    implicit val hc = new HeaderCarrier
    val taxYear = 2015
  }

  "TotalIncomeTaxService getIncomeData" should {

    "return a GenericViewModel when TaxYearUtil.extractTaxYear returns a taxYear" in new TestService {
      when(atsService.createModel(Matchers.eq(taxYear), Matchers.any[Function1[AtsData, GenericViewModel]]())(Matchers.any(), Matchers.any())).thenReturn(genericViewModel)
      lazy val request = AuthenticatedRequest("userId", None, Some(SaUtr(testUtr)), None, None, None, None, FakeRequest("GET","?taxYear=2015"))
      val result = Await.result(getIncomeData(taxYear)(hc, request), 1500 millis)
      result mustEqual genericViewModel
    }
  }

    "TotalIncomeTaxService.totalIncomeConverter" should {


    "return complete TotalIncomeTax data when given complete AtsData" in new TestService {
      val incomeData: AtsData = AtsTestData.totalIncomeTaxData
      val result: TotalIncomeTax = totalIncomeConverter(incomeData)

      val scottishTax = ScottishTax(
        Amount.gbp(1800),
        Amount.gbp(1900),
        Amount.gbp(2000),
        Amount.gbp(2100),
        Amount.gbp(2200),
        Amount.gbp(2300),
        Amount.gbp(2400),
        Amount.gbp(2500),
        Amount.gbp(2600),
        Amount.gbp(2700),
        Amount.gbp(2800)
      )

      val savingsTax = SavingsTax(
        Amount.gbp(2900),
        Amount.gbp(3000),
        Amount.gbp(3100),
        Amount.gbp(3200),
        Amount.gbp(3300),
        Amount.gbp(3400)
      )

      val scottishRates = ScottishRates(
        Rate("80%"),
        Rate("90%"),
        Rate("100%"),
        Rate("110%"),
        Rate("120%")
      )

      val savingsRates = SavingsRates(
        Rate("130%"),
        Rate("140%"),
        Rate("150%")
      )
         println(result)
      result mustEqual TotalIncomeTax(
        2019,
        "1111111111",
        Amount(100, "GBP"),
        Amount(200, "GBP"),
        Amount(300, "GBP"),
        Amount(400, "GBP"),
        Amount(500, "GBP"),
        Amount(600, "GBP"),
        Amount(700, "GBP"),
        Amount(800, "GBP"),
        Amount(900, "GBP"),
        Amount(1000, "GBP"),
        Amount(1100, "GBP"),
        Amount(1200, "GBP"),
        Amount(1300, "GBP"),
        Amount(1400, "GBP"),
        Amount(1500, "GBP"),
        Amount(1600, "GBP"),
        Amount(1700, "GBP"),
        scottishTax,
        Amount.gbp(3500),
        Amount.gbp(3600),
        savingsTax,
        "0002",
        Rate("10%"),
        Rate("20%"),
        Rate("30%"),
        Rate("40%"),
        Rate("50%"),
        Rate("60%"),
        Rate("70%"),
        scottishRates,
        savingsRates,
        Amount(3700, "GBP"),
        Amount(3800, "GBP"),
        "Mr",
        "John",
        "Smith"
      )
    }

  }
}
