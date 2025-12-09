/*
 * Copyright 2023 HM Revenue & Customs
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

package common.view_models.paye

import common.models.{DataHolder, PayeAtsData}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import common.utils.{JsonUtil, TaxYearForTesting}
import common.view_models.Amount
import paye.view_models.{AmountRow, PayeTaxFreeAmount}

class PayeTaxFreeAmountSpec
    extends AnyWordSpec
    with Matchers
    with JsonUtil
    with GuiceOneAppPerSuite
    with ScalaFutures
    with IntegrationPatience
    with TaxYearForTesting {

  def payeAtsData(allowance_data: Map[String, Amount], summary_data: Map[String, Amount]): PayeAtsData =
    PayeAtsData(
      taxYear = currentTaxYearSA,
      income_tax = None,
      summary_data = Some(DataHolder(Some(summary_data), None, None)),
      income_data = None,
      allowance_data = Some(DataHolder(Some(allowance_data), None, None)),
      gov_spending = None,
      includeBRDMessage = false
    )

  "PayeTaxFreeAmount" must {

    "use total_tax_free_amount as tax_free_amount if it is non-zero " in {

      val data = payeAtsData(
        summary_data = Map(
          "personal_tax_free_amount" -> Amount(9440, "GBP"),
          "total_tax_free_amount"    -> Amount(9740, "GBP"),
          "total_income_before_tax"  -> Amount(500, "GBP"),
          "liable_tax_amount"        -> Amount(1200, "GBP")
        ),
        allowance_data = Map(
          "personal_tax_free_amount" -> Amount(9440, "GBP"),
          "other_allowances_amount"  -> Amount(300, "GBP")
        )
      )

      val expectedViewModel = PayeTaxFreeAmount(
        currentTaxYearSA,
        List(
          AmountRow("personal_tax_free_amount", Amount.gbp(9440)),
          AmountRow("other_allowances_amount", Amount.gbp(300))
        ),
        Amount.gbp(9740),
        List(
          AmountRow("income_before_tax", Amount.gbp(500)),
          AmountRow("tax_free_amount", Amount.gbp(9740))
        ),
        Amount.gbp(1200)
      )

      val result = PayeTaxFreeAmount(data)

      result mustBe expectedViewModel
    }

    "use personal_tax_free_amount as tax_free_amount if total_tax_free_amount is zero" in {

      val data = payeAtsData(
        summary_data = Map(
          "personal_tax_free_amount" -> Amount(9440, "GBP"),
          "total_tax_free_amount"    -> Amount(0, "GBP"),
          "total_income_before_tax"  -> Amount(500, "GBP"),
          "liable_tax_amount"        -> Amount(1200, "GBP")
        ),
        allowance_data = Map(
          "personal_tax_free_amount" -> Amount(9440, "GBP"),
          "other_allowances_amount"  -> Amount(300, "GBP")
        )
      )

      val expectedViewModel = PayeTaxFreeAmount(
        currentTaxYearSA,
        List(
          AmountRow("personal_tax_free_amount", Amount.gbp(9440)),
          AmountRow("other_allowances_amount", Amount.gbp(300))
        ),
        Amount.gbp(0),
        List(
          AmountRow("income_before_tax", Amount.gbp(500)),
          AmountRow("tax_free_amount", Amount.gbp(9440))
        ),
        Amount.gbp(1200)
      )

      val result = PayeTaxFreeAmount(data)

      result mustBe expectedViewModel
    }

    "use personal_tax_free_amount as tax_free_amount if total_tax_free_amount is missing" in {

      val data = payeAtsData(
        summary_data = Map(
          "personal_tax_free_amount" -> Amount(9440, "GBP"),
          "total_income_before_tax"  -> Amount(500, "GBP"),
          "liable_tax_amount"        -> Amount(1200, "GBP")
        ),
        allowance_data = Map(
          "personal_tax_free_amount" -> Amount(9440, "GBP"),
          "other_allowances_amount"  -> Amount(300, "GBP")
        )
      )

      val expectedViewModel = PayeTaxFreeAmount(
        currentTaxYearSA,
        List(
          AmountRow("personal_tax_free_amount", Amount.gbp(9440)),
          AmountRow("other_allowances_amount", Amount.gbp(300))
        ),
        Amount.gbp(0),
        List(
          AmountRow("income_before_tax", Amount.gbp(500)),
          AmountRow("tax_free_amount", Amount.gbp(9440))
        ),
        Amount.gbp(1200)
      )

      val result = PayeTaxFreeAmount(data)

      result mustBe expectedViewModel
    }

    "include populated rows for personal tax free amount, marriage allowance and other allowances when present" in {

      val data = payeAtsData(
        summary_data = Map(
          "personal_tax_free_amount" -> Amount(9440, "GBP"),
          "total_tax_free_amount"    -> Amount(9740, "GBP"),
          "total_income_before_tax"  -> Amount(500, "GBP"),
          "liable_tax_amount"        -> Amount(1200, "GBP")
        ),
        allowance_data = Map(
          "personal_tax_free_amount"              -> Amount(9440, "GBP"),
          "marriage_allowance_transferred_amount" -> Amount(200, "GBP"),
          "other_allowances_amount"               -> Amount(300, "GBP")
        )
      )

      val expectedViewModel = PayeTaxFreeAmount(
        currentTaxYearSA,
        List(
          AmountRow("personal_tax_free_amount", Amount.gbp(9440)),
          AmountRow("marriage_allowance_transferred_amount", Amount.gbp(200)),
          AmountRow("other_allowances_amount", Amount.gbp(300))
        ),
        Amount.gbp(9740),
        List(
          AmountRow("income_before_tax", Amount.gbp(500)),
          AmountRow("tax_free_amount", Amount.gbp(9740))
        ),
        Amount.gbp(1200)
      )

      val result = PayeTaxFreeAmount(data)

      result mustBe expectedViewModel
    }

    "have no rows for personal tax free amount, marriage allowance and other allowances when zero" in {

      val data = payeAtsData(
        summary_data = Map(
          "personal_tax_free_amount" -> Amount(9440, "GBP"),
          "total_tax_free_amount"    -> Amount(9740, "GBP"),
          "total_income_before_tax"  -> Amount(500, "GBP"),
          "liable_tax_amount"        -> Amount(1200, "GBP")
        ),
        allowance_data = Map(
          "personal_tax_free_amount"              -> Amount(0, "GBP"),
          "marriage_allowance_transferred_amount" -> Amount(0, "GBP"),
          "other_allowances_amount"               -> Amount(0, "GBP")
        )
      )

      val expectedViewModel = PayeTaxFreeAmount(
        currentTaxYearSA,
        List(
        ),
        Amount.gbp(9740),
        List(
          AmountRow("income_before_tax", Amount.gbp(500)),
          AmountRow("tax_free_amount", Amount.gbp(9740))
        ),
        Amount.gbp(1200)
      )

      val result = PayeTaxFreeAmount(data)

      result mustBe expectedViewModel
    }

    "have no rows for personal tax free amount, marriage allowance and other allowances when missing" in {

      val data = payeAtsData(
        summary_data = Map(
          "personal_tax_free_amount" -> Amount(9440, "GBP"),
          "total_tax_free_amount"    -> Amount(9740, "GBP"),
          "total_income_before_tax"  -> Amount(500, "GBP"),
          "liable_tax_amount"        -> Amount(1200, "GBP")
        ),
        allowance_data = Map(
        )
      )

      val expectedViewModel = PayeTaxFreeAmount(
        currentTaxYearSA,
        List(
        ),
        Amount.gbp(9740),
        List(
          AmountRow("income_before_tax", Amount.gbp(500)),
          AmountRow("tax_free_amount", Amount.gbp(9740))
        ),
        Amount.gbp(1200)
      )

      val result = PayeTaxFreeAmount(data)

      result mustBe expectedViewModel
    }

    "return zeroed summary rows if values are zero" in {

      val data = payeAtsData(
        summary_data = Map(
          "personal_tax_free_amount" -> Amount(0, "GBP"),
          "total_tax_free_amount"    -> Amount(0, "GBP"),
          "total_income_before_tax"  -> Amount(0, "GBP"),
          "liable_tax_amount"        -> Amount(0, "GBP")
        ),
        allowance_data = Map(
          "personal_tax_free_amount" -> Amount(0, "GBP"),
          "other_allowances_amount"  -> Amount(0, "GBP")
        )
      )

      val expectedViewModel = PayeTaxFreeAmount(
        currentTaxYearSA,
        List(
        ),
        Amount.gbp(0),
        List(
          AmountRow("income_before_tax", Amount.gbp(0)),
          AmountRow("tax_free_amount", Amount.gbp(0))
        ),
        Amount.gbp(0)
      )

      val result = PayeTaxFreeAmount(data)

      result mustBe expectedViewModel
    }

    "return zeroed summary rows if values are missing" in {

      val data = payeAtsData(
        summary_data = Map(
        ),
        allowance_data = Map(
        )
      )

      val expectedViewModel = PayeTaxFreeAmount(
        currentTaxYearSA,
        List(
        ),
        Amount.gbp(0),
        List(
          AmountRow("income_before_tax", Amount.gbp(0)),
          AmountRow("tax_free_amount", Amount.gbp(0))
        ),
        Amount.gbp(0)
      )

      val result = PayeTaxFreeAmount(data)

      result mustBe expectedViewModel
    }
  }
}
