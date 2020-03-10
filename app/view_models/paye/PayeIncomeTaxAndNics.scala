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

package view_models.paye

import models.{PayeAtsData, TaxBand}
import view_models.{Amount, Rate}

case class PayeIncomeTaxAndNics(taxYear: Int,
                                scottishTaxBands: List[TaxBand],
                                totalScottishIncomeTax: Amount) extends TaxYearFormatting

object PayeIncomeTaxAndNics {

  private val scottishRates = List(
    "scottish_starter_rate",
    "scottish_basic_rate",
    "scottish_intermediate_rate",
    "scottish_higher_rate"
  )

  def apply(payeAtsData: PayeAtsData): PayeIncomeTaxAndNics = {

    val scottishTaxBands = (for {
      incomeTax <- payeAtsData.income_tax
      rates <- incomeTax.rates
      payload <- incomeTax.payload
    } yield {
      scottishRates.flatMap { name =>
        for {
          rate <- rates.get("paye_" + name)
          incomeInTaxBand <- payload.get(name + "_income_tax")
          taxPaidInBand <- payload.get(name + "_income_tax_amount")
        } yield {
          TaxBand(name, incomeInTaxBand, taxPaidInBand, rate)
        }
      }.filter(_.bandRate != Rate.empty)
    }).getOrElse(List.empty)

    val totalScottishIncomeTax = payeAtsData.income_tax.flatMap(
      incomeTaxData => incomeTaxData.payload.flatMap(
        _.get("scottish_total_tax"))).getOrElse(Amount.empty)

    PayeIncomeTaxAndNics(payeAtsData.taxYear, scottishTaxBands, totalScottishIncomeTax)
  }
}
