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

package common.utils

object TextGenerator {
  def createOnScreenText(hasTotalIncomeTax: Boolean, hasCapitalGainsTax: Boolean, hasNics: Boolean) =
    if (hasTotalIncomeTax && hasCapitalGainsTax && hasNics) {
      createOnScreenTextTernary
    } else if (hasTotalIncomeTax ^ hasCapitalGainsTax ^ hasNics) {
      createOnScreenTextUnary(hasTotalIncomeTax, hasCapitalGainsTax)
    } else {
      createOnScreenTextBinary(hasTotalIncomeTax, hasCapitalGainsTax)
    }

  private def createOnScreenTextTernary =
    (
      "ats.summary.taxable_income.your_total_tax.msg_ternary",
      List(
        "ats.summary.taxable_income.your_total_tax.description_total_income_tax",
        "ats.summary.taxable_income.your_total_tax.description_nics",
        "ats.summary.taxable_income.your_total_tax.description_cg"
      )
    )

  private def createOnScreenTextBinary(hasTotalIncomeTax: Boolean, hasCapitalGainsTax: Boolean) =
    if (hasTotalIncomeTax) {
      if (hasCapitalGainsTax) {
        (
          "ats.summary.taxable_income.your_total_tax.msg_binary",
          List(
            "ats.summary.taxable_income.your_total_tax.description_total_income_tax",
            "ats.summary.taxable_income.your_total_tax.description_cg"
          )
        )
      } else {
        (
          "ats.summary.taxable_income.your_total_tax.msg_binary",
          List(
            "ats.summary.taxable_income.your_total_tax.description_total_income_tax",
            "ats.summary.taxable_income.your_total_tax.description_nics"
          )
        )
      }
    } else {
      (
        "ats.summary.taxable_income.your_total_tax.msg_binary",
        List(
          "ats.summary.taxable_income.your_total_tax.description_nics",
          "ats.summary.taxable_income.your_total_tax.description_cg"
        )
      )
    }

  private def createOnScreenTextUnary(hasTotalIncomeTax: Boolean, hasCapitalGainsTax: Boolean) =
    if (hasTotalIncomeTax) {
      (
        "ats.summary.taxable_income.your_total_tax.msg_unary",
        List("ats.summary.taxable_income.your_total_tax.description_total_income_tax")
      )
    } else if (hasCapitalGainsTax) {
      (
        "ats.summary.taxable_income.your_total_tax.msg_unary",
        List("ats.summary.taxable_income.your_total_tax.description_cg")
      )
    } else {
      (
        "ats.summary.taxable_income.your_total_tax.msg_unary",
        List("ats.summary.taxable_income.your_total_tax.description_nics")
      )
    }
}
