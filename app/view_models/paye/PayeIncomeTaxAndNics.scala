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

  def apply(payeAtsData: PayeAtsData): PayeIncomeTaxAndNics = {

    val isScottish: Boolean = payeAtsData.income_tax.flatMap(incomeTaxData => incomeTaxData.payload.flatMap(_.get("scottish_total_tax"))).exists(_.nonZero)

    if(isScottish) {

      val totalScottishIncomeTax = payeAtsData.income_tax.flatMap { incomeTaxData =>
        incomeTaxData.payload.flatMap { payload =>
          payload.get("scottish_total_tax")
        }
      }.getOrElse(Amount.empty)

      val scottishBandRates: Map[String, Rate] = payeAtsData.income_tax.flatMap {
        incomeTaxData =>
          incomeTaxData.rates.map{
            rates => {
              Map("scottish_starter_rate" -> rates.getOrElse("paye_scottish_starter_rate", Rate.empty),
                  "scottish_basic_rate" -> rates.getOrElse("paye_scottish_basic_rate", Rate.empty),
                  "scottish_intermediate_rate" -> rates.getOrElse("paye_scottish_intermediate_rate", Rate.empty),
                  "scottish_higher_rate" -> rates.getOrElse("paye_scottish_higher_rate", Rate.empty)
              )
            }
          }
      }.getOrElse(Map.empty).filter(_._2!=Rate.empty)

      val scottishTaxBands: List[TaxBand] = scottishBandRates.flatMap(
        rate => {
          payeAtsData.income_tax.flatMap {
            incomeTaxData =>
              incomeTaxData.payload.map(
                payload => {
                  val incomeInTaxBand = payload.getOrElse(rate._1 + "_income_tax", Amount.empty)
                  val taxPaidInBand = payload.getOrElse(rate._1 + "_income_tax_amount", Amount.empty)
                  TaxBand(incomeInTaxBand, taxPaidInBand, rate._2)
                }
              )
          }
        }
      ).toList

      PayeIncomeTaxAndNics(payeAtsData.taxYear, scottishTaxBands, totalScottishIncomeTax)

    } else {
      PayeIncomeTaxAndNics(payeAtsData.taxYear, List.empty, Amount.empty)
    }
  }
}
