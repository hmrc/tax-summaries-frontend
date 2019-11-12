/*
 * Copyright 2019 HM Revenue & Customs
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

package utils

object TaxsValidator extends TaxsValidator

trait TaxsValidator {

  // match leadings spaces + any combination of digits and spaces 10 times
  val utrRegex = """^(?:[ \t]*\d[ \t]*){10}$"""

  val emailRegex = """(^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$)"""

  val dateRegex =  """^\d{2}/\d{2}/\d{4}$"""

  val asciiRegex =  """^[\x00-\x7F]*$"""

  val alphaRegex = """^(?i)[A-Z ]+$"""

  val numericRegex = """^[0-9 ]+$"""

  val alphaNumericRegex = """^(?i)[A-Z0-9 ]+$"""

  val telephoneRegex = """^[0-9]*+"""

  val asciiChar32 = 32
  val asciiChar126 = 126
  val asciiChar160 = 160
  val asciiChar255 = 255

  def validText(input: String): Boolean = {
    validateISO88591(input)
  }

  def validateISO88591(input: String): Boolean = {
    val inputList: List[Char] = input.toList
    inputList.forall { c =>
      (c >= asciiChar32 && c <= asciiChar126) || (c >= asciiChar160 && c <= asciiChar255)
    }
  }

  def validTextRegex(regex: String): (String) => Boolean = (input: String) => input.matches(regex)

  val validAlphaNumeric = validTextRegex(alphaNumericRegex)

}
