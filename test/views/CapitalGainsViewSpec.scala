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

package views

import controllers.auth.AuthenticatedRequest
import models.ActingAsAttorneyFor
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.SaUtr
import utils.TestConstants
import view_models.{Amount, CapitalGains}
import views.html.CapitalGainsView

class CapitalGainsViewSpec extends ViewSpecBase with TestConstants {

  implicit val request: AuthenticatedRequest[AnyContentAsEmpty.type] =
    AuthenticatedRequest(
      "userId",
      None,
      Some(SaUtr(testUtr)),
      None,
      isSa = true,
      isAgentActive = false,
      ConfidenceLevel.L50,
      fakeCredentials,
      FakeRequest()
    )
  lazy val capitalGainsView: CapitalGainsView                        = inject[CapitalGainsView]

  def view(cg: CapitalGains): String =
    capitalGainsView(cg).body

  def agentView(cg: CapitalGains): String =
    capitalGainsView(cg, Some(ActingAsAttorneyFor(Some("Agent"), Map()))).body

  "view" must {
    "show lower rate rcpi row" in {
      val result = view(capitalGains)

      result must include("Individuals for residential property and carried interest (&pound;4,000 at 15%)")
      result must include("&pound;1,000")
    }

    "hide lower rate rcpi row" in {
      val result = view(capitalGains.copy(rpciLowerTax = Amount.gbp(0)))

      result must not include "Individuals for residential property and carried interest (&pound;4,000 at 15%)"
    }

    "show higher rate rcpi row" in {
      val result = view(capitalGains)

      result must include("Individuals for residential property and carried interest (&pound;4,500 at 25%)")
      result must include("&pound;1,500")
    }

    "hide higher rate rcpi row" in {
      val result = view(capitalGains.copy(rpciHigherTax = Amount.gbp(0)))

      result must not include "Individuals for residential property and carried interest (&pound;4,500 at 25%)"
    }

    "not show account menu for agent" in {

      val result = agentView(capitalGains)
      result must not include "hmrc-account-menu"
    }

    "show account menu for non agent users" in {

      val result = view(capitalGains)
      result must include("hmrc-account-menu")
    }

  }
}
