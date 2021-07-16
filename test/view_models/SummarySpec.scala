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

package view_models

import org.scalacheck.Gen
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import uk.gov.hmrc.domain.SaUtrGenerator

class SummarySpec extends WordSpec with Matchers with ScalaCheckDrivenPropertyChecks {

  def amountGen: Gen[Amount] =
    Gen.choose(-100, 100).map { amount =>
      Amount(BigDecimal(amount), "gbp")
    }

  def summaryGen: Gen[Summary] =
    for {
      amount <- amountGen
      utr = new SaUtrGenerator().nextSaUtr.utr
      rate <- Rate(Gen.chooseNum(0, 20).toString)
      year <- Gen.chooseNum(2010, 2099)
    } yield {
      Summary(
        year,
        utr,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        amount,
        rate,
        rate,
        "Mr",
        "Fake",
        "Man")
    }

  val zero = BigDecimal(0)

  "Summary" when {

    "taxYearInterval is called" must {

      "provide a correctly formatted string" in {
        forAll(summaryGen) { summary =>
          summary.taxYearInterval shouldBe s"${summary.year - 1}-${summary.year.toString.takeRight(2)}"
        }
      }
    }

    "taxYearIntervalTo is called" must {
      "provide a correctly formatted string" in {
        forAll(summaryGen) { summary =>
          summary.taxYearIntervalTo shouldBe s"${summary.year - 1} to ${summary.year}"
        }
      }
    }

    "hasTotalIncomeTaxAmount is called" must {

      "return true" when {
        "Total income tax amount is above zero" in {
          forAll(summaryGen) { summary =>
            whenever(summary.totalIncomeTaxAmount.amount > zero) {
              summary.hasTotalIncomeTaxAmount shouldBe true
            }
          }
        }
      }

      "return false" when {
        "Total income tax amount is below zero" in {
          forAll(summaryGen) { summary =>
            whenever(summary.totalIncomeTaxAmount.amount < zero) {
              summary.hasTotalIncomeTaxAmount shouldBe false
            }
          }
        }

        "Total income tax amount is exactly zero" in {
          forAll(summaryGen) { summary =>
            val summaryWithZero = summary.copy(totalIncomeTaxAmount = Amount(0, "gbp"))

            summaryWithZero.hasTotalIncomeTaxAmount shouldBe false
          }
        }
      }
    }

    "hasTotalCapitalGains is called" must {

      "return true" when {
        "Total capital gains tax is above zero" in {
          forAll(summaryGen) { summary =>
            whenever(summary.totalCapitalGainsTax.amount > zero) {
              summary.hasTotalCapitalGains shouldBe true
            }
          }
        }
      }

      "return false" when {
        "Total capital gains tax is exactly zero" in {
          forAll(summaryGen) { summary =>
            val summaryWithZero = summary.copy(totalCapitalGainsTax = Amount(0, "gbp"))
            summaryWithZero.hasTotalCapitalGains shouldBe false
          }
        }
      }
    }

    "hasEmployeeNicAmount is called" must {

      "return true" when {
        "employee NICs amount is above zero" in {
          forAll(summaryGen) { summary =>
            whenever(summary.employeeNicAmount.amount > zero) {
              summary.hasEmployeeNicAmount shouldBe true
            }
          }
        }
      }

      "return false" when {
        "employee NICs amount is exactly zero" in {
          forAll(summaryGen) { summary =>
            val summaryWithZero = summary.copy(employeeNicAmount = Amount(0, "gbp"))

            summaryWithZero.hasEmployeeNicAmount shouldBe false
          }
        }
      }
    }

    "nonNegativeTotalIncomeTaxAndNics is called" must {

      "give the sum of total income tax and NI contributions" when {

        "both income tax and NICs are positive" in {

          forAll(summaryGen) { summary =>
            whenever(
              summary.totalIncomeTaxAmount.amount > zero && summary.employeeNicAmount.amount > zero
            ) {
              summary.nonNegativeTotalIncomeTaxAndNics shouldBe
                Amount(
                  summary.totalIncomeTaxAmount.amount + summary.employeeNicAmount.amount,
                  summary.totalIncomeTaxAndNics.currency
                )
            }
          }
        }
      }

      "give the value of NI contributions" when {

        "total income tax is less than zero" in {

          forAll(summaryGen) { summary =>
            whenever(summary.employeeNicAmount.amount > zero) {
              val summaryWithLessThanZero = summary.copy(totalIncomeTaxAmount = Amount(-0.01, "gbp"))
              summaryWithLessThanZero.nonNegativeTotalIncomeTaxAndNics shouldBe summary.employeeNicAmount
            }
          }
        }

        "total income tax is zero" in {

          forAll(summaryGen) { summary =>
            whenever(summary.employeeNicAmount.amount > zero) {
              val summaryWithZero = summary.copy(totalIncomeTaxAmount = Amount(0, "gbp"))
              summaryWithZero.nonNegativeTotalIncomeTaxAndNics shouldBe summary.employeeNicAmount
            }
          }
        }
      }
    }

    "yourTotalTaxTextKey is called" must {

      "return the correct message key for the summary" when {

        "Summary has Income tax and NICs" in {
          forAll(summaryGen) { summary =>
            whenever(
              summary.totalIncomeTaxAmount.amount > zero && summary.employeeNicAmount.amount > zero
            ) {
              summary.yourTotalTaxTextKey shouldBe "ats.summary.tax_and_nics.title"
            }
          }
        }

        "Summary has only Income tax" in {
          forAll(summaryGen) { summary =>
            whenever(summary.totalIncomeTaxAmount.amount > zero) {
              val summaryWithZero = summary.copy(employeeNicAmount = Amount(0, "gbp"))
              summaryWithZero.yourTotalTaxTextKey shouldBe "ats.summary.tax.title"
            }
          }
        }

        "Summary has only NICs" in {
          forAll(summaryGen) { summary =>
            whenever(summary.employeeNicAmount.amount > zero) {
              val summaryWithZero = summary.copy(totalIncomeTaxAmount = Amount(0, "gbp"))
              summaryWithZero.yourTotalTaxTextKey shouldBe "ats.summary.nics.title"
            }
          }
        }
      }
    }

    "yourTotalTaxTextKeys is called" must {

      def expectedTitle(msg: String) = s"ats.summary.taxable_income.your_total_tax.msg_$msg"

      val incomeTaxDescription = "ats.summary.taxable_income.your_total_tax.description_total_income_tax"
      val nicsDescription = "ats.summary.taxable_income.your_total_tax.description_nics"
      val cgtDescription = "ats.summary.taxable_income.your_total_tax.description_cg"

      "return the UNARY message keys" when {

        "Summary has only income tax" in {
          forAll(summaryGen) { summary =>
            whenever(summary.totalIncomeTaxAmount.amount > zero) {
              val summaryIncomeOnly = summary.copy(
                employeeNicAmount = Amount(0, "gbp"),
                totalCapitalGainsTax = Amount(0, "gbp")
              )
              summaryIncomeOnly.yourTotalTaxTextKeys._1 shouldBe expectedTitle("unary")
              summaryIncomeOnly.yourTotalTaxTextKeys._2 shouldBe List(incomeTaxDescription)
            }
          }
        }

        "Summary has only capital gains tax" in {
          forAll(summaryGen) { summary =>
            whenever(summary.totalCapitalGainsTax.amount > zero) {
              val summaryCgtOnly = summary.copy(
                totalIncomeTaxAmount = Amount(0, "gbp"),
                employeeNicAmount = Amount(0, "gbp")
              )
              summaryCgtOnly.yourTotalTaxTextKeys._1 shouldBe expectedTitle("unary")
              summaryCgtOnly.yourTotalTaxTextKeys._2 shouldBe List(cgtDescription)
            }
          }
        }

        "Summary has only NI contributions" in {
          forAll(summaryGen) { summary =>
            whenever(summary.employeeNicAmount.amount > zero) {
              val summaryNicsOnly = summary.copy(
                totalIncomeTaxAmount = Amount(0, "gbp"),
                totalCapitalGainsTax = Amount(0, "gbp")
              )
              summaryNicsOnly.yourTotalTaxTextKeys._1 shouldBe expectedTitle("unary")
              summaryNicsOnly.yourTotalTaxTextKeys._2 shouldBe List(nicsDescription)
            }
          }
        }
      }

      "return the BINARY message keys" when {
        "Summary has Income tax and NICs but not Capital gains tax" in {
          forAll(summaryGen) { summary =>
            whenever(
              summary.totalIncomeTaxAmount.amount > zero &&
                summary.employeeNicAmount.amount > zero
            ) {
              val summaryWithNoCgt = summary.copy(totalCapitalGainsTax = Amount(0, "gbp"))
              summaryWithNoCgt.yourTotalTaxTextKeys._1 shouldBe expectedTitle("binary")
              summaryWithNoCgt.yourTotalTaxTextKeys._2 shouldBe
                List(incomeTaxDescription, nicsDescription)
            }
          }
        }

        "Summary has Income tax and Capital gains tax but not NICs" in {
          forAll(summaryGen) { summary =>
            whenever(
              summary.totalIncomeTaxAmount.amount > zero &&
                summary.totalCapitalGainsTax.amount > zero
            ) {
              val summaryWithNoNics = summary.copy(employeeNicAmount = Amount(0, "gbp"))
              summaryWithNoNics.yourTotalTaxTextKeys._1 shouldBe expectedTitle("binary")
              summaryWithNoNics.yourTotalTaxTextKeys._2 shouldBe
                List(incomeTaxDescription, cgtDescription)
            }
          }
        }

        "Summary has NICs and Capital gains tax but not Income tax" in {
          forAll(summaryGen) { summary =>
            whenever(
              summary.employeeNicAmount.amount > zero &&
                summary.totalCapitalGainsTax.amount > zero
            ) {
              val summaryWithNoNics = summary.copy(totalIncomeTaxAmount = Amount(0, "gbp"))
              summaryWithNoNics.yourTotalTaxTextKeys._1 shouldBe expectedTitle("binary")
              summaryWithNoNics.yourTotalTaxTextKeys._2 shouldBe
                List(nicsDescription, cgtDescription)
            }
          }
        }
      }

      "return the TERNARY message keys" when {
        "Summary has Income tax, Nics and Capital gains tax" in {
          forAll(summaryGen) { summary =>
            whenever(
              summary.totalIncomeTaxAmount.amount > zero &&
                summary.employeeNicAmount.amount > zero &&
                summary.totalCapitalGainsTax.amount > zero
            ) {
              summary.yourTotalTaxTextKeys._1 shouldBe expectedTitle("ternary")
              summary.yourTotalTaxTextKeys._2 shouldBe
                List(incomeTaxDescription, nicsDescription, cgtDescription)
            }
          }
        }
      }
    }

    "hasTaxableGains is called" must {

      "return true" when {

        "Taxable gains is greater than zero" in {

          forAll(summaryGen) { summary =>
            whenever(summary.taxableGains.amount > zero) {
              summary.hasTaxableGains shouldBe true
            }
          }
        }
      }

      "return false" when {
        "Taxable gains is exactly zero" in {
          forAll(summaryGen) { summary =>
            val summaryWithZero = summary.copy(taxableGains = Amount(0, "gbp"))
            summaryWithZero.hasTaxableGains shouldBe false
          }
        }
      }
    }

    "taxYearTo is called" must {
      "return the year as a string" in {
        forAll(summaryGen) { summary =>
          summary.taxYearTo shouldBe summary.year.toString
        }
      }
    }

    "taxYearFrom is called" must {
      "return the previous year as a string" in {
        forAll(summaryGen) { summary =>
          summary.taxYearFrom shouldBe (summary.year - 1).toString
        }
      }
    }
  }
}
