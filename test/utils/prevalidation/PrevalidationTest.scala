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

package utils.prevalidation

import play.api.data.Form
import play.api.data.Forms._
import utils.TaxsUnitTestTraits
import utils.prevalidation.TrimOption.TrimOption
import utils.prevalidation.CaseOption.CaseOption
import utils.prevalidation.prevalidation._

class PrevalidationTest extends TaxsUnitTestTraits {

  case class DummyData(string1: String)

  object DummyForm {

    val dummyForm = Form[DummyData](
      mapping(
        "string1" -> text
      )(DummyData.apply)(DummyData.unapply)
    )

    def preprocessedForm(trims: Map[String, TrimOption] = Map(), caseRules: Map[String, CaseOption] = Map()) =
      PreprocessedForm(dummyForm, trims, caseRules)

  }

  def testData(data: String): Map[String, String] = Map[String, String]("string1" -> data)

  "Form submission " should {
    "trim any text strings at both ends when additional whitespace exists for option 'both'" in {
      import TrimOption._
      val defaultTrims = Map[String, TrimOption](
        "string1" -> both
      )
      val form = DummyForm.preprocessedForm(defaultTrims)

      val result = form.bind(testData(" Vinnie and the \t    grenades    \t")).get
      result.string1 shouldBe "Vinnie and the \t    grenades"
    }

    "remove all whitespace if it exists for option 'all'" in {
      val defaultTrims = Map[String, TrimOption](
        "string1" -> TrimOption.all
      )
      val form = DummyForm.preprocessedForm(defaultTrims)

      val result = form.bind(testData(" Vinnie and the \t    grenades    \t")).get
      result.string1 shouldBe "Vinnieandthegrenades"
    }

    "trim any text strings at both ends and compress when additional whitespace exists for option 'bothAndCompress'" in {
      import TrimOption._
      val defaultTrims = Map[String, TrimOption](
        "string1" -> bothAndCompress
      )
      val form = DummyForm.preprocessedForm(defaultTrims)

      val result = form.bind(testData(" Vinnie and the \t    grenades    \t")).get
      result.string1 shouldBe "Vinnie and the grenades"
    }

    "not trim any text strings when additional whitespace exists for option 'none'" in {
      import TrimOption._
      val defaultTrims = Map[String, TrimOption](
        "string1" -> none
      )
      val form = DummyForm.preprocessedForm(defaultTrims)

      val result = form.bind(testData(" Vinnie and the \t    grenades    \t")).get
      result.string1 shouldBe " Vinnie and the \t    grenades    \t"
    }

    "amend the case of any text strings to uppercase for option 'upper'" in {
      import CaseOption._
      val defaultCase = Map[String, CaseOption](
        "string1" -> upper
      )
      val form = DummyForm.preprocessedForm(caseRules = defaultCase)

      val result = form.bind(testData("Vinnie and the grenades")).get
      result.string1 shouldBe "VINNIE AND THE GRENADES"
    }

    "amend the case of any text strings to lowercase for option 'lower'" in {
      import CaseOption._
      val defaultCase = Map[String, CaseOption](
        "string1" -> lower
      )
      val form = DummyForm.preprocessedForm(caseRules = defaultCase)

      val result = form.bind(testData("Vinnie and the grenades")).get
      result.string1 shouldBe "vinnie and the grenades"
    }

    "leave the case of any text strings for option 'none'" in {
      import CaseOption._
      val defaultCase = Map[String, CaseOption](
        "string1" -> none
      )
      val form = DummyForm.preprocessedForm(caseRules = defaultCase)

      val result = form.bind(testData("Vinnie and the grenades")).get
      result.string1 shouldBe "Vinnie and the grenades"
    }
  }
}
