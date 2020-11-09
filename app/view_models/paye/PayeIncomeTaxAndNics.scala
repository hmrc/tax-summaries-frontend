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

import config.PayeConfig
import models.{PayeAtsData, TaxBand}
import play.api.Play
import view_models.{Amount, Rate}

case class PayeIncomeTaxAndNics(
  taxYear: Int,
  scottishTaxBands: List[TaxBand],
  ukTaxBands: List[TaxBand],
  totalScottishIncomeTax: Amount,
  totalRestOfUKIncomeTax: Amount,
  totalUKIncomeTax: Amount,
  adjustments: List[AdjustmentRow],
  employeeContributions: Amount,
  employerContributions: Amount,
  totalIncomeTax2Nics: Amount,
  welshIncomeTax: Amount)
    extends TaxYearFormatting

object PayeIncomeTaxAndNics {

  lazy val scottishRates: List[String] = Play.current.injector.instanceOf[PayeConfig].scottishTaxBandKeys

  lazy val uKRates: List[String] = Play.current.injector.instanceOf[PayeConfig].ukTaxBandKeys

  lazy val adjustments: Set[String] = Play.current.injector.instanceOf[PayeConfig].adjustmentsKeys.toSet

  def apply(payeAtsData: PayeAtsData): PayeIncomeTaxAndNics =
    PayeIncomeTaxAndNics(
      payeAtsData.taxYear,
      getTaxBands(payeAtsData, scottishRates),
      getTaxBands(payeAtsData, uKRates),
      getTotalIncomeTax(payeAtsData, "scottish_total_tax"),
      getTotalIncomeTax(payeAtsData, "total_UK_income_tax"),
      getTotalIncomeTax(payeAtsData, "total_income_tax_2"),
      getAdjustments(payeAtsData),
      getNationalInsuranceContribution(payeAtsData, "employee_nic_amount"),
      getNationalInsuranceContribution(payeAtsData, "employer_nic_amount"),
      getNationalInsuranceContribution(payeAtsData, "total_income_tax_2_nics"),
      getWelshIncomeTax(payeAtsData)
    )

  private def getWelshIncomeTax(payeAtsData: PayeAtsData): Amount =
    payeAtsData.income_data
      .flatMap(incomeData => incomeData.payload.flatMap(_.get("scottish_income_tax")))
      .getOrElse(Amount.empty)

  private def getTotalIncomeTax(payeAtsData: PayeAtsData, totalTaxKey: String): Amount =
    payeAtsData.income_tax
      .flatMap(incomeTaxData => incomeTaxData.payload.flatMap(_.get(totalTaxKey)))
      .getOrElse(Amount.empty)

  private def getTaxBands(payeAtsData: PayeAtsData, taxBandRates: List[String]) =
    (for {
      incomeTax <- payeAtsData.income_tax
      rates     <- incomeTax.rates
      payload   <- incomeTax.payload
    } yield {
      taxBandRates
        .flatMap { name =>
          for {
            rate            <- rates.get("paye_" + name)
            incomeInTaxBand <- payload.get(name)
            taxPaidInBand   <- payload.get(name + "_amount")
          } yield {
            TaxBand(name, incomeInTaxBand, taxPaidInBand, rate)
          }
        }
        .filter(_.bandRate != Rate.empty)
    }).getOrElse(List.empty)

  private def getAdjustments(payeAtsData: PayeAtsData): List[AdjustmentRow] =
    (for {
      incomeTax <- payeAtsData.income_tax
      payload   <- incomeTax.payload
    } yield {
      payload
        .filterKeys(adjustments)
        .toList
        .map(
          adjustment => AdjustmentRow(adjustment._1, adjustment._2)
        )
        .filter(_.adjustmentAmount != Amount.empty)
        .sortBy(_.label)
    }).getOrElse(List.empty)

  private def getNationalInsuranceContribution(payeAtsData: PayeAtsData, nicKey: String): Amount =
    payeAtsData.summary_data
      .flatMap(summary_data => summary_data.payload.flatMap(_.get(nicKey)))
      .getOrElse(Amount.empty)
}

case class AdjustmentRow(label: String, adjustmentAmount: Amount)
