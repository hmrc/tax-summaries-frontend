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

package services.paye

import models.AtsData
import org.scalatest.MustMatchers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import services.atsData.AtsTestData
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import view_models.paye._
import view_models._

class PayeTotalIncomeTaxServiceSpec extends UnitSpec with GuiceOneAppPerSuite with ScalaFutures with MockitoSugar {

  class TestService extends PayeTotalIncomeTaxService with MockitoSugar {
    override lazy val atsService: PayeAtsService = mock[PayeAtsService]
    implicit val hc = new HeaderCarrier
    val taxYear = 2015
  }


  "PayeTotalIncomeTaxService.totalIncomeConverter" should {
    "return complete TotalIncomeTax data when given complete AtsData" in new TestService {
      val incomeData: AtsData = AtsTestData.totalIncomeTaxData
      val result: PayeTotalIncomeTax = totalIncomeConverter(incomeData)

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
      result mustEqual PayeTotalIncomeTax(
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
