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

package views

import controllers.auth.AuthenticatedRequest
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.SaUtr
import utils.TestConstants
import view_models.{Amount, CapitalGains}
import views.html.CapitalGainsView

class CapitalGainsViewSpec extends ViewSpecBase with TestConstants {

  implicit val request =
    AuthenticatedRequest(
      "userId",
      None,
      Some(SaUtr(testUtr)),
      None,
      None,
      None,
      None,
      true,
      false,
      fakeCredentials,
      FakeRequest())
  lazy val capitalGainsView = inject[CapitalGainsView]

  def view(cg: CapitalGains): String =
    capitalGainsView(cg).body

  "view" should {
    "show lower rate rcpi row" in {
      val result = view(capitalGains)

      result should include("Individuals for residential property and carried interest (&pound;4,000 at 15%)")
      result should include("&pound;1,000")
    }

    "hide lower rate rcpi row" in {
      val result = view(capitalGains.copy(rpciLowerTax = Amount.gbp(0)))

      result should not include "Individuals for residential property and carried interest (&pound;4,000 at 15%)"
    }

    "show higher rate rcpi row" in {
      val result = view(capitalGains)

      result should include("Individuals for residential property and carried interest (&pound;4,500 at 25%)")
      result should include("&pound;1,500")
    }

    "hide higher rate rcpi row" in {
      val result = view(capitalGains.copy(rpciHigherTax = Amount.gbp(0)))

      result should not include "Individuals for residential property and carried interest (&pound;4,500 at 25%)"
    }
  }
}
